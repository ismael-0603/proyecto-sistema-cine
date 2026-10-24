package ucsur.pe.cineobjects.cineInterfaz;

import ucsur.pe.cineobjects.DateTime;

/** Contrato común para validar y calcular descuentos que pueden activarse o desactivarse. */

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
