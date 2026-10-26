package ucsur.pe.cineobjects;

import java.util.HashMap;
import java.util.Map;
import ucsur.pe.cineInterfaz.Inventariable;

/**
 * Por qué existe: es un producto de dulcería con su precio y su stock en cada sede.
 * Puede ser combo (da puntos) u oferta solo para cierto nivel de socio.
 */
public class Producto implements Inventariable {
    // Valores fijos de los productos (constantes: se usan como Producto.NOMBRE y su valor no cambia).
    public static final int PUNTOS_COMBO = 2; // puntos que da cada combo vendido
    public static final int NIVEL_SOCIO_MAXIMO = 4; // nivel de socio más alto (los niveles van de 1 a 4)

    private int idProducto;
    private String nombre;
    private String descripcion;
    private String categoria;
    private double precio;
    private boolean esCombo;
    private int nivelSocioMinimo; // 0 = todos; 1 = cualquier socio; 2 a 4 = ofertas de ese nivel o superior
    /** Unidades disponibles en cada sede; equivale a las filas de inventario_sede de este producto. */
    private Map<Sede, Integer> stockPorSede = new HashMap<>();
    private boolean estado;

    /** Crea un producto abierto para cualquier cliente, sin stock en ninguna sede. */
    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, boolean esCombo, boolean estado) {
        this(idProducto, nombre, descripcion, categoria, precio, esCombo, 0, estado);
    }

    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    double precio, boolean esCombo, int nivelSocioMinimo, boolean estado) {
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
        this.idProducto = idProducto;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.categoria = categoria.trim();
        this.precio = precio;
        this.esCombo = esCombo;
        this.nivelSocioMinimo = nivelSocioMinimo;
        this.estado = estado;
    }

    /** Devuelve true si el producto está activo y la sede tiene unidades suficientes para la cantidad solicitada. */
    @Override
    public boolean verificarStock(Sede sede, int cant) {
        return this.estado && sede != null && cant > 0 && getStock(sede) >= cant;
    }

    /** Rechaza productos inactivos y exige un nivel de socio mínimo cuando el producto es una oferta. */
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        if (this.nivelSocioMinimo > 0) {
            return cliente != null && cliente.esSocio() && cliente.getNivelSocio() >= this.nivelSocioMinimo;
        }
        return true;
    }

    /** Descuenta unidades de la sede solo cuando la cantidad es positiva y esa sede tiene stock suficiente. */
    @Override
    public void descontarStock(Sede sede, int cant) {
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (!verificarStock(sede, cant)) {
            throw new IllegalStateException("Stock insuficiente o producto inactivo: " + nombre);
        }
        stockPorSede.put(sede, getStock(sede) - cant);
    }

    /** Aumenta el stock de la sede; rechaza una sede nula y cantidades iguales o menores que cero. */
    @Override
    public void reponerStock(Sede sede, int cant) {
        if (sede == null) {
            throw new IllegalArgumentException("La sede es obligatoria");
        }
        if (cant <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a 0");
        }
        stockPorSede.put(sede, getStock(sede) + cant);
    }

    /** Habilita el producto para la venta. */
    public void activar() {
        this.estado = true;
    }

    /** Retira el producto de la venta. */
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
    /** Unidades disponibles en la sede indicada; cero si la sede no tiene registro de este producto. */
    public int getStock(Sede sede) { return stockPorSede.getOrDefault(sede, 0); }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (S/ " + String.format("%.2f", precio) + ")";
    }
}