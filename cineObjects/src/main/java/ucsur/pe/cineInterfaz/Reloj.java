package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.DateTime;

/**
 * Por qué existe: la hora actual es algo externo al modelo, como la pasarela de pagos.
 * Las reglas que dependen de la hora (plazo de 4 minutos, bloqueo de butacas, estado de una función)
 * la piden a esta interfaz y no a DateTime.ahora(). Así, en pruebas se usa un reloj fijo
 * ("son las 20:05") y se comprueba el plazo sin esperar 4 minutos reales.
 */
public interface Reloj {
    /** Fecha y hora actual según este reloj. */
    DateTime ahora();
}
