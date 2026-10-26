package ucsur.pe.cineobjects;

/**
 * Representa una línea de pedido y conserva el precio unitario vigente al crearla.
 * Requiere un producto activo, cantidad positiva y personalización de hasta 255 caracteres.
 * El stock no se valida aquí porque depende de la sede; Pedido lo comprueba al registrarse.
 */
public class DetallePedido {
    private int idDetalle;
    private int cantidad;
    /** Precio guardado al crear la línea, para mantener el importe aunque cambie el precio del producto. */
    private double precioUnitario;
    private String personalizacion;
    private double subtotal; // cantidad por precio unitario
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
        this.precioUnitario = producto.getPrecio(); // precio fijado al momento de la compra
        this.personalizacion = personalizacion;
        calcularSubtotal();
    }

    /** Calcula y guarda el subtotal de la línea con la cantidad actual. */
    public double calcularSubtotal() {
        this.subtotal = cantidad * precioUnitario;
        return this.subtotal;
    }

    /** Cambia la cantidad si es positiva; el stock de la sede se comprueba al registrar el pedido. */
    public void modificarCantidad(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        this.cantidad = cant;
        calcularSubtotal();
    }

    public int getIdDetalle() { return idDetalle; }
    public int getCantidad() { return cantidad; }
    public double getPrecioUnitario() { return precioUnitario; }
    public String getPersonalizacion() { return personalizacion; }
    public double getSubtotal() { return subtotal; }
    public Producto getProducto() { return producto; }

    @Override
    public String toString() {
        return cantidad + " x " + producto.getNombre() + " = S/ " + String.format("%.2f", subtotal);
    }
}
