package ucsur.pe.service;

import ucsur.pe.correo.ServicioCorreo;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Reloj;
import ucsur.pe.repository.ClienteRepository;
import ucsur.pe.repository.EmpleadoRepository;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Por qué existe: casos de uso "Registrar usuario", "Iniciar sesión" y "Recuperar contraseña" (RF-01, RF-27),
 * y el paso del empleado entre su panel y su cuenta personal (con PIN).
 * Busca las cuentas en los repositorios y aplica las reglas que dependen de los datos guardados,
 * como que el correo y el DNI de una cuenta registrada sean únicos.
 */
public class AutenticacionService {
    public static final int MINUTOS_VIGENCIA_CODIGO = 10; // el código de recuperación vence a los 10 minutos
    public static final int MAX_INTENTOS_CODIGO = 5;      // tras 5 códigos equivocados hay que pedir otro
    public static final int MAX_INTENTOS_PIN = 3;         // tras 3 PIN equivocados seguidos se cierra la sesión
    /** Mismo mensaje exista o no la cuenta: así no se revela qué correos están registrados. */
    public static final String MENSAJE_CODIGO_ENVIADO =
            "Si el correo está registrado, te enviamos un código para cambiar tu contraseña";

    private final ClienteRepository clientes;
    private final EmpleadoRepository empleados;
    private final ServicioCorreo correo;
    private final Reloj reloj;
    private final SecureRandom azar = new SecureRandom(); // el código no se debe poder adivinar
    /** Códigos pendientes por correo (en minúsculas). Viven en memoria: si el kiosco se reinicia, se pide otro. */
    private final Map<String, CodigoRecuperacion> codigos = new HashMap<>();

    /** Código enviado a una cuenta, cuándo vence y cuántas veces se escribió mal. */
    private static final class CodigoRecuperacion {
        private final String codigo;
        private final DateTime vence;
        private int intentosFallidos;

        private CodigoRecuperacion(String codigo, DateTime vence) {
            this.codigo = codigo;
            this.vence = vence;
        }
    }

    public AutenticacionService(ClienteRepository clientes, EmpleadoRepository empleados, ServicioCorreo correo,
                                Reloj reloj) {
        this.clientes = clientes;
        this.empleados = empleados;
        this.correo = correo;
        this.reloj = reloj;
    }

    /** El cliente compra sin registrarse; solo deja su correo (RF-01). */
    public Cliente ingresarComoInvitado(String correo) {
        Cliente invitado = Cliente.ingresarComoInvitado(correo, reloj);
        clientes.guardar(invitado);
        return invitado;
    }

    /**
     * Registra un socio nuevo; rechaza un correo que ya usa otra cuenta registrada y una confirmación
     * de contraseña que no coincide (wireframe 5: «Confirmar contraseña»).
     */
    public Cliente registrarCliente(String dni, String nombres, String apellidos, DateTime fechaNac,
                                    String correo, String telefono, String contra, String confirmarContra) {
        if (contra == null || !contra.equals(confirmarContra)) {
            throw new ReglaNegocioException("Las contraseñas no coinciden");
        }
        exigirCorreoLibre(correo, null);
        exigirDniLibre(dni);
        Cliente cliente = new Cliente(0, dni, nombres, apellidos, fechaNac, correo, telefono, contra, reloj);
        clientes.guardar(cliente); // la base le genera el id (AUTO_INCREMENT)
        return cliente;
    }

    /**
     * Inicia sesión de un cliente o empleado y devuelve la Sesion con su tipo ya resuelto: se sabe en qué
     * repositorio se encontró la cuenta, así que no hace falta instanceof. Ante cualquier fallo responde
     * lo mismo ("correo o contraseña incorrectos") para no revelar qué correos existen.
     */
    public Sesion iniciarSesion(String correo, String contra) {
        if (correo != null) {
            Optional<Cliente> cliente = clientes.buscarPorCorreo(correo);
            if (cliente.isPresent()) {
                return verificar(cliente.get(), contra, Sesion.deCliente(cliente.get()));
            }
            Optional<Empleado> empleado = empleados.buscarPorCorreo(correo);
            if (empleado.isPresent()) {
                return verificar(empleado.get(), contra, Sesion.deEmpleado(empleado.get()));
            }
        }
        throw new ReglaNegocioException("Correo o contraseña incorrectos");
    }

    private static Sesion verificar(Persona cuenta, String contra, Sesion sesion) {
        if (!cuenta.verificarContrasena(contra)) {
            throw new ReglaNegocioException("Correo o contraseña incorrectos");
        }
        return sesion;
    }

    /** Cambia teléfono y correo de un cliente; el correo nuevo no puede usarlo otra cuenta. */
    public void actualizarDatos(Cliente cliente, String telefono, String correo) {
        exigirCorreoLibre(correo, cliente);
        cliente.modificarDatos(telefono, correo);
        clientes.guardar(cliente);
    }

    /** Cambia teléfono y correo de un empleado; el correo nuevo no puede usarlo otra cuenta. */
    public void actualizarDatos(Empleado empleado, String telefono, String correo) {
        exigirCorreoLibre(correo, empleado);
        empleado.modificarDatos(telefono, correo);
        empleados.guardar(empleado);
    }

    // ---------- Cuenta personal del empleado ----------

    /** Qué necesita el empleado para comprar con su cuenta personal (ver CuentaPersonal). */
    public CuentaPersonal consultarCuentaPersonal(Empleado empleado) {
        if (empleado.tieneCuentaPersonal()) {
            return new CuentaPersonal(CuentaPersonal.Estado.VINCULADA, null);
        }
        return clientes.buscarPorDni(empleado.getDni())
                .map(c -> new CuentaPersonal(CuentaPersonal.Estado.POR_CONFIRMAR, CuentaPersonal.enmascarar(c.getCorreo())))
                .orElse(new CuentaPersonal(CuentaPersonal.Estado.NUEVA, null));
    }

    /** Pasa al modo cliente con la cuenta personal ya vinculada; la cuenta debe estar activa. */
    public void pasarAModoCliente(Sesion sesion) {
        Empleado empleado = exigirEmpleado(sesion);
        if (!empleado.tieneCuentaPersonal()) {
            throw new ReglaNegocioException("Primero vincula tu cuenta personal");
        }
        if (!empleado.getCuentaPersonal().isEstado()) {
            throw new ReglaNegocioException("Tu cuenta personal está desactivada");
        }
        sesion.pasarAModoCliente();
    }

    /**
     * Ya existe un cliente con el DNI del empleado: se vincula si escribe la contraseña de esa cuenta,
     * y pasa al modo cliente.
     */
    public void vincularCuentaExistente(Sesion sesion, String contraPersonal) {
        Empleado empleado = exigirEmpleado(sesion);
        Cliente cliente = clientes.buscarPorDni(empleado.getDni())
                .orElseThrow(() -> new ReglaNegocioException("No hay una cuenta personal con tu DNI"));
        if (!cliente.isEstado()) {
            throw new ReglaNegocioException("Tu cuenta personal está desactivada");
        }
        if (!cliente.verificarContrasena(contraPersonal)) {
            throw new ReglaNegocioException("La contraseña de tu cuenta personal no es correcta");
        }
        empleado.vincularCuentaPersonal(cliente);
        empleados.guardar(empleado);
        sesion.pasarAModoCliente();
    }

    /**
     * Crea la cuenta personal del empleado con sus datos y el correo y la contraseña que elige
     * (no los de trabajo), la vincula y pasa al modo cliente.
     */
    public void crearCuentaPersonal(Sesion sesion, String correoPersonal, String contra, String confirmarContra) {
        Empleado empleado = exigirEmpleado(sesion);
        Cliente cliente = registrarCliente(empleado.getDni(), empleado.getNombres(), empleado.getApellidos(),
                empleado.getFechaNac(), correoPersonal, empleado.getTelefono(), contra, confirmarContra);
        empleado.vincularCuentaPersonal(cliente);
        empleados.guardar(empleado);
        sesion.pasarAModoCliente();
    }

    /**
     * Vuelve del modo cliente al panel si el PIN es correcto. Tras MAX_INTENTOS_PIN fallos seguidos
     * la sesión queda cerrada por seguridad (Sesion.cerradaPorSeguridad) y la vista la cierra.
     */
    public void volverAModoEmpleado(Sesion sesion, String pin) {
        Empleado empleado = exigirEmpleado(sesion);
        if (sesion.cerradaPorSeguridad()) {
            throw new ReglaNegocioException("La sesión se cerró por seguridad");
        }
        if (empleado.verificarPin(pin)) {
            sesion.pasarAModoEmpleado();
            return;
        }
        int fallidos = sesion.registrarPinFallido();
        if (fallidos >= MAX_INTENTOS_PIN) {
            sesion.cerrarPorSeguridad();
            throw new ReglaNegocioException("Demasiados intentos con un PIN incorrecto; por seguridad se cerró la sesión");
        }
        throw new ReglaNegocioException("PIN incorrecto (te quedan " + (MAX_INTENTOS_PIN - fallidos) + " intentos)");
    }

    /** Cambia el PIN del empleado; el nuevo se escribe dos veces. */
    public void cambiarPin(Empleado empleado, String pinActual, String pinNuevo, String confirmarPin) {
        if (pinNuevo == null || !pinNuevo.equals(confirmarPin)) {
            throw new ReglaNegocioException("Los PIN nuevos no coinciden");
        }
        empleado.cambiarPin(pinActual, pinNuevo);
        empleados.guardar(empleado);
    }

    private static Empleado exigirEmpleado(Sesion sesion) {
        if (sesion == null || !sesion.esEmpleado()) {
            throw new ReglaNegocioException("Esta opción es solo para empleados");
        }
        return sesion.empleado();
    }

    // ---------- Recuperar contraseña («¿Olvidaste tu contraseña?», wireframe 4) ----------
    /**
     * Paso 1: si el correo es de un socio o empleado activo, le envía un código de 6 dígitos que vence en
     * MINUTOS_VIGENCIA_CODIGO minutos (pedir otro reemplaza al anterior). Los invitados no tienen contraseña.
     * Siempre devuelve MENSAJE_CODIGO_ENVIADO, exista o no la cuenta.
     */
    public String solicitarRecuperacion(String correoCuenta) {
        Optional<Persona> cuenta = buscarCuenta(correoCuenta == null ? null : correoCuenta.trim());
        if (cuenta.isPresent() && puedeRecuperar(cuenta.get())) {
            String codigo = String.format("%06d", azar.nextInt(1_000_000));
            codigos.put(clave(correoCuenta),
                    new CodigoRecuperacion(codigo, reloj.ahora().sumarMinutos(MINUTOS_VIGENCIA_CODIGO)));
            String cuerpo = "Hola " + cuenta.get().getNombreCompleto() + ",\n\nTu código para cambiar la contraseña es "
                    + codigo + ". Vence en " + MINUTOS_VIGENCIA_CODIGO + " minutos.\n"
                    + "Si no lo pediste, ignora este mensaje: tu contraseña no cambia.";
            if (!correo.enviar(cuenta.get().getCorreo(), "Código para cambiar tu contraseña", cuerpo)) {
                codigos.remove(clave(correoCuenta));
                throw new ReglaNegocioException("No se pudo enviar el código; inténtalo de nuevo");
            }
        }
        return MENSAJE_CODIGO_ENVIADO;
    }

    /**
     * Paso 2: con el código recibido cambia la contraseña. El código sirve una sola vez, vence a los
     * MINUTOS_VIGENCIA_CODIGO minutos y se anula tras MAX_INTENTOS_CODIGO intentos equivocados.
     */
    public void restablecerContrasena(String correoCuenta, String codigo, String nueva, String confirmar) {
        CodigoRecuperacion pendiente = correoCuenta == null ? null : codigos.get(clave(correoCuenta));
        if (pendiente == null) {
            throw new ReglaNegocioException("El código no es válido; solicita uno nuevo");
        }
        if (reloj.ahora().esDespuesDe(pendiente.vence)) {
            codigos.remove(clave(correoCuenta));
            throw new ReglaNegocioException("El código venció; solicita uno nuevo");
        }
        if (codigo == null || !pendiente.codigo.equals(codigo.trim())) {
            pendiente.intentosFallidos++;
            if (pendiente.intentosFallidos >= MAX_INTENTOS_CODIGO) {
                codigos.remove(clave(correoCuenta));
                throw new ReglaNegocioException("Demasiados intentos con un código incorrecto; solicita uno nuevo");
            }
            throw new ReglaNegocioException("El código no es correcto (te quedan "
                    + (MAX_INTENTOS_CODIGO - pendiente.intentosFallidos) + " intentos)");
        }
        if (nueva == null || !nueva.equals(confirmar)) {
            throw new ReglaNegocioException("Las contraseñas no coinciden");
        }
        Optional<Persona> cuenta = buscarCuenta(correoCuenta.trim());
        if (cuenta.isEmpty() || !puedeRecuperar(cuenta.get())) {
            codigos.remove(clave(correoCuenta));
            throw new ReglaNegocioException("El código no es válido; solicita uno nuevo");
        }
        cuenta.get().cambiarContrasena(nueva); // valida el largo (mínimo 8) antes de gastar el código
        if (cuenta.get() instanceof Cliente cliente) {
            clientes.guardar(cliente);
        } else {
            empleados.guardar((Empleado) cuenta.get());
        }
        codigos.remove(clave(correoCuenta));
    }

    /** Recupera la contraseña una cuenta activa que la tenga: socio registrado o empleado, nunca un invitado. */
    private static boolean puedeRecuperar(Persona cuenta) {
        if (!cuenta.isEstado()) return false;
        return !(cuenta instanceof Cliente cliente) || !cliente.esInvitado();
    }

    private static String clave(String correoCuenta) {
        return correoCuenta.trim().toLowerCase();
    }

    private Optional<Persona> buscarCuenta(String correo) {
        if (correo == null) return Optional.empty();
        Optional<Persona> cliente = clientes.buscarPorCorreo(correo).map(c -> c);
        return cliente.isPresent() ? cliente : empleados.buscarPorCorreo(correo).map(e -> e);
    }

    private void exigirDniLibre(String dni) {
        if (clientes.buscarPorDni(dni).isPresent()) {
            throw new ReglaNegocioException("Ya existe una cuenta registrada con el DNI " + dni);
        }
    }

    private void exigirCorreoLibre(String correo, Persona propietario) {
        Optional<Persona> otra = buscarCuenta(correo);
        if (otra.isPresent() && otra.get() != propietario) {
            throw new ReglaNegocioException("Ya existe una cuenta registrada con el correo " + correo);
        }
    }
}
