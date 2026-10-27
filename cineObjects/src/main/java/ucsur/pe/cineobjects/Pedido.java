package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.Inventariable;
import ucsur.pe.cineInterfaz.Registrable;
import ucsur.pe.cineInterfaz.Totalizable;
import java.util.ArrayList;
import java.util.List;
/**
 * Por qué existe: es el pedido de dulcería. Descuenta el stock de la sede al registrarse y avanza
 * en orden: PENDIENTE, EN_PREPARACION, LISTO y ENTREGADO.
 */
public class Pedido implements Totalizable, Registrable{
    // Estados de un pedido, en el orden en que avanzan (constantes: se usan como Pedido.NOMBRE y su valor no cambia).
    public static final String PENDIENTE = "PENDIENTE"; // registrado, esperando a un empleado
    public static final String EN_PREPARACION = "EN_PREPARACION"; // un empleado lo está preparando
    public static final String LISTO = "LISTO"; // listo para recoger; se avisa al cliente
    public static final String ENTREGADO = "ENTREGADO"; // el cliente ya lo recogió

    private int idPedido;
    private String codigoQR; // null hasta que el pedido se registra, cuando ya existe su id real.
    private DateTime fechaHora;
    private DateTime fechaEntrega;
    private String estado;
    private double total; // suma de los subtotales de las líneas
    private Empleado empleado; // Permanece null hasta que un empleado inicia la preparación.
    /** Sede de cuyo inventario se descuenta el stock; se fija al registrar el pedido. */
    private Sede sede;
    /** Productos y cantidades que forman el pedido. */
    private List<DetallePedido> detalles;
    /** Se activa al descontar el stock, para no registrar ni cobrar el pedido dos veces. */
    private boolean registrado;

    public Pedido(int idPedido) {
        this.idPedido = idPedido;
        this.fechaHora = DateTime.ahora();
        this.estado = PENDIENTE;
        this.detalles = new ArrayList<>();
        this.registrado = false;
    }

    /** Asigna el id real generado por la base tras el INSERT; si el QR ya existe, lo rehace con ese id. */
    public void asignarId(int idPedido) {
        if (this.idPedido > 0) {
            throw new IllegalStateException("El pedido ya tiene id asignado");
        }
        if (idPedido <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser mayor a 0");
        }
        this.idPedido = idPedido;
        if (codigoQR != null) {
            this.codigoQR = generarCodigoQR();
        }
    }

    /** Forma el QR con el id del pedido y la fecha de registro; se llama una vez que el pedido ya tiene su id. */
    private String generarCodigoQR() {
        return "QR-PED-" + idPedido + "-" + fechaHora.toValorComparable();
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

    /** Fija la sede del pedido; no se puede cambiar una vez registrado. */
    public void asignarSede(Sede sede) {
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        if (registrado) {
            throw new IllegalStateException("No se puede cambiar la sede de un pedido ya registrado");
        }
        this.sede = sede;
    }

    /** Registra el pedido descontando el stock del inventario de la sede indicada. */
    public boolean registrar(Sede sede) {
        if (sede == null || registrado) return false;
        this.sede = sede;
        return registrar();
    }

    /**
     * Verifica primero el stock de todas las líneas en la sede del pedido y luego lo descuenta.
     * Devuelve false si el pedido no tiene sede o si algo no procede.
     * Para el stock, el pedido ve cada producto solo como Inventariable: no necesita saber nada más de él.
     */
    @Override
    public boolean registrar() {
        if (registrado || detalles.isEmpty() || sede == null) return false;
        for (DetallePedido d : detalles) {
            Inventariable articulo = d.getProducto();
            if (!articulo.verificarStock(sede, d.getCantidad())) {
                return false;
            }
        }
        for (DetallePedido d : detalles) {
            Inventariable articulo = d.getProducto();
            articulo.descontarStock(sede, d.getCantidad());
        }
        this.registrado = true;
        this.fechaHora = DateTime.ahora();
        this.estado = PENDIENTE;
        this.codigoQR = generarCodigoQR();
        calcularTotal();
        return true;
    }

    /** Suma los subtotales de todas las líneas y guarda el total actualizado. */
    @Override
    public double calcularTotal() {
        double suma = 0;
        for (DetallePedido d : detalles) {
            suma += d.calcularSubtotal();
        }
        this.total = suma;
        return this.total;
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

    /** Pasa de EN_PREPARACION a LISTO y avisa al cliente. */
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

    /** Pasa de LISTO a ENTREGADO y guarda la fecha de entrega. */
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
    public Sede getSede() { return sede; }
    public List<DetallePedido> getDetalles() { return new ArrayList<>(detalles); }
    public boolean isRegistrado() { return registrado; }

    @Override
    public String toString() {
        return "Pedido #" + idPedido + " [" + estado + "] S/ " + String.format("%.2f", total);
    }
}
