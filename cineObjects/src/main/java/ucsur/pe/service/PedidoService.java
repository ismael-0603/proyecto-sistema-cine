package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Rol;
import ucsur.pe.model.Sede;
import ucsur.pe.repository.CompraRepository;
import ucsur.pe.repository.PedidoRepository;
import ucsur.pe.repository.ProductoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Por qué existe: casos de uso "Comprar dulcería" (menú, RF-12 y RF-31) y "Preparar / Entregar pedido"
 * (RF-15, RF-34). Exige que quien atiende tenga el permiso ATENDER_PEDIDO.
 * Las compras sirven para encontrar un pedido por el código del comprobante (wireframes 15 y 16).
 */
public class PedidoService {
    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final CompraRepository compras;
    private final Reloj reloj; // para saber qué pedidos se entregaron hoy

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos, CompraRepository compras,
                         Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
        this.pedidos = pedidos;
        this.productos = productos;
        this.compras = compras;
        this.reloj = reloj;
    }

    /** Productos del menú de la sede para este cliente: activos y para su nivel de socio (los agotados van al final). */
    public List<Producto> consultarMenu(Sede sede, Cliente cliente) {
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return productos.listarTodos().stream()
                .filter(p -> p.validarElegibilidad(cliente))
                .sorted(Comparator.comparing(p -> p.estaAgotado(sede)))
                .toList();
    }

    /** Cola de pedidos de la sede del empleado que esperan o se están preparando. */
    public List<Pedido> consultarPendientes(Empleado empleado) {
        exigirPermiso(empleado);
        return pedidos.listarPorSede(empleado.getSede()).stream()
                .filter(p -> Pedido.PENDIENTE.equals(p.getEstado()) || Pedido.EN_PREPARACION.equals(p.getEstado()))
                .toList();
    }

    /** Columna «Listos para entregar» del panel (wireframe 15): pedidos LISTO de la sede del empleado. */
    public List<Pedido> consultarListos(Empleado empleado) {
        exigirPermiso(empleado);
        return pedidos.listarPorSede(empleado.getSede()).stream()
                .filter(p -> Pedido.LISTO.equals(p.getEstado()))
                .toList();
    }

    /** Columna «Entregados hoy» del panel (wireframe 15): pedidos ENTREGADO cuya entrega fue hoy. */
    public List<Pedido> consultarEntregadosHoy(Empleado empleado) {
        exigirPermiso(empleado);
        long hoy = reloj.ahora().toValorSoloFecha();
        return pedidos.listarPorSede(empleado.getSede()).stream()
                .filter(p -> Pedido.ENTREGADO.equals(p.getEstado()))
                .filter(p -> p.getFechaEntrega() != null && p.getFechaEntrega().toValorSoloFecha() == hoy)
                .toList();
    }

    /**
     * «Buscar pedido» del panel (wireframe 15): por número de pedido (0003 o #0003) o por el código
     * del comprobante (B001-000123). Solo encuentra pedidos de la sede del empleado.
     */
    public Pedido buscarPedido(Empleado empleado, String texto) {
        exigirPermiso(empleado);
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Escribe el número de pedido o el código del comprobante");
        }
        String buscado = texto.trim();
        Optional<Pedido> encontrado = pedidoDeLaSerie(buscado);
        if (encontrado.isEmpty()) {
            String digitos = buscado.startsWith("#") ? buscado.substring(1) : buscado;
            if (digitos.matches("\\d{1,9}")) {
                int id = Integer.parseInt(digitos);
                encontrado = pedidos.listarPorSede(empleado.getSede()).stream()
                        .filter(p -> p.getIdPedido() == id)
                        .findFirst();
            }
        }
        return encontrado
                .filter(p -> empleado.getSede().equals(p.getSede()))
                .orElseThrow(() -> new ReglaNegocioException("No hay un pedido con «" + buscado + "» en "
                        + empleado.getSede().getNombre()));
    }

    /** Pedido de la compra cuyo comprobante tiene esa serie, si la compra tiene dulcería. */
    private Optional<Pedido> pedidoDeLaSerie(String serie) {
        return compras.buscarPorSerie(serie).map(Compra::getPedido);
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
     * Entrega el pedido solo si el cliente muestra el QR del pedido o el código de su comprobante
     * (B001-000123; wireframe 16: «O ingresa el código»). Lo puede entregar cualquier empleado activo
     * de la sede (por ejemplo, si cambió el turno): el código es lo que prueba que el pedido es del cliente.
     */
    public void entregarPedido(Empleado empleado, Pedido pedido, String codigo) {
        exigirPedidoDeSuSede(empleado, pedido);
        boolean esSuComprobante = codigo != null
                && pedidoDeLaSerie(codigo.trim()).filter(p -> p == pedido).isPresent();
        if (!pedido.validarCodigo(codigo) && !esSuComprobante) {
            throw new ReglaNegocioException("El código no corresponde al pedido");
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
