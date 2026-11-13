package ucsur.pe.controller;

import ucsur.pe.model.Butaca;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Promocion;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.service.CompraService;
import java.util.List;

/** Acciones de la compra: butacas, dulcería, descuentos, cancelación y comprobante. */
public class CompraController extends ControllerBase {
    private final CompraService servicio;

    public CompraController(CompraService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<Compra> iniciarCompra(Cliente cliente, Sede sede) {
        return ejecutar("Compra iniciada: tienes " + Compra.TIEMPO_MAXIMO_COMPRA_MIN + " minutos",
                () -> servicio.iniciarCompra(cliente, sede));
    }

    public Respuesta<List<Butaca>> recomendarAsientos(Funcion funcion, int cantidad) {
        return ejecutar("", () -> servicio.recomendarAsientos(funcion, cantidad));
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

    public Respuesta<Void> asignarPedido(Compra compra, Pedido pedido) {
        return ejecutar("Pedido agregado", () -> servicio.asignarPedido(compra, pedido));
    }

    public Respuesta<Void> aplicarCupon(Compra compra, String codigo) {
        return ejecutar("Cupón aplicado", () -> servicio.aplicarCupon(compra, codigo));
    }

    public Respuesta<Void> aplicarPromocion(Compra compra, Promocion promocion) {
        return ejecutar("Promoción aplicada", () -> servicio.aplicarPromocion(compra, promocion));
    }

    public Respuesta<Void> validarBeneficiosSocio(Compra compra) {
        return ejecutar("Beneficios válidos", () -> servicio.validarBeneficiosSocio(compra));
    }

    public Respuesta<Void> cancelar(Compra compra) {
        return ejecutar("Compra cancelada", () -> servicio.cancelar(compra));
    }

    public Respuesta<String> generarComprobante(Compra compra) {
        return ejecutar("", () -> servicio.generarComprobante(compra));
    }
}
