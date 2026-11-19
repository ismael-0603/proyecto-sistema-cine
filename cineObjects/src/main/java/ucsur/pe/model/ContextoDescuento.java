package ucsur.pe.model;

/**
 * Por qué existe: son los únicos datos de una compra que un descuento necesita para saber si aplica.
 * Así la interfaz Descuento no depende de la clase Compra entera. Es inmutable (record).
 *
 * @param fecha fecha y hora de la compra (para la vigencia y el día de la semana)
 * @param nivelSocio nivel de socio del cliente: 0 si es invitado o no es socio, 1 a 4 si lo es
 */
public record ContextoDescuento(DateTime fecha, int nivelSocio) {

    /** Indica si el cliente es socio (cualquier nivel). */
    public boolean clienteEsSocio() {
        return nivelSocio > 0;
    }

    /** Un nivel mínimo de 0 deja pasar a todos; si no, el cliente debe tener ese nivel o uno mayor. */
    public boolean cumpleNivel(int nivelMinimo) {
        return nivelMinimo <= 0 || nivelSocio >= nivelMinimo;
    }
}
