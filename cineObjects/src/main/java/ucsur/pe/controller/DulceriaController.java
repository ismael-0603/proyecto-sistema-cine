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

    /** Columna «Listos para entregar» del panel de dulcería. */
    public Respuesta<List<Pedido>> consultarListos(Empleado empleado) {
        return ejecutar("", () -> servicio.consultarListos(empleado));
    }

    /** Columna «Entregados hoy» del panel de dulcería. */
    public Respuesta<List<Pedido>> consultarEntregadosHoy(Empleado empleado) {
        return ejecutar("", () -> servicio.consultarEntregadosHoy(empleado));
    }

    /** «Buscar pedido»: número de pedido o código del comprobante (B001-000123). */
    public Respuesta<Pedido> buscarPedido(Empleado empleado, String texto) {
        return ejecutar("", () -> servicio.buscarPedido(empleado, texto));
    }

    /** Productos de la sede del empleado que tienen stock bajo o están agotados. */
    public Respuesta<List<Producto>> consultarStockBajo(Empleado empleado) {
        return ejecutar("", () -> servicio.consultarStockBajo(empleado));
    }

    public Respuesta<Void> prepararPedido(Empleado empleado, Pedido pedido) {
        return ejecutar("Pedido en preparación", () -> servicio.prepararPedido(empleado, pedido));
    }

    public Respuesta<String> marcarListo(Empleado empleado, Pedido pedido) {
        return ejecutar("Pedido listo", () -> servicio.marcarListo(empleado, pedido));
    }

    /** El código es el QR del pedido o el código del comprobante de su compra. */
    public Respuesta<Void> entregarPedido(Empleado empleado, Pedido pedido, String codigo) {
        return ejecutar("Pedido entregado", () -> servicio.entregarPedido(empleado, pedido, codigo));
    }
}
