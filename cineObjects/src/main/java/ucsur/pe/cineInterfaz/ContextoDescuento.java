package ucsur.pe.cineInterfaz;

import ucsur.pe.cineobjects.DateTime;

/**
 * Por qué existe: son los únicos datos de una compra que un descuento necesita para saber si aplica.
 * Así la interfaz Descuento no depende de la clase Compra entera. Es inmutable (record).
 *
 * @param fecha fecha y hora de la compra (para la vigencia y el día de la semana)
 * @param clienteEsSocio si el cliente es socio (para las promociones solo para socios)
 */
public record ContextoDescuento(DateTime fecha, boolean clienteEsSocio) {
}
