package ucsur.pe.cineobjects.cineInterfaz;

/**
 *
 * @author user
 */
public interface Autenticable {
    boolean iniciarSesion(String correo, String contrasena);
    boolean modificarDatos(String telefono, String correo);
}
