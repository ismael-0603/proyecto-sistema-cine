package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.DateTime;

/**
 * Por qué existe: para tratar igual a cupones y promociones: todos dicen si están vigentes,
 * cuánto descuentan y se pueden activar o desactivar.
 */
public interface Descuento {
    /** Comprueba si el descuento puede usarse en la fecha indicada. */
    boolean validarVigencia(DateTime fecha);
    /** Calcula cuánto se descuenta del importe recibido. */
    double calcularDescuento(double total);
    /** Habilita el descuento. */
    void activar();
    /** Deshabilita el descuento. */
    void desactivar();
}
