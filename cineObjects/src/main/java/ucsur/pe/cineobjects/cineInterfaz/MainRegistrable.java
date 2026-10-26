/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ucsur.pe.cineobjects.cineInterfaz;
import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Butaca;
import ucsur.pe.cineobjects.Calificacion;
import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.DetallePedido;
import ucsur.pe.cineobjects.Entrada;
import ucsur.pe.cineobjects.Funcion;
import ucsur.pe.cineobjects.Pedido;
import ucsur.pe.cineobjects.Pelicula;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.Reclamo;
import ucsur.pe.cineobjects.Sala;
import ucsur.pe.cineobjects.Sede;
import ucsur.pe.cineobjects.TipoEntradaGeneral;
import ucsur.pe.cineobjects.TipoSala;
import ucsur.pe.cineobjects.TransaccionPago;

/**
 *
 * @author Usuario
 */

public class MainRegistrable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ REGISTRABLE ===");

        // 1. Creamos un cliente y una compra confirmada con el flujo real (entrada + pago)
        Cliente cliente = Cliente.ingresarComoInvitado("invitado@cine.com");
        Compra compraPrueba = crearCompraConfirmada(cliente);

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

        // 9. Los datos inválidos se rechazan al construir el objeto (los constructores validan)
        System.out.println("\n--- DATOS INVALIDOS (rechazados al construir) ---");

        try {
            new Reclamo(2, "OTRO", "   ", compraPrueba);
        } catch (IllegalArgumentException ex) {
            System.out.println("Reclamo: " + ex.getMessage());
        }

        try {
            new Calificacion(2, 6, "Puntuación fuera del rango", compraPrueba);
        } catch (IllegalArgumentException ex) {
            System.out.println("Calificacion: " + ex.getMessage());
        }

        // 10. Los objetos válidos que incumplen reglas de negocio se rechazan al registrar
        Pedido pedidoVacio = new Pedido(2);
        Compra compraSinConfirmar = cliente.realizarCompra();
        Calificacion calificacionSinCompraConfirmada =
                new Calificacion(3, 4, "La compra no está confirmada", compraSinConfirmar);

        List<Registrable> registrosRechazados = new ArrayList<>();

        registrosRechazados.add(pedidoVacio);
        registrosRechazados.add(calificacionSinCompraConfirmada);

        probarRegistros("REGISTROS RECHAZADOS", registrosRechazados);

        // 11. Intentamos registrar nuevamente los objetos anteriores
        probarRegistros("INTENTO DE REGISTRO DUPLICADO", registrosValidos);
    }

    /** Arma una compra confirmada con el flujo real: butacas, entradas, bloqueo y pago aprobado. */
    private static Compra crearCompraConfirmada(Cliente cliente) {
        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        Sala sala = new Sala(1, 1, 10, Sala.ACTIVA, sede, new TipoSala(1, "2D", "Sala estandar", 0));
        sede.agregarSala(sala);
        for (int i = 1; i <= 5; i++) {
            sala.agregarButaca(new Butaca(0, "A", i, Butaca.ESTANDAR, sala));
        }

        Pelicula pelicula = new Pelicula(1, "Pelicula de prueba", "Sinopsis", 120, "+14",
                null, null, new DateTime(1, 1, 2026), Pelicula.EN_CARTELERA);
        DateTime manana = DateTime.ahora().sumarDias(1);
        Funcion funcion = new Funcion(1, new DateTime(manana.getDia(), manana.getMes(), manana.getAnio()),
                new DateTime(19, 0), "Doblada", null, pelicula, sala);
        sede.agregarFuncion(funcion);

        TipoEntradaGeneral general = new TipoEntradaGeneral(1, "GENERAL", "Entrada general", 20.0, false, true);
        Compra compra = cliente.realizarCompra();
        compra.agregarEntrada(new Entrada(0, funcion, funcion.recomendarMejoresAsientos(1).get(0), general));
        compra.confirmarSeleccionAsientos();

        new TransaccionPago(0, compra.getTotal(), TransaccionPago.TARJETA, compra).procesarPago();
        return compra;
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
