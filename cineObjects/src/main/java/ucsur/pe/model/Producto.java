package ucsur.pe.model;

import ucsur.pe.util.ConversorDinero;
import java.util.HashMap;
import java.util.Map;

/**
 * Por qué existe: es un producto de dulcería con su precio y su stock en cada sede.
 * Puede ser combo (da puntos) u oferta solo para cierto nivel de socio.
 */
public class Producto implements Activable {
    // Valores fijos de los productos (constantes: se usan como Producto.NOMBRE y su valor no cambia).
    public static final int PUNTOS_COMBO = 2; // puntos que da cada combo vendido

    private int idProducto;
    private String nombre;
    private String descripcion;
    private String categoria;
    private long precioCentavos; // en céntimos: S/ 12.50 -> 1250 (ver ConversorDinero)
    private boolean esCombo;
    private int nivelSocioMinimo; // 0 = todos; 1 = cualquier socio; 2 a 4 = ofertas de ese nivel o superior
    /** Unidades disponibles en cada sede; equivale a las filas de inventario_sede de este producto. */
    private Map<Sede, Integer> stockPorSede = new HashMap<>();
    private boolean estado;

    /** Crea un producto abierto para cualquier cliente, sin stock en ninguna sede. */
    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    long precioCentavos, boolean esCombo, boolean estado) {
        this(idProducto, nombre, descripcion, categoria, precioCentavos, esCombo, 0, estado);
    }

    public Producto(int idProducto, String nombre, String descripcion, String categoria,
                    long precioCentavos, boolean esCombo, int nivelSocioMinimo, boolean estado) {
        validarDatos(nombre, descripcion, categoria, precioCentavos, nivelSocioMinimo);
        this.idProducto = idProducto;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.categoria = categoria.trim();
        this.precioCentavos = precioCentavos;
        this.esCombo = esCombo;
        this.nivelSocioMinimo = nivelSocioMinimo;
        this.estado = estado;
    }

    /**
     * El administrador edita el producto (wireframe 20). El nuevo precio vale para pedidos futuros:
     * los ya registrados guardan su precio unitario en DetallePedido.
     */
    public void actualizar(String nombre, String descripcion, String categoria, long precioCentavos,
                           boolean esCombo, int nivelSocioMinimo) {
        validarDatos(nombre, descripcion, categoria, precioCentavos, nivelSocioMinimo);
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.categoria = categoria.trim();
        this.precioCentavos = precioCentavos;
        this.esCombo = esCombo;
        this.nivelSocioMinimo = nivelSocioMinimo;
    }

    private static void validarDatos(String nombre, String descripcion, String categoria,
                                     long precioCentavos, int nivelSocioMinimo) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 100) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 100 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar 255 caracteres");
        }
        if (categoria == null || categoria.trim().isEmpty() || categoria.trim().length() > 40) {
            throw new IllegalArgumentException("La categoría es obligatoria (máx. 40 caracteres)");
        }
        if (precioCentavos < 0) {
            throw new IllegalArgumentException("El precio no puede ser negativo");
        }
        if (nivelSocioMinimo < 0 || nivelSocioMinimo > Cliente.NIVEL_MAXIMO) {
            throw new IllegalArgumentException("El nivel de socio mínimo debe estar entre 0 y " + Cliente.NIVEL_MAXIMO);
        }
    }

    /** Devuelve true si el producto está activo y la sede tiene unidades suficientes para la cantidad solicitada. */
    public boolean verificarStock(Sede sede, int cant) {
        return this.estado && sede != null && cant > 0 && getStock(sede) >= cant;
    }

    /** Rechaza productos inactivos y exige un nivel de socio mínimo cuando el producto es una oferta. */
    public boolean validarElegibilidad(Cliente cliente) {
        if (!this.estado) return false;
        return this.nivelSocioMinimo == 0 || (cliente != null && cliente.cumpleNivelSocio(this.nivelSocioMinimo));
    }

    /** Descuenta unidades de la sede solo cuando la cantidad es positiva y esa sede tiene stock suficiente. */
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
    @Override
    public void activar() {
        this.estado = true;
    }

    /** Retira el producto de la venta. */
    @Override
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
    public long getPrecioCentavos() { return precioCentavos; }
    public boolean isEsCombo() { return esCombo; }
    public int getNivelSocioMinimo() { return nivelSocioMinimo; }
    /** Unidades disponibles en la sede indicada; cero si la sede no tiene registro de este producto. */
    public int getStock(Sede sede) { return stockPorSede.getOrDefault(sede, 0); }
    @Override
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        return nombre + " (" + ConversorDinero.formatear(precioCentavos) + ")";
    }
}