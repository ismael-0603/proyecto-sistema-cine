package ucsur.pe.controller;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.CuentaPersonal;
import ucsur.pe.service.Sesion;

/**
 * Acciones de la pantalla de inicio: entrar como invitado, registrarse, iniciar sesión y editar datos;
 * y el paso del empleado entre su panel y su cuenta personal.
 */
public class LoginController extends ControllerBase {
    private final AutenticacionService servicio;

    public LoginController(AutenticacionService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Cliente> ingresarComoInvitado(String correo) {
        return ejecutar("Bienvenido", () -> servicio.ingresarComoInvitado(correo));
    }

    public Respuesta<Cliente> registrarCliente(String dni, String nombres, String apellidos, DateTime fechaNac,
                                               String correo, String telefono, String contra,
                                               String confirmarContra) {
        return ejecutar("Cuenta registrada", () -> servicio.registrarCliente(dni, nombres, apellidos, fechaNac,
                correo, telefono, contra, confirmarContra));
    }

    /** La sesión dice a qué zona entra la cuenta (cliente, dulcería o administración). */
    public Respuesta<Sesion> iniciarSesion(String correo, String contra) {
        return ejecutar("Sesión iniciada", () -> servicio.iniciarSesion(correo, contra));
    }

    /** «¿Olvidaste tu contraseña?» paso 1: envía un código al correo; el mensaje es el mismo exista o no la cuenta. */
    public Respuesta<Void> solicitarRecuperacion(String correo) {
        Respuesta<String> r = ejecutar("", () -> servicio.solicitarRecuperacion(correo));
        return r.ok() ? Respuesta.exito(r.dato(), null) : Respuesta.error(r.mensaje());
    }

    /** Paso 2: con el código del correo se elige la contraseña nueva (y su confirmación). */
    public Respuesta<Void> restablecerContrasena(String correo, String codigo, String nueva, String confirmar) {
        return ejecutar("Contraseña cambiada; ya puedes iniciar sesión",
                () -> servicio.restablecerContrasena(correo, codigo, nueva, confirmar));
    }

    public Respuesta<Void> actualizarDatos(Cliente cliente, String telefono, String correo) {
        return ejecutar("Datos actualizados", () -> servicio.actualizarDatos(cliente, telefono, correo));
    }

    public Respuesta<Void> actualizarDatos(Empleado empleado, String telefono, String correo) {
        return ejecutar("Datos actualizados", () -> servicio.actualizarDatos(empleado, telefono, correo));
    }

    // ---------- Cuenta personal y PIN del empleado ----------

    public Respuesta<CuentaPersonal> consultarCuentaPersonal(Empleado empleado) {
        return ejecutar("", () -> servicio.consultarCuentaPersonal(empleado));
    }

    public Respuesta<Void> pasarAModoCliente(Sesion sesion) {
        return ejecutar("", () -> servicio.pasarAModoCliente(sesion));
    }

    public Respuesta<Void> vincularCuentaExistente(Sesion sesion, String contraPersonal) {
        return ejecutar("Cuenta personal vinculada", () -> servicio.vincularCuentaExistente(sesion, contraPersonal));
    }

    public Respuesta<Void> crearCuentaPersonal(Sesion sesion, String correo, String contra, String confirmar) {
        return ejecutar("Cuenta personal creada", () -> servicio.crearCuentaPersonal(sesion, correo, contra, confirmar));
    }

    /** Si falla 3 veces seguidas, sesion.cerradaPorSeguridad() queda en true. */
    public Respuesta<Void> volverAModoEmpleado(Sesion sesion, String pin) {
        return ejecutar("", () -> servicio.volverAModoEmpleado(sesion, pin));
    }

    public Respuesta<Void> cambiarPin(Empleado empleado, String actual, String nuevo, String confirmar) {
        return ejecutar("PIN actualizado", () -> servicio.cambiarPin(empleado, actual, nuevo, confirmar));
    }
}
