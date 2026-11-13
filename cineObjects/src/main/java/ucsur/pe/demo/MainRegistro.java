package ucsur.pe.demo;

import ucsur.pe.App;
import ucsur.pe.controller.Respuesta;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Reclamo;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import ucsur.pe.model.TransaccionPago;

/**
 * Por qué existe: prueba del registro de Pedido (en el modelo), y de Reclamo y Calificacion
 * (por PostventaController), incluidos los casos que se rechazan.
 */
public class MainRegistro {

    private static String resultado(Respuesta<?> r) {
        return r.ok() ? "true - Registro exitoso" : "false - Registro rechazado: " + r.mensaje();
    }

    /** El pedido se registra en el modelo: si algo no procede, lanza el error con el motivo. */
    private static String registrarPedido(Pedido pedido, Sede sede) {
        try {
            pedido.registrar(sede);
            return "true - Registro exitoso";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return "false - Registro rechazado: " + ex.getMessage();
        }
    }

    public static void main(String[] args) {
        System.out.println("=== REGISTRO DE PEDIDO, RECLAMO Y CALIFICACION ===");
        App app = new App();

        // 1. Una compra confirmada con el flujo real (entrada + pago)
        Cliente cliente = app.login().ingresarComoInvitado("invitado@cine.com").dato();
        Compra compraPrueba = crearCompraConfirmada(app, cliente);
        Sede sede = compraPrueba.getSede();

        // 2. Un pedido con un producto que tiene stock en la sede
        Producto producto = new Producto(1, "Canchita mediana", "Canchita para compartir", "Dulceria", 12.00, false, true);
        producto.reponerStock(sede, 20);
        Pedido pedidoValido = new Pedido(1);
        pedidoValido.agregarDetalle(new DetallePedido(1, producto, 2, null));

        System.out.println("\n--- REGISTROS VALIDOS ---");
        System.out.println("Pedido: " + registrarPedido(pedidoValido, sede));
        System.out.println("Reclamo: " + resultado(app.postventa().presentarReclamo(compraPrueba, Reclamo.RECLAMO,
                "El pedido demoró más de lo esperado")));
        System.out.println("Calificacion: " + resultado(app.postventa().calificar(compraPrueba, 5, "Buena experiencia")));

        System.out.println("\n--- DATOS INVALIDOS ---");
        System.out.println("Reclamo: " + resultado(app.postventa().presentarReclamo(compraPrueba, "OTRO", "   ")));
        Compra otraConfirmada = crearCompraConfirmada(app, cliente);
        System.out.println("Calificacion: " + resultado(app.postventa().calificar(otraConfirmada, 6, "Fuera de rango")));

        System.out.println("\n--- REGISTROS RECHAZADOS ---");
        System.out.println("Pedido vacio: " + registrarPedido(new Pedido(2), sede));
        Compra compraSinConfirmar = app.compra().iniciarCompra(cliente, sede).dato();
        System.out.println("Calificacion sin compra confirmada: "
                + resultado(app.postventa().calificar(compraSinConfirmar, 4, "La compra no está confirmada")));

        System.out.println("\n--- INTENTO DE REGISTRO DUPLICADO ---");
        System.out.println("Pedido: " + registrarPedido(pedidoValido, sede));
        System.out.println("Calificacion: " + resultado(app.postventa().calificar(compraPrueba, 3, "Otra vez")));
    }

    /** Arma una compra confirmada con el flujo real: butacas, entradas, bloqueo y pago aprobado. */
    private static Compra crearCompraConfirmada(App app, Cliente cliente) {
        Sede sede = new Sede(0, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
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

        TipoEntrada general = new TipoEntrada(1, "GENERAL", "Entrada general", 20.0, 2, false, true);
        Compra compra = app.compra().iniciarCompra(cliente, sede).dato();
        app.compra().agregarEntrada(compra, funcion, funcion.recomendarMejoresAsientos(1).get(0), general);
        app.compra().confirmarAsientos(compra);
        app.pago().pagar(compra, compra.getTotal(), TransaccionPago.TARJETA_CREDITO);
        return compra;
    }
}
