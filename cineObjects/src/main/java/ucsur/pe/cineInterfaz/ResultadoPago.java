package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: es la respuesta de la pasarela de pagos. Es inmutable (record):
 * si se aprobó trae la referencia de la operación; si se rechazó, el motivo.
 */
public record ResultadoPago(boolean aprobado, String referencia, String motivo) {

    /** Respuesta de un cobro aprobado. */
    public static ResultadoPago aprobado(String referencia) {
        return new ResultadoPago(true, referencia, null);
    }

    /** Respuesta de un cobro rechazado. */
    public static ResultadoPago rechazado(String motivo) {
        return new ResultadoPago(false, null, motivo);
    }
}
