package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.DateTime;

/**
 * Por qué existe: para tratar igual a cupones y promociones: todos dicen si están vigentes,
 * cuánto descuentan, si aplican a una compra y se pueden activar o desactivar.
 * Compra trabaja solo con esta interfaz; no sabe si tiene un cupón, una promoción u otro descuento.
 */
public interface Descuento {
    /** Comprueba si el descuento puede usarse en la fecha indicada. */
    boolean validarVigencia(DateTime fecha);
    /** Calcula cuánto se descuenta del importe recibido. */
    double calcularDescuento(double total);
    /** Indica si se puede usar en esa compra (fechas, días, socio, usos...: cada descuento pone sus reglas). */
    boolean esAplicable(Compra compra);
    /**
     * Orden al combinar descuentos: el menor se aplica primero sobre el subtotal
     * y el siguiente sobre lo que queda.
     */
    int getOrdenAplicacion();
    /** Lo que pasa cuando una compra confirmada usa este descuento (por ejemplo, un cupón suma un uso). */
    void registrarUso();
    /** Habilita el descuento. */
    void activar();
    /** Deshabilita el descuento. */
    void desactivar();
}
