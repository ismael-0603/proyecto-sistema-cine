package ucsur.pe.demo;

import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Persona;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;

/**
 * Por qué existe: prueba del caso de uso "Iniciar sesión" y de la edición de datos, pasando por
 * LoginController. Cliente y Empleado se tratan como Persona (la clase padre abstracta).
 */
public class MainInicioSesion {

    private static String resultado(Respuesta<?> r) {
        return r.ok() ? "true" : "false (" + r.mensaje() + ")";
    }

    public static void main(String[] args) {
        App app = new App();
        System.out.println("=== INICIO DE SESION (Persona) ===");

        Cliente cliente = app.login().registrarCliente("12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123").dato();

        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        Rol rol = new Rol(1, "Dulceria", "Atiende pedidos", Rol.NIVEL_DULCERIA);
        Empleado empleado = new Empleado(1, "87654321", "Ana", "Perez",
                new DateTime(1, 1, 1995), "ana@cine.com", null, "empleado1", "MANANA", rol, sede);
        app.empleados().guardar(empleado);

        System.out.println("\n--- INICIO DE SESION ---");
        System.out.println("Cliente (clave correcta): " + resultado(app.login().iniciarSesion("ismael@mail.com", "clave123")));
        System.out.println("Cliente (clave incorrecta): " + resultado(app.login().iniciarSesion("ismael@mail.com", "otra")));
        System.out.println("Correo no registrado: " + resultado(app.login().iniciarSesion("nadie@mail.com", "clave123")));
        Respuesta<Persona> sesionEmpleado = app.login().iniciarSesion("ana@cine.com", "empleado1");
        System.out.println("Empleado (clave correcta): " + resultado(sesionEmpleado)
                + " -> " + sesionEmpleado.dato().getClass().getSimpleName());
        empleado.desactivar();
        System.out.println("Empleado desactivado: " + resultado(app.login().iniciarSesion("ana@cine.com", "empleado1")));
        empleado.activar();

        System.out.println("\n--- MODIFICAR DATOS ---");
        System.out.println("Cliente: " + resultado(app.login().actualizarDatos(cliente, "999888777", "ismael.nuevo@mail.com")));
        System.out.println("Cliente (correo invalido): " + resultado(app.login().actualizarDatos(cliente, "999888777", "correo-sin-arroba")));
        System.out.println("Cliente (correo de otra cuenta): " + resultado(app.login().actualizarDatos(cliente, "999888777", "ana@cine.com")));
        System.out.println("Empleado: " + resultado(app.login().actualizarDatos(empleado, "988777666", "ana.perez@cine.com")));
        System.out.println("Correo vigente del cliente: " + cliente.getCorreo());
    }
}
