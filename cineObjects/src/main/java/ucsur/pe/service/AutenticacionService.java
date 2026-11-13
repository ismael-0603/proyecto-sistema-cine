package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;
import ucsur.pe.repository.ClienteRepository;
import ucsur.pe.repository.EmpleadoRepository;
import java.util.Optional;

/**
 * Por qué existe: casos de uso "Registrar usuario" e "Iniciar sesión" (RF-01, RF-27).
 * Busca las cuentas en los repositorios y aplica las reglas que dependen de los datos guardados,
 * como que el correo de una cuenta registrada sea único.
 */
public class AutenticacionService {
    private final ClienteRepository clientes;
    private final EmpleadoRepository empleados;

    public AutenticacionService(ClienteRepository clientes, EmpleadoRepository empleados) {
        this.clientes = clientes;
        this.empleados = empleados;
    }

    /** El cliente compra sin registrarse; solo deja su correo (RF-01). */
    public Cliente ingresarComoInvitado(String correo) {
        Cliente invitado = Cliente.ingresarComoInvitado(correo);
        clientes.guardar(invitado);
        return invitado;
    }

    /** Registra un socio nuevo; rechaza un correo que ya usa otra cuenta registrada. */
    public Cliente registrarCliente(String dni, String nombres, String apellidos, DateTime fechaNac,
                                    String correo, String telefono, String contra) {
        exigirCorreoLibre(correo, null);
        Cliente cliente = new Cliente(clientes.siguienteId(), dni, nombres, apellidos, fechaNac, correo, telefono, contra);
        clientes.guardar(cliente);
        return cliente;
    }

    /**
     * Inicia sesión de un cliente o empleado. Ante cualquier fallo responde lo mismo
     * ("correo o contraseña incorrectos") para no revelar qué correos existen.
     */
    public Persona iniciarSesion(String correo, String contra) {
        Optional<Persona> cuenta = buscarCuenta(correo);
        if (cuenta.isEmpty() || !cuenta.get().verificarContrasena(contra)) {
            throw new ReglaNegocioException("Correo o contraseña incorrectos");
        }
        return cuenta.get();
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

    private Optional<Persona> buscarCuenta(String correo) {
        if (correo == null) return Optional.empty();
        Optional<Persona> cliente = clientes.buscarPorCorreo(correo).map(c -> c);
        return cliente.isPresent() ? cliente : empleados.buscarPorCorreo(correo).map(e -> e);
    }

    private void exigirCorreoLibre(String correo, Persona propietario) {
        Optional<Persona> otra = buscarCuenta(correo);
        if (otra.isPresent() && otra.get() != propietario) {
            throw new ReglaNegocioException("Ya existe una cuenta registrada con el correo " + correo);
        }
    }
}
