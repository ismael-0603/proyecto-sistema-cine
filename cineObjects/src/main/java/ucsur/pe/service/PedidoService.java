package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.PedidoRepository;
import ucsur.pe.repository.ProductoRepository;
import java.util.List;

/**
 * Por qué existe: casos de uso "Comprar dulcería" (menú, RF-12 y RF-31) y "Preparar / Entregar pedido"
 * (RF-15, RF-34). Exige que quien atiende tenga el permiso ATENDER_PEDIDO.
 */
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos) {
        this.pedidos = pedidos;
        this.productos = productos;
    }

    /** Productos que el cliente puede pedir en la sede: activos, con stock y para su nivel de socio. */
    public List<Producto> consultarMenu(Sede sede, Cliente cliente) {
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return productos.listarTodos().stream()
                .filter(p -> p.getStock(sede) > 0 && p.validarElegibilidad(cliente))
                .toList();
    }

    /** Cola de pedidos de la sede del empleado que esperan o se están preparando. */
    public List<Pedido> consultarPendientes(Empleado empleado) {
        exigirPermiso(empleado);
        return pedidos.listarPorSede(empleado.getSede()).stream()
                .filter(p -> Pedido.PENDIENTE.equals(p.getEstado()) || Pedido.EN_PREPARACION.equals(p.getEstado()))
                .toList();
    }

    public void prepararPedido(Empleado empleado, Pedido pedido) {
        exigirPedidoDeSuSede(empleado, pedido);
        pedido.iniciarPreparacion(empleado); // exige un pedido registrado y PENDIENTE
        pedidos.guardar(pedido);
    }

    /** Pasa el pedido a LISTO y devuelve el aviso para el cliente (RF-34). Solo quien lo preparó. */
    public String marcarListo(Empleado empleado, Pedido pedido) {
        exigirPedidoDeSuSede(empleado, pedido);
        if (pedido.getEmpleado() != empleado) {
            throw new ReglaNegocioException("Solo el empleado que preparó el pedido puede marcarlo como listo");
        }
        String aviso = pedido.marcarListo();
        pedidos.guardar(pedido);
        return aviso;
    }

    /**
     * Entrega el pedido solo si el cliente muestra el QR correcto. Lo puede entregar cualquier empleado
     * activo de la sede (por ejemplo, si cambió el turno): el QR es lo que prueba que el pedido es del cliente.
     */
    public void entregarPedido(Empleado empleado, Pedido pedido, String codigoQR) {
        exigirPedidoDeSuSede(empleado, pedido);
        if (!pedido.validarCodigo(codigoQR)) {
            throw new ReglaNegocioException("El código QR no corresponde al pedido");
        }
        pedido.entregar(); // exige un pedido LISTO; si no, lanza el error con el estado actual
        pedidos.guardar(pedido);
    }

    /** Aviso para el panel de dulcería: productos con stock bajo o agotados en la sede del empleado. */
    public List<Producto> consultarStockBajo(Empleado empleado) {
        exigirPermiso(empleado);
        return productos.listarTodos().stream().filter(p -> p.requiereReposicion(empleado.getSede())).toList();
    }

    /** El empleado debe tener la cuenta activa y el permiso ATENDER_PEDIDO. */
    private void exigirPermiso(Empleado empleado) {
        if (empleado == null || !empleado.isEstado()) {
            throw new ReglaNegocioException("La cuenta del empleado no está activa");
        }
        if (!empleado.getRol().tienePermiso(Rol.ATENDER_PEDIDO)) {
            throw new ReglaNegocioException("El empleado no tiene permiso para atender pedidos");
        }
    }

    /** Además del permiso, el pedido debe ser de la sede donde trabaja el empleado. */
    private void exigirPedidoDeSuSede(Empleado empleado, Pedido pedido) {
        exigirPermiso(empleado);
        if (pedido == null) throw new IllegalArgumentException("El pedido es obligatorio");
        if (!empleado.getSede().equals(pedido.getSede())) {
            throw new ReglaNegocioException("El pedido es de otra sede; el empleado atiende en " + empleado.getSede().getNombre());
        }
    }
}
