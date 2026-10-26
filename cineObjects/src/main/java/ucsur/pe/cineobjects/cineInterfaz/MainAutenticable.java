package ucsur.pe.cineobjects.cineInterfaz;

import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.Empleado;
import ucsur.pe.cineobjects.Rol;
import ucsur.pe.cineobjects.Sede;

/** Demuestra polimorfismo: Cliente y Empleado se autentican a través de la interfaz Autenticable. */
public class MainAutenticable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ AUTENTICABLE ===");

        Cliente cliente = new Cliente(1, "12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123");

        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        Rol rol = new Rol(1, "Dulceria", "Atiende pedidos", Rol.NIVEL_DULCERIA);
        Empleado empleado = new Empleado(1, "87654321", "Ana", "Perez",
                new DateTime(1, 1, 1995), "ana@cine.com", null, "empleado1", "MANANA", rol, sede);

        List<Autenticable> cuentas = new ArrayList<>();
        cuentas.add(cliente);
        cuentas.add(empleado);

        System.out.println("\n--- INICIO DE SESION ---");
        System.out.println("Cliente (clave correcta): " + cuentas.get(0).iniciarSesion("ismael@mail.com", "clave123"));
        System.out.println("Cliente (clave incorrecta): " + cuentas.get(0).iniciarSesion("ismael@mail.com", "otra"));
        System.out.println("Empleado (clave correcta): " + cuentas.get(1).iniciarSesion("ana@cine.com", "empleado1"));

        System.out.println("\n--- MODIFICAR DATOS ---");
        for (Autenticable cuenta : cuentas) {
            System.out.println(cuenta.getClass().getSimpleName() + ": "
                    + cuenta.modificarDatos("999888777", "nuevo_" + System.nanoTime() % 1000 + "@mail.com"));
            System.out.println(cuenta.getClass().getSimpleName() + " (correo invalido): "
                    + cuenta.modificarDatos("999888777", "correo-sin-arroba"));
        }
    }
}
