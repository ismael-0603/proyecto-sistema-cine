package ucsur.pe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.ClienteRepository;
import ucsur.pe.repository.EmpleadoRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del paso del empleado entre su panel y su cuenta personal (AutenticacionService y Sesion).
 *
 * Se verifica:
 *   - El login con el correo de trabajo entra en modo EMPLEADO, sin cliente para comprar.
 *   - Crear la cuenta personal (correo propio) la vincula y pasa a modo cliente; rechaza correo usado
 *     y contraseñas distintas.
 *   - Un cliente que ya existe con su DNI se vincula solo con su contraseña y conserva la misma cuenta.
 *   - El PIN: correcto vuelve al panel, incorrecto avisa los intentos, 3 fallos cierran la sesión.
 *   - Cambiar el PIN y el DNI repetido al registrar un cliente.
 *
 * Usa repositorios en memoria: no necesita base de datos ni JavaFX. Patrón AAA en cada prueba.
 */
@DisplayName("Pruebas: cuenta personal y PIN del empleado")
class CuentaPersonalEmpleadoTest {

    private static final String DNI = "12345678";
    private static final String CORREO_TRABAJO = "ana.rojas@cine.pe";
    private static final String CONTRA_TRABAJO = "Trabajo@123";
    private static final String PIN = "4321";

    private final Reloj reloj = () -> new DateTime(23, 11, 2026, 10, 0);
    private ClientesEnMemoria clientes;
    private EmpleadosEnMemoria empleados;
    private AutenticacionService servicio;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        clientes = new ClientesEnMemoria();
        empleados = new EmpleadosEnMemoria();
        servicio = new AutenticacionService(clientes, empleados, (para, asunto, cuerpo) -> true, reloj);
        Rol dulceria = new Rol(2, "Empleado", "Atención de dulcería", Rol.NIVEL_DULCERIA);
        Sede sede = new Sede(1, "Cine Centro", "Av. Principal 123", "Lima", null, null, Sede.ACTIVA);
        empleado = new Empleado(0, DNI, "Ana", "Rojas Paz", null, CORREO_TRABAJO, null, CONTRA_TRABAJO,
                "MAÑANA", dulceria, sede, PIN, reloj);
        empleados.guardar(empleado);
    }

    private Sesion iniciarComoEmpleado() {
        return servicio.iniciarSesion(CORREO_TRABAJO, CONTRA_TRABAJO);
    }

    // ---------------- Inicio de sesión ----------------

    @Test
    @DisplayName("El correo de trabajo entra en modo empleado, sin cuenta de cliente para comprar")
    void loginEmpleado_entraEnModoEmpleado() {
        Sesion sesion = iniciarComoEmpleado();

        assertTrue(sesion.esEmpleado());
        assertFalse(sesion.enModoCliente());
        assertSame(empleado, sesion.empleadoEnPanel());
        assertNull(sesion.cliente());
    }

    // ---------------- Crear la cuenta personal ----------------

    @Test
    @DisplayName("Sin cliente con su DNI, la cuenta personal es NUEVA")
    void sinCliente_cuentaNueva() {
        assertEquals(CuentaPersonal.Estado.NUEVA, servicio.consultarCuentaPersonal(empleado).estado());
    }

    @Test
    @DisplayName("Crear la cuenta personal la vincula con su DNI y pasa a modo cliente")
    void crearCuentaPersonal_vinculaYPasaAModoCliente() {
        Sesion sesion = iniciarComoEmpleado();

        servicio.crearCuentaPersonal(sesion, "ana.personal@gmail.com", "Personal@1", "Personal@1");

        Cliente personal = sesion.cliente();
        assertNotNull(personal);
        assertEquals(DNI, personal.getDni());
        assertEquals("ana.personal@gmail.com", personal.getCorreo());
        assertTrue(sesion.enModoCliente());
        assertNull(sesion.empleadoEnPanel(), "en modo cliente no se entra a los paneles");
        assertEquals(CuentaPersonal.Estado.VINCULADA, servicio.consultarCuentaPersonal(empleado).estado());
    }

    @Test
    @DisplayName("No se puede crear la cuenta personal con un correo que ya usa otra cuenta")
    void crearCuentaPersonal_correoUsado_falla() {
        Sesion sesion = iniciarComoEmpleado();

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearCuentaPersonal(sesion, CORREO_TRABAJO, "Personal@1", "Personal@1"));
        assertFalse(empleado.tieneCuentaPersonal());
        assertFalse(sesion.enModoCliente());
    }

    @Test
    @DisplayName("Las contraseñas de la cuenta personal deben coincidir")
    void crearCuentaPersonal_contrasDistintas_falla() {
        Sesion sesion = iniciarComoEmpleado();

        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearCuentaPersonal(sesion, "ana.personal@gmail.com", "Personal@1", "Otra@1234"));
        assertFalse(empleado.tieneCuentaPersonal());
    }

    // ---------------- Cliente que ya existía ----------------

    @Test
    @DisplayName("Con un cliente de su mismo DNI, pide confirmar y muestra el correo enmascarado")
    void clienteExistente_porConfirmar() {
        clientes.guardar(new Cliente(0, DNI, "Ana", "Rojas Paz", null, "juana@gmail.com", null, "Personal@1", reloj));

        CuentaPersonal cuenta = servicio.consultarCuentaPersonal(empleado);

        assertEquals(CuentaPersonal.Estado.POR_CONFIRMAR, cuenta.estado());
        assertEquals("ju***@gmail.com", cuenta.correoEnmascarado());
    }

    @Test
    @DisplayName("Vincular un cliente existente exige su contraseña y conserva la misma cuenta")
    void vincularExistente_conContrasena() {
        Cliente existente = new Cliente(0, DNI, "Ana", "Rojas Paz", null, "juana@gmail.com", null, "Personal@1", reloj);
        clientes.guardar(existente);
        Sesion sesion = iniciarComoEmpleado();

        assertThrows(ReglaNegocioException.class, () -> servicio.vincularCuentaExistente(sesion, "Equivocada1"));
        assertFalse(empleado.tieneCuentaPersonal());

        servicio.vincularCuentaExistente(sesion, "Personal@1");
        assertSame(existente, sesion.cliente(), "es la misma cuenta, con sus puntos e historial");
        assertTrue(sesion.enModoCliente());
    }

    @Test
    @DisplayName("Con la cuenta personal desactivada no pasa a modo cliente")
    void cuentaPersonalDesactivada_noPasa() {
        Sesion sesion = iniciarComoEmpleado();
        servicio.crearCuentaPersonal(sesion, "ana.personal@gmail.com", "Personal@1", "Personal@1");
        servicio.volverAModoEmpleado(sesion, PIN);
        empleado.getCuentaPersonal().desactivar();

        assertThrows(ReglaNegocioException.class, () -> servicio.pasarAModoCliente(sesion));
        assertFalse(sesion.enModoCliente());
    }

    @Test
    @DisplayName("Registrar un cliente con un DNI que ya existe falla con un mensaje claro")
    void registrarCliente_dniRepetido_falla() {
        clientes.guardar(new Cliente(0, DNI, "Ana", "Rojas Paz", null, "juana@gmail.com", null, "Personal@1", reloj));

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class, () -> servicio.registrarCliente(
                DNI, "Otra", "Persona", null, "otra@gmail.com", null, "Personal@1", "Personal@1"));
        assertTrue(e.getMessage().contains("DNI"));
    }

    // ---------------- PIN ----------------

    private Sesion enModoCliente() {
        Sesion sesion = iniciarComoEmpleado();
        servicio.crearCuentaPersonal(sesion, "ana.personal@gmail.com", "Personal@1", "Personal@1");
        return sesion;
    }

    @Test
    @DisplayName("Con el PIN correcto vuelve al panel")
    void pinCorrecto_vuelveAlPanel() {
        Sesion sesion = enModoCliente();

        servicio.volverAModoEmpleado(sesion, PIN);

        assertFalse(sesion.enModoCliente());
        assertSame(empleado, sesion.empleadoEnPanel());
    }

    @Test
    @DisplayName("Un PIN incorrecto avisa cuántos intentos quedan y no cambia de modo")
    void pinIncorrecto_avisaIntentos() {
        Sesion sesion = enModoCliente();

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> servicio.volverAModoEmpleado(sesion, "0000"));

        assertTrue(e.getMessage().contains("2 intentos"));
        assertTrue(sesion.enModoCliente());
        assertFalse(sesion.cerradaPorSeguridad());
    }

    @Test
    @DisplayName("Tres PIN incorrectos seguidos cierran la sesión por seguridad")
    void tresPinIncorrectos_cierranLaSesion() {
        Sesion sesion = enModoCliente();

        for (int i = 0; i < AutenticacionService.MAX_INTENTOS_PIN; i++) {
            assertThrows(ReglaNegocioException.class, () -> servicio.volverAModoEmpleado(sesion, "0000"));
        }

        assertTrue(sesion.cerradaPorSeguridad());
        assertThrows(ReglaNegocioException.class, () -> servicio.volverAModoEmpleado(sesion, PIN),
                "ni el PIN correcto sirve después del cierre");
    }

    @Test
    @DisplayName("Acertar el PIN reinicia los intentos fallidos")
    void pinCorrecto_reiniciaIntentos() {
        Sesion sesion = enModoCliente();
        assertThrows(ReglaNegocioException.class, () -> servicio.volverAModoEmpleado(sesion, "0000"));
        assertThrows(ReglaNegocioException.class, () -> servicio.volverAModoEmpleado(sesion, "0000"));
        servicio.volverAModoEmpleado(sesion, PIN);
        servicio.pasarAModoCliente(sesion);

        ReglaNegocioException e = assertThrows(ReglaNegocioException.class,
                () -> servicio.volverAModoEmpleado(sesion, "0000"));

        assertTrue(e.getMessage().contains("2 intentos"));
    }

    @Test
    @DisplayName("Cambiar el PIN exige el actual, 4 dígitos y la confirmación")
    void cambiarPin() {
        assertThrows(ReglaNegocioException.class, () -> servicio.cambiarPin(empleado, PIN, "1111", "2222"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarPin(empleado, "9999", "1111", "1111"));
        assertThrows(IllegalArgumentException.class, () -> servicio.cambiarPin(empleado, PIN, "12a4", "12a4"));

        servicio.cambiarPin(empleado, PIN, "1111", "1111");

        assertTrue(empleado.verificarPin("1111"));
        assertFalse(empleado.verificarPin(PIN));
    }

    @Test
    @DisplayName("Un empleado no se puede crear con un PIN que no tenga 4 dígitos")
    void pinInvalido_alCrearEmpleado() {
        assertThrows(IllegalArgumentException.class, () -> new Empleado(0, "87654321", "Luis", "Paz", null,
                "luis@cine.pe", null, CONTRA_TRABAJO, null, empleado.getRol(), empleado.getSede(), "12", reloj));
    }

    // ---------------- Repositorios en memoria ----------------

    private static final class ClientesEnMemoria implements ClienteRepository {
        private final List<Cliente> lista = new ArrayList<>();

        @Override public void guardar(Cliente cliente) {
            if (cliente.getIdCliente() == 0) {
                cliente.asignarId(lista.size() + 1);
                lista.add(cliente);
            }
        }
        @Override public Optional<Cliente> buscarPorId(int id) {
            return lista.stream().filter(c -> c.getIdCliente() == id).findFirst();
        }
        @Override public List<Cliente> listarTodos() { return lista; }
    }

    private static final class EmpleadosEnMemoria implements EmpleadoRepository {
        private final List<Empleado> lista = new ArrayList<>();

        @Override public void guardar(Empleado empleado) {
            if (empleado.getIdEmpleado() == 0) {
                empleado.asignarId(lista.size() + 1);
                lista.add(empleado);
            }
        }
        @Override public Optional<Empleado> buscarPorId(int id) {
            return lista.stream().filter(e -> e.getIdEmpleado() == id).findFirst();
        }
        @Override public List<Empleado> listarTodos() { return lista; }
    }
}
