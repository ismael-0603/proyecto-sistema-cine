package ucsur.pe.cineobjects;

/**
 * Representa un producto de dulcería y administra su estado, stock y puntos si es un combo.
 * Valida nombre, categoría, límites de descripción, precio y stock no negativos.
 * Un producto puede ser una oferta disponible solo desde cierto nivel de socio.
 */
public class Producto {
    public static final int PUNTOS_COMBO = 2;
    public static final int NIVEL_SOCIO_MAXIMO = 4;

    private int idProducto;
    private String nombre;
    private String descripcion;
    private String categoria;
    private double precio;
    private boolean esCombo;
    private int nivelSocioMinimo; // 0 = todos; 1 = cualquier socio; 2 a 4 = ofertas de ese nivel o superior
    private int stock; // Stock simplificado del modelo; en la base de datos se registra por sede.
    private boolean estado;

    /** Crea un producto abierto para cualquier cliente. */
    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, boolean esCombo, int stock, boolean estado) {
        this(idProducto, nombre, descripcion, categoria, precio, esCombo, 0, stock, estado);
    }

    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, boolean esCombo, int nivelSocioMinimo, int stock, boolean estado) {
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
        if (nivelSocioMinimo < 0 || nivelSocioMinimo > NIVEL_SOCIO_MAXIMO) {
            throw new IllegalArgumentException("El nivel de socio mínimo debe estar entre 0 y " + NIVEL_SOCIO_MAXIMO);
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
        this.nivelSocioMinimo = nivelSocioMinimo;
        this.stock = stock;
        this.estado = estado;
    }

    /** Devuelve true si el producto está activo y hay unidades suficientes para la cantidad solicitada. */
    public boolean verificarStock(int cant) {
        return this.estado && cant > 0 && this.stock >= cant;
    }

    /** Rechaza productos inactivos y exige un nivel de socio mínimo cuando el producto es una oferta. */
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        if (this.nivelSocioMinimo > 0) {
            return cliente != null && cliente.esSocio() && cliente.getNivelSocio() >= this.nivelSocioMinimo;
        }
        return true;
    }

    /** Descuenta unidades solo cuando la cantidad es positiva y el producto tiene stock suficiente. */
    public void descontarStock(int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (!verificarStock(cant)) {
            throw new IllegalStateException("Stock insuficiente o producto inactivo: " + nombre);
        }
        this.stock -= cant;
    }

    /** Aumenta el stock; rechaza cantidades iguales o menores que cero. */
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

    /** Devuelve puntos solo para combos activos; los demás productos generan cero puntos. */
    public int obtenerPuntos() {
        return (this.estado && this.esCombo) ? PUNTOS_COMBO : 0;
    }

    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getPrecio() { return precio; }
    public boolean isEsCombo() { return esCombo; }
    public int getNivelSocioMinimo() { return nivelSocioMinimo; }
    public boolean isRequiereSocio() { return nivelSocioMinimo > 0; }
    public int getStock() { return stock; }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (S/ " + String.format("%.2f", precio) + ")";
    }
}