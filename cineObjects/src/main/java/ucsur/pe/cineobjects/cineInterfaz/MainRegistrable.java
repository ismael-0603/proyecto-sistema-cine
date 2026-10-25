/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ucsur.pe.cineobjects.cineInterfaz;
import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.DetallePedido;
import ucsur.pe.cineobjects.Pedido;
import ucsur.pe.cineobjects.Reclamo;
import ucsur.pe.cineobjects.Calificacion;

/**
 *
 * @author Usuario
 */

public class MainRegistrable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ REGISTRABLE ===");

        // 1. Creamos un cliente y una compra de prueba
        Cliente cliente = Cliente.ingresarComoInvitado("invitado@cine.com");

        Compra compraPrueba = cliente.realizarCompra();

        // Simulación únicamente para probar los registros.
        // No sustituye el proceso real de confirmación de una compra.
        compraPrueba.setEstado(Compra.CONFIRMADA);

        // 2. Creamos un producto
        Producto producto = new Producto(
                1,
                "Canchita mediana",
                "Canchita para compartir",
                "Dulceria",
                12.00,
                false,
                20,
                true
        );

        // 3. Creamos el detalle del pedido
        DetallePedido detalle = new DetallePedido(
                1,
                producto,
                2,
                null
        );

        // 4. Creamos el pedido y agregamos el detalle
        Pedido pedidoValido = new Pedido(1);
        pedidoValido.agregarDetalle(detalle);

        // 5. Creamos un reclamo válido
        Reclamo reclamoValido = new Reclamo(
                1,
                Reclamo.RECLAMO,
                "El pedido demoró más de lo esperado",
                compraPrueba
        );

        // 6. Creamos una calificación válida
        Calificacion calificacionValida = new Calificacion(
                1,
                5,
                "Buena experiencia",
                compraPrueba
        );

        // 7. Creamos una lista que acepta objetos Registrable
        List<Registrable> registrosValidos = new ArrayList<>();

        registrosValidos.add(pedidoValido);
        registrosValidos.add(reclamoValido);
        registrosValidos.add(calificacionValida);

        // 8. Intentamos registrar cada objeto
        probarRegistros("REGISTROS VALIDOS", registrosValidos);

        // 9. Creamos objetos que incumplen reglas de negocio
        Pedido pedidoVacio = new Pedido(2);

        Reclamo reclamoInvalido = new Reclamo(
                2,
                "OTRO",
                "   ",
                compraPrueba
        );

        Calificacion calificacionInvalida = new Calificacion(
                2,
                6,
                "Puntuación fuera del rango",
                compraPrueba
        );

        List<Registrable> registrosRechazados = new ArrayList<>();

        registrosRechazados.add(pedidoVacio);
        registrosRechazados.add(reclamoInvalido);
        registrosRechazados.add(calificacionInvalida);

        // 10. Probamos los registros rechazados
        probarRegistros("REGISTROS RECHAZADOS", registrosRechazados);

        // 11. Intentamos registrar nuevamente los objetos anteriores
        probarRegistros("INTENTO DE REGISTRO DUPLICADO", registrosValidos);
    }

    public static void probarRegistros(
        String titulo,
        List<Registrable> elementos) {

    System.out.println("\n--- " + titulo + " ---");

    for (Registrable elemento : elementos) {

        boolean resultado;

        try {
            resultado = elemento.registrar();

        } catch (IllegalArgumentException | IllegalStateException ex) {
            resultado = false;
            System.out.println("Motivo del rechazo: " + ex.getMessage());
        }

        System.out.println(
                elemento.getClass().getSimpleName()
                + ": "
                + resultado
                + (resultado
                    ? " - Registro exitoso"
                    : " - Registro rechazado")
            );
        }
    }
}