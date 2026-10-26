package ucsur.pe.cineInterfaz;

/** Define las operaciones de inicio de sesión y actualización de datos de una cuenta. */
public interface Autenticable {
    /** Valida las credenciales y devuelve si se pudo iniciar sesión. */
    boolean iniciarSesion(String correo, String contrasena);
    /** Actualiza correo y teléfono si los nuevos datos pasan sus validaciones. */
    boolean modificarDatos(String telefono, String correo);
}
