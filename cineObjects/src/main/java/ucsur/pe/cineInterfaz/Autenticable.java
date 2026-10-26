package ucsur.pe.cineInterfaz;

/**
 * Por qué existe: para que cualquier cuenta (cliente o empleado) inicie sesión y actualice
 * sus datos de la misma forma, sin que el resto del sistema necesite saber cuál es.
 */
public interface Autenticable {
    /** Valida las credenciales y devuelve si se pudo iniciar sesión. */
    boolean iniciarSesion(String correo, String contrasena);
    /** Actualiza correo y teléfono si los nuevos datos pasan sus validaciones. */
    boolean modificarDatos(String telefono, String correo);
}
