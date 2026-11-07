package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: agrupa lo que el administrador hace con cualquier elemento del catálogo que se
 * puede habilitar o retirar de la venta (descuentos, productos y tipos de entrada), sin que la venta
 * tenga que conocer estos métodos. En la BD corresponde a la columna estado (BOOLEAN) de esas tablas.
 */
public interface Activable {
    /** Habilita el elemento para nuevas ventas. */
    void activar();
    /** Retira el elemento de la venta; lo ya vendido no cambia. */
    void desactivar();
    /** Indica si el elemento está habilitado. */
    boolean isEstado();
}
