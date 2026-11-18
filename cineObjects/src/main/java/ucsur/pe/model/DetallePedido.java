package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;

/**
 * Por qué existe: es una línea del pedido (producto x cantidad). Guarda el precio del momento
 * para que el importe no cambie si después sube el precio del producto.
 */
public class DetallePedido {
    private int idDetalle;
    private int cantidad;
    /** Precio guardado al crear la línea, para mantener el importe aunque cambie el precio del producto. */
    private long precioUnitarioCentavos;
    private String personalizacion;
    private Producto producto;

    public DetallePedido(int idDetalle, Producto producto, int cantidad, String personalizacion) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (personalizacion != null && personalizacion.length() > 255) {
            throw new IllegalArgumentException("La personalización no puede superar 255 caracteres");
        }
        if (!producto.isEstado()) {
            throw new IllegalStateException("Producto inactivo: " + producto.getNombre());
        }
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitarioCentavos = producto.getPrecioCentavos(); // precio fijado al momento de la compra
        this.personalizacion = personalizacion;
    }

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye una línea ya guardada con el precio
     * de ese momento (detalle_pedido.precio_unitario). No exige que el producto siga activo:
     * el pedido se hizo cuando sí lo estaba.
     */
    public static DetallePedido restaurar(int idDetalle, Producto producto, int cantidad,
                                          long precioUnitarioCentavos, String personalizacion) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (precioUnitarioCentavos < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        return new DetallePedido(idDetalle, producto, cantidad, personalizacion, precioUnitarioCentavos);
    }

    /** Constructor de restaurar: asigna sin las validaciones de una línea nueva. */
    private DetallePedido(int idDetalle, Producto producto, int cantidad, String personalizacion,
                          long precioUnitarioCentavos) {
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitarioCentavos = precioUnitarioCentavos;
        this.personalizacion = personalizacion;
    }

    /** Subtotal con la cantidad actual y el precio pactado; no se guarda: siempre sale de esos dos datos. */
    public long calcularSubtotalCentavos() {
        return cantidad * precioUnitarioCentavos;
    }

    /**
     * Cambia la cantidad si es positiva; el stock de la sede se comprueba al registrar el pedido.
     * No es pública: se cambia con Pedido.modificarCantidad, que impide tocar un pedido ya registrado.
     */
    void modificarCantidad(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.cantidad = cant;
    }

    public int getCantidad() { return cantidad; }
    public long getPrecioUnitarioCentavos() { return precioUnitarioCentavos; }
    public String getPersonalizacion() { return personalizacion; }
    public long getSubtotalCentavos() { return calcularSubtotalCentavos(); }
    public Producto getProducto() { return producto; }

    @Override
    public String toString() {
        return cantidad + " x " + producto.getNombre() + " = " + ConversorDinero.formatear(calcularSubtotalCentavos());
    }
}
