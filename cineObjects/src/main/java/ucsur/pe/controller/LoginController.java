package ucsur.pe.controller;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;
import ucsur.pe.service.AutenticacionService;

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
                                               String correo, String telefono, String contra) {
        return ejecutar("Cuenta registrada",
                () -> servicio.registrarCliente(dni, nombres, apellidos, fechaNac, correo, telefono, contra));
    }

    public Respuesta<Persona> iniciarSesion(String correo, String contra) {
        return ejecutar("Sesión iniciada", () -> servicio.iniciarSesion(correo, contra));
    }

    public Respuesta<Void> actualizarDatos(Cliente cliente, String telefono, String correo) {
        return ejecutar("Datos actualizados", () -> servicio.actualizarDatos(cliente, telefono, correo));
    }

    public Respuesta<Void> actualizarDatos(Empleado empleado, String telefono, String correo) {
        return ejecutar("Datos actualizados", () -> servicio.actualizarDatos(empleado, telefono, correo));
    }
}
