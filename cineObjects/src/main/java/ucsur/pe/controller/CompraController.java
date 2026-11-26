package ucsur.pe.controller;

import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.MapaButacas;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.service.CompraService;
import java.util.List;

/**
 * Acciones de la compra: butacas, dulcería, descuentos y cancelación. El comprobante (ver, imprimir o enviar)
 * está en EmisionController.
 */
public class CompraController extends ControllerBase {
    private final CompraService servicio;

    public CompraController(CompraService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Compra> iniciarCompra(Cliente cliente, Sede sede) {
        return ejecutar("Compra iniciada: tienes " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos",
                () -> servicio.iniciarCompra(cliente, sede));
    }

    /** Mapa de la sala con las butacas libres y la sugerencia marcada; si hay aviso, va en el mensaje. */
    public Respuesta<MapaButacas> consultarMapa(Compra compra, Funcion funcion, int cantidad, boolean incluirAccesibles) {
        Respuesta<MapaButacas> r = ejecutar("", () -> servicio.consultarMapa(compra, funcion, cantidad, incluirAccesibles));
        return (r.ok() && r.dato().aviso() != null) ? Respuesta.exito(r.dato().aviso(), r.dato()) : r;
    }

    /** Tarifas que este cliente puede elegir (General, Niño, Socio...). */
    public Respuesta<List<TipoEntrada>> listarTiposEntrada(Cliente cliente) {
        return ejecutar("", () -> servicio.listarTiposEntrada(cliente));
    }

    public Respuesta<Entrada> agregarEntrada(Compra compra, Funcion funcion, Butaca butaca, TipoEntrada tipo) {
        return ejecutar("Butaca agregada", () -> servicio.agregarEntrada(compra, funcion, butaca, tipo));
    }

    public Respuesta<Void> quitarEntrada(Compra compra, Entrada entrada) {
        return ejecutar("Butaca quitada", () -> servicio.quitarEntrada(compra, entrada));
    }

    public Respuesta<Void> confirmarAsientos(Compra compra) {
        return ejecutar("Butacas bloqueadas", () -> servicio.confirmarAsientos(compra));
    }

    public Respuesta<Void> liberarAsientos(Compra compra) {
        return ejecutar("Butacas liberadas: puedes cambiarlas", () -> servicio.liberarAsientos(compra));
    }

    // ---------- Dulcería ----------
    public Respuesta<DetallePedido> agregarProducto(Compra compra, Producto producto, int cantidad, String personalizacion) {
        return ejecutar("Producto agregado", () -> servicio.agregarProducto(compra, producto, cantidad, personalizacion));
    }

    public Respuesta<Void> cambiarCantidad(Compra compra, DetallePedido detalle, int cantidad) {
        return ejecutar("Cantidad actualizada", () -> servicio.cambiarCantidad(compra, detalle, cantidad));
    }

    public Respuesta<Void> quitarProducto(Compra compra, DetallePedido detalle) {
        return ejecutar("Producto quitado", () -> servicio.quitarProducto(compra, detalle));
    }

    public Respuesta<Void> aplicarCupon(Compra compra, String codigo) {
        return ejecutar("Cupón aplicado", () -> servicio.aplicarCupon(compra, codigo));
    }

    public Respuesta<Void> aplicarPromocion(Compra compra, Promocion promocion) {
        return ejecutar("Promoción aplicada", () -> servicio.aplicarPromocion(compra, promocion));
    }

    public Respuesta<Boolean> quitarCupon(Compra compra) {
        return ejecutar("Cupón retirado", () -> servicio.quitarCupon(compra));
    }

    public Respuesta<Boolean> quitarPromocion(Compra compra) {
        return ejecutar("Promoción retirada", () -> servicio.quitarPromocion(compra));
    }

    public Respuesta<Void> cancelar(Compra compra) {
        return ejecutar("Compra cancelada", () -> servicio.cancelar(compra));
    }

    public Respuesta<Void> expirar(Compra compra) {
        return ejecutar("Compra expirada", () -> servicio.expirar(compra));
    }

}
