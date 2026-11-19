package ucsur.pe.controller;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.service.AutenticacionService;
import ucsur.pe.service.Sesion;

/** Acciones de la pantalla de inicio: entrar como invitado, registrarse, iniciar sesión y editar datos. */
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
}
