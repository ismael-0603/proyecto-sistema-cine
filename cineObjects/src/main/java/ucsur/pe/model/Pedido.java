package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Por qué existe: es el pedido de dulcería. Descuenta el stock de la sede al registrarse y avanza
 * en orden: PENDIENTE, EN_PREPARACION, LISTO y ENTREGADO.
 */
public class Pedido implements Totalizable {
    // Estados de un pedido, en el orden en que avanzan (constantes: se usan como Pedido.NOMBRE y su valor no cambia).
    public static final String PENDIENTE = "PENDIENTE"; // registrado, esperando a un empleado
    public static final String EN_PREPARACION = "EN_PREPARACION"; // un empleado lo está preparando
    public static final String LISTO = "LISTO"; // listo para recoger; se avisa al cliente
    public static final String ENTREGADO = "ENTREGADO"; // el cliente ya lo recogió
    public static final String ANULADO = "ANULADO"; // su compra se canceló, expiró o se reembolsó; ya no se prepara ni se entrega

    private int idPedido;
    private String codigoQR; // null hasta registrarse; asignarId() lo rehace con el id generado por la BD.
    private DateTime fechaHora;
    private DateTime fechaEntrega;
    private String estado;
    private final Reloj reloj; // de dónde sale la hora actual
    private Empleado empleado; // Permanece null hasta que un empleado inicia la preparación.
    /** Sede de cuyo inventario se descuenta el stock; se fija al registrar el pedido. */
    private Sede sede;
    /** Productos y cantidades que forman el pedido. */
    private List<DetallePedido> detalles;
    /** Se activa al descontar el stock, para no registrar ni cobrar el pedido dos veces. */
    private boolean registrado;

    /** Recibe el reloj (el real o uno fijo en pruebas) para fechar la creación, el registro y la entrega. */
    public Pedido(int idPedido, Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
        this.reloj = reloj;
        this.idPedido = idPedido;
        this.fechaHora = reloj.ahora();
        this.estado = PENDIENTE;
        this.detalles = new ArrayList<>();
        this.registrado = false;
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un pedido tal como quedó guardado.
     * No vuelve a descontar ni a reponer stock. El pedido está registrado si tiene QR, porque el QR se crea
     * justo al registrarlo; solo un pedido registrado tiene sede
     * (en la BD la sede no está en pedido: es la de su compra).
     */
    public static Pedido restaurar(int idPedido, String codigoQR, DateTime fechaHora, DateTime fechaEntrega,
                                   String estado, Sede sede, Empleado empleado, List<DetallePedido> detalles,
                                   Reloj reloj) {
        Pedido pedido = new Pedido(idPedido, reloj); // valida el reloj
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha del pedido es obligatoria");
        }
        if (!esEstadoValido(estado)) {
            throw new IllegalArgumentException("Estado de pedido inválido: " + estado);
        }
        if (empleado == null && (EN_PREPARACION.equals(estado) || LISTO.equals(estado))) {
            throw new IllegalArgumentException("Un pedido " + estado + " necesita al empleado que lo prepara");
        }
        boolean registrado = (codigoQR != null);
        if (registrado && sede == null) {
            throw new IllegalArgumentException("Un pedido registrado necesita su sede");
        }
        pedido.codigoQR = codigoQR;
        pedido.fechaHora = fechaHora;
        pedido.fechaEntrega = fechaEntrega;
        pedido.estado = estado;
        pedido.empleado = empleado;
        pedido.registrado = registrado;
        pedido.sede = registrado ? sede : null;
        if (detalles != null) {
            for (DetallePedido d : detalles) {
                if (d == null) throw new IllegalArgumentException("El detalle no puede ser nulo");
                pedido.detalles.add(d);
            }
        }
        return pedido;
    }

    /** Los cinco estados posibles (en la BD, ck_pedido_estado). */
    private static boolean esEstadoValido(String estado) {
        return PENDIENTE.equals(estado) || EN_PREPARACION.equals(estado) || LISTO.equals(estado)
                || ENTREGADO.equals(estado) || ANULADO.equals(estado);
    }

    /** Asigna el id real generado por la base tras el INSERT; si el QR ya existe, lo rehace con ese id. */
    public void asignarId(int idPedido) {
        this.idPedido = Ids.asignar(this.idPedido, idPedido, "el pedido");
        if (codigoQR != null) {
            this.codigoQR = generarCodigoQR();
        }
    }

    /** Forma el QR con el id actual del pedido y su fecha; se rehace al asignar el id real de la BD. */
    private String generarCodigoQR() {
        return formarCodigoQR(idPedido, fechaHora);
    }

    /**
     * La fórmula del QR, en un solo lugar: la usa el pedido y también el DAO, que la necesita para escribir
     * el QR en la misma transacción del INSERT (el id lo genera la base y se asigna al objeto después del commit).
     */
    public static String formarCodigoQR(int idPedido, DateTime fechaHora) {
        return "QR-PED-" + idPedido + "-" + fechaHora.toValorComparable();
    }

    /** Agrega una línea antes del registro. */
    public void agregarDetalle(DetallePedido detalle) {
        agregarDetalle(detalle, null);
    }

    /** Agrega una línea validando además el stock acumulado del producto en la sede de la compra. */
    void agregarDetalle(DetallePedido detalle, Sede sede) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        exigirNoRegistrado();
        exigirStockDisponible(sede, detalle.getProducto(), cantidadDe(detalle.getProducto(), null) + detalle.getCantidad());
        detalles.add(detalle);
    }

    /** Cambia la cantidad de una línea mientras el pedido no esté registrado. */
    public void modificarCantidad(DetallePedido detalle, int cantidad) {
        modificarCantidad(detalle, cantidad, null);
    }

    /** Cambia la cantidad validando el stock acumulado del producto en la sede de la compra. */
    void modificarCantidad(DetallePedido detalle, int cantidad, Sede sede) {
        exigirNoRegistrado();
        if (!detalles.contains(detalle)) {
            throw new IllegalArgumentException("La línea no pertenece a este pedido");
        }
        if (cantidad > 0) {
            exigirStockDisponible(sede, detalle.getProducto(), cantidadDe(detalle.getProducto(), detalle) + cantidad);
        }
        detalle.modificarCantidad(cantidad);
    }

    private int cantidadDe(Producto producto, DetallePedido excluyendo) {
        if (producto == null) return 0;
        int total = 0;
        for (DetallePedido d : detalles) {
            if (d != excluyendo && d.getProducto().getIdProducto() == producto.getIdProducto()) {
                total += d.getCantidad();
            }
        }
        return total;
    }

    private static void exigirStockDisponible(Sede sede, Producto producto, int cantidadTotal) {
        if (sede == null || producto == null) return;
        int disponible = producto.getStock(sede);
        if (!producto.isEstado() || disponible < cantidadTotal) {
            throw new IllegalStateException("Stock insuficiente de " + producto.getNombre()
                    + " en " + sede.getNombre() + " (disponible: " + disponible + ")");
        }
    }

    /** Quita una línea del carrito mientras el pedido no esté registrado (RF-14). */
    public boolean quitarDetalle(DetallePedido detalle) {
        exigirNoRegistrado();
        return detalles.remove(detalle);
    }

    /** Después de registrarlo (stock descontado y cobrado) el pedido ya no se puede cambiar. */
    private void exigirNoRegistrado() {
        if (registrado) {
            throw new IllegalStateException("No se puede modificar un pedido ya registrado");
        }
    }

    /**
     * Registra el pedido en la sede indicada: verifica primero el stock de todo el pedido y luego lo descuenta.
     * Es el único camino para fijar la sede y registrar. Si algo no procede, lanza el error con el motivo.
     */
    public void registrar(Sede sede) {
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        if (registrado) {
            throw new IllegalStateException("El pedido ya fue registrado");
        }
        if (detalles.isEmpty()) {
            throw new IllegalStateException("El pedido no tiene productos");
        }
        if (!hayStock(sede)) {
            throw new IllegalStateException("No hay stock suficiente en " + sede.getNombre() + " para el pedido");
        }
        // Ya se verificó el total de cada producto, así que ningún descuento puede fallar a mitad de camino.
        for (DetallePedido d : detalles) {
            Producto articulo = d.getProducto();
            articulo.descontarStock(sede, d.getCantidad());
        }
        this.sede = sede;
        this.registrado = true;
        this.fechaHora = reloj.ahora();
        this.estado = PENDIENTE;
        this.codigoQR = generarCodigoQR();
    }

    /**
     * Revisa, sin descontar nada, si la sede tiene stock para todo el pedido.
     * Suma las cantidades del mismo producto aunque esté en varias líneas (por ejemplo, 3 + 3 canchitas),
     * porque el stock se descuenta del mismo producto.
     */
    public boolean hayStock(Sede sede) {
        if (sede == null || detalles.isEmpty()) return false;
        Map<Producto, Integer> totalPorProducto = new LinkedHashMap<>();
        for (DetallePedido d : detalles) {
            totalPorProducto.merge(d.getProducto(), d.getCantidad(), Integer::sum);
        }
        for (Map.Entry<Producto, Integer> item : totalPorProducto.entrySet()) {
            if (!item.getKey().verificarStock(sede, item.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Anula el pedido cuando su compra se cancela, expira o se reembolsa. Solo vuelve el stock si el pedido
     * estaba registrado y seguía PENDIENTE (nadie lo preparó); si ya estaba en preparación o listo,
     * los productos ya se usaron. No es pública: solo la llama Compra.
     */
    void anular() {
        if (ENTREGADO.equals(estado)) {
            throw new IllegalStateException("No se puede anular un pedido ya entregado");
        }
        if (ANULADO.equals(estado)) return;
        if (registrado && PENDIENTE.equals(estado)) {
            for (DetallePedido d : detalles) {
                d.getProducto().reponerStock(sede, d.getCantidad());
            }
        }
        this.estado = ANULADO;
    }

    /** Suma los subtotales de todas las líneas; el total no se guarda: siempre sale de las líneas. */
    @Override
    public long calcularTotalCentavos() {
        long suma = 0;
        for (DetallePedido d : detalles) {
            suma += d.calcularSubtotalCentavos();
        }
        return suma;
    }

    /** Pasa a EN_PREPARACION; exige un pedido registrado y PENDIENTE, y un empleado que pueda atender pedidos. */
    public void iniciarPreparacion(Empleado empleado) {
        if (!registrado || !PENDIENTE.equals(estado)) {
            throw new IllegalStateException("Solo se puede preparar un pedido registrado y PENDIENTE (estado actual: " + estado + ")");
        }
        if (empleado == null) {
            throw new IllegalArgumentException("El empleado es obligatorio");
        }
        if (!empleado.getRol().tienePermiso(Rol.ATENDER_PEDIDO)) {
            throw new IllegalStateException("El empleado no tiene permiso para atender pedidos");
        }
        this.empleado = empleado;
        this.estado = EN_PREPARACION;
    }

    /** Pasa de EN_PREPARACION a LISTO y devuelve el aviso para el cliente. */
    public String marcarListo() {
        if (!EN_PREPARACION.equals(estado)) {
            throw new IllegalStateException("Solo se puede marcar LISTO un pedido EN_PREPARACION (estado actual: " + estado + ")");
        }
        this.estado = LISTO;
        return notificarCliente();
    }

    /** Arma el aviso que recibe el cliente cuando el pedido pasa a LISTO; quien lo muestra es la pantalla del panel de dulcería. */
    private String notificarCliente() {
        return "Notificación: Pedido #" + idPedido + " listo para recoger. QR: " + codigoQR;
    }

    /** Compara el código recibido con el QR asociado al pedido. */
    public boolean validarCodigo(String codigo) {
        return this.codigoQR != null && this.codigoQR.equals(codigo);
    }

    /** Pasa de LISTO a ENTREGADO y guarda la fecha de entrega. */
    public void entregar() {
        if (!LISTO.equals(estado)) {
            throw new IllegalStateException("Solo se puede entregar un pedido LISTO (estado actual: " + estado + ")");
        }
        this.estado = ENTREGADO;
        this.fechaEntrega = reloj.ahora();
    }

    public int getIdPedido() { return idPedido; }
    public String getCodigoQR() { return codigoQR; }
    public DateTime getFechaHora() { return fechaHora; }
    public DateTime getFechaEntrega() { return fechaEntrega; }
    public String getEstado() { return estado; }
    public long getTotalCentavos() { return calcularTotalCentavos(); }
    public Empleado getEmpleado() { return empleado; }
    public Sede getSede() { return sede; }
    public List<DetallePedido> getDetalles() { return new ArrayList<>(detalles); }
    public boolean isRegistrado() { return registrado; }

    @Override
    public String toString() {
        return "Pedido #" + idPedido + " [" + estado + "] " + ConversorDinero.formatear(calcularTotalCentavos());
    }
}
