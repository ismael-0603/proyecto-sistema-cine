package ucsur.pe.exception;

/**
 * Por qué existe: error propio cuando se rompe una regla del negocio que revisa un servicio
 * (correo ya registrado, cupón inexistente, compra ya calificada...).
 * Hereda de IllegalStateException, igual que los errores de reglas del modelo, así el controller
 * los atrapa a todos de la misma forma y muestra el mensaje.
 */
public class ReglaNegocioException extends IllegalStateException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
