package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class DetallePedido {
    private int idDetalle;
    private int cantidad;
    private double precioUnitario;
    private String personalizacion;
    private double subtotal;
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
        if (!producto.verificarStock(cantidad)) {
            throw new IllegalStateException("Stock insuficiente o producto inactivo: " + producto.getNombre());
        }
        this.idDetalle = idDetalle;
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = producto.getPrecio(); // precio fijado al momento de la compra
        this.personalizacion = personalizacion;
        calcularSubtotal(this.cantidad);
    }

    // Firma del diagrama: + calcularSubtotal(cant: int): double
    public double calcularSubtotal(int cant) {
        this.subtotal = cant * this.precioUnitario;
        return this.subtotal;
    }

    public double calcularSubtotal() {
        return calcularSubtotal(this.cantidad);
    }

    public void modificarCantidad(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (!producto.verificarStock(cant)) {
            throw new IllegalStateException("Stock insuficiente para " + producto.getNombre());
        }
        this.cantidad = cant;
        calcularSubtotal(this.cantidad);
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
