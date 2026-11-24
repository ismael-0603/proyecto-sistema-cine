package ucsur.pe.service;

/**
 * Por qué existe: dice a la vista qué hacer cuando un empleado va a comprar entradas.
 * VINCULADA: entra directo con su cuenta personal. POR_CONFIRMAR: ya existe un cliente con su DNI y debe
 * escribir la contraseña de esa cuenta (se muestra su correo enmascarado). NUEVA: debe crear su cuenta personal.
 */
public record CuentaPersonal(Estado estado, String correoEnmascarado) {

    public enum Estado { VINCULADA, POR_CONFIRMAR, NUEVA }

    /** «juan.perez@gmail.com» -> «ju***@gmail.com»: no muestra el correo completo de otra persona. */
    static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba < 0) return "***";
        String usuario = correo.substring(0, arroba);
        return usuario.substring(0, Math.min(2, usuario.length())) + "***" + correo.substring(arroba);
    }
}
