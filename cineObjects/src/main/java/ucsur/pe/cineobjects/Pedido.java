package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 * Agrupa productos de dulcería y controla el ciclo de preparación y entrega del pedido.
 * Solo registra pedidos con detalles y stock suficiente; los cambios de estado respetan el orden del proceso.
 */
public class Pedido {
    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_PREPARACION = "EN_PREPARACION";
    public static final String LISTO = "LISTO";
    public static final String ENTREGADO = "ENTREGADO";

    private int idPedido;
    private String codigoQR;
    private DateTime fechaHora;
    private DateTime fechaEntrega;
    private String estado;
    private double total;
    private Empleado empleado; // Permanece null hasta que un empleado inicia la preparación.
    /** Productos y cantidades que forman el pedido. */
    private List<DetallePedido> detalles;
    /** Indica si el pedido ya descontó las cantidades correspondientes del inventario. */
    /** Se activa después de descontar el stock para evitar registrarlo o cobrarlo dos veces. */
    private boolean registrado;

    public Pedido(int idPedido) {
        this.idPedido = idPedido;
        this.fechaHora = DateTime.ahora();
        this.estado = PENDIENTE;
        this.codigoQR = "QR-PED-" + idPedido + "-" + fechaHora.toValorComparable();
        this.detalles = new ArrayList<>();
        this.registrado = false;
    }

    /** Agrega una línea antes del registro y recalcula el total del pedido. */
    public void agregarDetalle(DetallePedido detalle) {
        if (detalle == null) {
            throw new IllegalArgumentException("El detalle no puede ser nulo");
        }
        if (registrado) {
            throw new IllegalStateException("No se pueden agregar productos a un pedido ya registrado");
        }
        detalles.add(detalle);
        calcularTotal();
    }

    /** Verifica primero el stock de todas las líneas y luego lo descuenta; false indica que no procede. */
    public boolean registrar() {
        if (registrado || detalles.isEmpty()) return false;
        for (DetallePedido d : detalles) {
            if (!d.getProducto().verificarStock(d.getCantidad())) {
                return false;
            }
        }
        for (DetallePedido d : detalles) {
            d.getProducto().descontarStock(d.getCantidad());
        }
        this.registrado = true;
        this.fechaHora = DateTime.ahora();
        this.estado = PENDIENTE;
        calcularTotal();
        return true;
    }

    /** Suma los subtotales de todas las líneas y guarda el total actualizado. */
    public double calcularTotal() {
        double suma = 0;
        for (DetallePedido d : detalles) {
            suma += d.calcularSubtotal();
        }
        this.total = suma;
        return this.total;
    }

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

    public void marcarListo() {
        if (!EN_PREPARACION.equals(estado)) {
            throw new IllegalStateException("Solo se puede marcar LISTO un pedido EN_PREPARACION (estado actual: " + estado + ")");
        }
        this.estado = LISTO;
        notificarCliente();
    }

    /** Simula el aviso que recibiría el cliente cuando el pedido pasa a LISTO. */
    public void notificarCliente() {
        System.out.println("Notificación: Pedido #" + idPedido + " listo para recoger. QR: " + codigoQR);
    }

    /** Compara el código recibido con el QR asociado al pedido. */
    public boolean validarCodigo(String codigo) {
        return this.codigoQR != null && this.codigoQR.equals(codigo);
    }

    public void entregar() {
        if (!LISTO.equals(estado)) {
            throw new IllegalStateException("Solo se puede entregar un pedido LISTO (estado actual: " + estado + ")");
        }
        this.estado = ENTREGADO;
        this.fechaEntrega = DateTime.ahora();
    }

    public int getIdPedido() { return idPedido; }
    public String getCodigoQR() { return codigoQR; }
    public DateTime getFechaHora() { return fechaHora; }
    public DateTime getFechaEntrega() { return fechaEntrega; }
    public String getEstado() { return estado; }
    public double getTotal() { return total; }
    public Empleado getEmpleado() { return empleado; }
    public List<DetallePedido> getDetalles() { return new ArrayList<>(detalles); }
    public boolean isRegistrado() { return registrado; }

    @Override
    public String toString() {
        return "Pedido #" + idPedido + " [" + estado + "] S/ " + String.format("%.2f", total);
    }
}
