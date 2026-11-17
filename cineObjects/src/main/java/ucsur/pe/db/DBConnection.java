package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Por qué existe: maneja la única conexión JDBC de la aplicación (Singleton, como en MVCone).
 * Lee los datos de db.properties y abre la conexión a DB_Cine.
 *
 * A diferencia de MVCone, NO ejecuta el script de la base: BD_Cine.sql usa DELIMITER $$ en sus
 * triggers y partirlo por ";" lo rompería. La base se crea una vez desde MySQL Workbench.
 */
public class DBConnection {

    private static final String RUTA_PROPIEDADES = "/ucsur/pe/db/db.properties";

    /**
     * Opciones de la URL:
     * - connectionTimeZone=-05:00 y forceConnectionTimeZoneToSession: la sesión usa la hora de Perú,
     *   igual que DateTime y el evento ev_expirar_compras (así NOW() coincide con DateTime.ahora()).
     * - preserveInstants=false: las fechas viajan tal cual se leen ("2026-11-17 10:30:00"),
     *   sin correrse aunque la zona horaria de la PC sea otra.
     */
    private static final String OPCIONES = "?useUnicode=true&characterEncoding=UTF-8"
            + "&connectionTimeZone=-05:00&forceConnectionTimeZoneToSession=true&preserveInstants=false";

    private static DBConnection instancia;
    private final Connection conexion;

    private DBConnection() {
        this.conexion = abrir(leerPropiedades());
    }

    /** Devuelve la instancia compartida; la crea (y se conecta) la primera vez. */
    public static DBConnection getInstance() {
        if (instancia == null) {
            instancia = new DBConnection();
        }
        return instancia;
    }

    /** La conexión activa, para pasársela a los DAO. */
    public Connection getConexion() {
        return conexion;
    }

    /** Cierra la conexión; se llama al cerrar la aplicación. Luego getInstance() volvería a conectar. */
    public void cerrar() {
        try {
            if (!conexion.isClosed()) {
                conexion.close();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Error al cerrar la conexión.", e);
        } finally {
            instancia = null;
        }
    }

    /**
     * Abre una conexión con los datos indicados. Es pública para poder probar en la demo
     * qué pasa con una contraseña incorrecta sin tocar la instancia compartida.
     */
    public static Connection abrir(Properties props) {
        String url = props.getProperty("db.url") + props.getProperty("db.name") + OPCIONES;
        try {
            return DriverManager.getConnection(url, props.getProperty("db.user"), props.getProperty("db.password"));
        } catch (SQLException e) {
            throw new RepositoryException(
                    "No se pudo conectar a la base de datos. Verifique db.properties.", e);
        }
    }

    /** Lee db.properties del classpath (src/main/resources/ucsur/pe/db). */
    public static Properties leerPropiedades() {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getResourceAsStream(RUTA_PROPIEDADES)) {
            if (in == null) {
                throw new RepositoryException(
                        "No se encontró db.properties. Copie db.properties.example como db.properties.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RepositoryException("No se pudo leer db.properties.", e);
        }
        for (String clave : new String[] {"db.url", "db.name", "db.user", "db.password"}) {
            if (props.getProperty(clave) == null) {
                throw new RepositoryException("Falta la clave " + clave + " en db.properties.");
            }
        }
        return props;
    }
}
