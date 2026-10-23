package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Producto {
    public static final int PUNTOS_COMBO = 2;
    
    private int idProducto;
    private String nombre;
    private String descripcion;
    private String categoria;
    private double precio;
    private boolean esCombo;
    private int stock; // en la BD el stock vive en inventario_sede (por sede); aquí se simplifica
    private boolean estado;

    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, boolean esCombo, int stock, boolean estado) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 100 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar 255 caracteres");
        }
        if (categoria == null || categoria.trim().isEmpty() || categoria.trim().length() > 40) {
            throw new IllegalArgumentException("La categoría es obligatoria (máx. 40 caracteres)");
        }
        if (precio < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.idProducto = idProducto;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.categoria = categoria.trim();
        this.precio = precio;
        this.esCombo = esCombo;
        this.stock = stock;
        this.estado = estado;
    }

    // true si el producto está activo y alcanza el stock
    public boolean verificarStock(int cant) {
        return this.estado && cant > 0 && this.stock >= cant;
    }

    public void descontarStock(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (!verificarStock(cant)) {
            throw new IllegalStateException("Stock insuficiente o producto inactivo: " + nombre);
        }
        this.stock -= cant;
    }

    public void reponerStock(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a 0");
        }
        this.stock += cant;
    }

    public void activar() { 
        this.estado = true; 
    }
    
    public void desactivar() { 
        this.estado = false; 
    }
    
    // Otorga puntos únicamente si el producto está activo y es un combo
    public int obtenerPuntos() {
        return (this.estado && this.esCombo) ? PUNTOS_COMBO : 0;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getPrecio() { return precio; }
    public boolean isEsCombo() { return esCombo; }
    public int getStock() { return stock; }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (S/ " + String.format("%.2f", precio) + ")";
    }
}