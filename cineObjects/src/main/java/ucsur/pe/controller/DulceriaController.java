package ucsur.pe.controller;

import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Sede;
import ucsur.pe.service.PedidoService;
import java.util.List;

/** Acciones de la dulcería: el menú para el cliente y la cola de pedidos para el personal. */
public class DulceriaController extends ControllerBase {
    private final PedidoService servicio;

    public DulceriaController(PedidoService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<Producto>> consultarMenu(Sede sede, Cliente cliente) {
        return ejecutar("", () -> servicio.consultarMenu(sede, cliente));
    }

    public Respuesta<List<Pedido>> consultarPendientes(Empleado empleado) {
        return ejecutar("", () -> servicio.consultarPendientes(empleado));
    }

    public Respuesta<Void> prepararPedido(Empleado empleado, Pedido pedido) {
        return ejecutar("Pedido en preparación", () -> servicio.prepararPedido(empleado, pedido));
    }

    public Respuesta<String> marcarListo(Empleado empleado, Pedido pedido) {
        return ejecutar("Pedido listo", () -> servicio.marcarListo(empleado, pedido));
    }

    public Respuesta<Void> entregarPedido(Empleado empleado, Pedido pedido, String codigoQR) {
        return ejecutar("Pedido entregado", () -> servicio.entregarPedido(empleado, pedido, codigoQR));
    }
}
