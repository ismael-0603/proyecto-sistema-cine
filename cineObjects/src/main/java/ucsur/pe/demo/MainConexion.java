package ucsur.pe.demo;

import ucsur.pe.db.DBConnection;
import ucsur.pe.db.Fechas;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.DateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Por qué existe: prueba por consola la conexión a DB_Cine (Fase 1 de la migración a MySQL).
 * Cada comprobación imprime OK o FALLA. Requiere MySQL encendido y la base creada con
 * BD_Cine.sql y BD_Cine_datos.sql.
 */
public class MainConexion {

    private static int fallas = 0;

    public static void main(String[] args) {
        System.out.println("=== CONEXIÓN A DB_Cine ===");

        probarFechasSinBD();
        probarContrasenaIncorrecta();

        DBConnection db;
        try {
            db = DBConnection.getInstance();
        } catch (RepositoryException e) {
            comprobar("conectar con db.properties", false);
            System.out.println("   " + e.getMessage());
            System.out.println("   Causa: " + (e.getCause() != null ? e.getCause().getMessage() : "-"));
            System.out.println("   ¿Está encendido el servicio MySQL80 y es correcta la contraseña?");
            terminar();
            return;
        }
        comprobar("conectar con db.properties", true);

        try {
            Connection cn = db.getConexion();
            comprobar("la base activa es DB_Cine", "DB_Cine".equalsIgnoreCase(textoDe(cn, "SELECT DATABASE()")));
            mostrarConteo(cn, "sede");
            mostrarConteo(cn, "sala");
            mostrarConteo(cn, "butaca");
            mostrarConteo(cn, "pelicula");
            mostrarConteo(cn, "funcion");
            mostrarConteo(cn, "producto");
            mostrarConteo(cn, "cliente");
            probarHoraDeLaSesion(cn);
            probarFechaIdaYVuelta(cn);
            probarLeerUnaFuncion(cn);
        } catch (SQLException e) {
            comprobar("consultas de prueba", false);
            System.out.println("   " + e.getMessage());
        } finally {
            db.cerrar();
        }
        terminar();
    }

    /** Fechas convierte DateTime a JDBC y de vuelta sin perder nada (no necesita la BD). */
    private static void probarFechasSinBD() {
        DateTime fechaHora = new DateTime(17, 11, 2026, 21, 5, 9);
        DateTime fecha = new DateTime(29, 2, 2028);
        DateTime hora = new DateTime(18, 45);
        comprobar("Fechas: DATETIME ida y vuelta", fechaHora.equals(Fechas.desdeTimestamp(Fechas.aTimestamp(fechaHora))));
        comprobar("Fechas: DATE ida y vuelta (29/02 bisiesto)", fecha.equals(Fechas.desdeDate(Fechas.aDate(fecha))));
        comprobar("Fechas: TIME ida y vuelta", hora.equals(Fechas.desdeTime(Fechas.aTime(hora))));
        comprobar("Fechas: null devuelve null", Fechas.aTimestamp(null) == null && Fechas.desdeDate(null) == null);
    }

    /** Con una contraseña mala debe salir el mensaje en español, no una traza de MySQL. */
    private static void probarContrasenaIncorrecta() {
        Properties props;
        try {
            props = DBConnection.leerPropiedades();
        } catch (RepositoryException e) {
            comprobar("leer db.properties", false);
            System.out.println("   " + e.getMessage());
            return;
        }
        props.setProperty("db.password", props.getProperty("db.password") + "_incorrecta");
        try {
            DBConnection.abrir(props).close();
            comprobar("contraseña incorrecta es rechazada", false);
        } catch (RepositoryException e) {
            comprobar("contraseña incorrecta -> \"" + e.getMessage() + "\"",
                    e.getMessage().startsWith("No se pudo conectar"));
        } catch (SQLException e) {
            comprobar("contraseña incorrecta es rechazada", false);
        }
    }

    /** NOW() de la sesión debe coincidir con DateTime.ahora() (los dos en hora de Perú). */
    private static void probarHoraDeLaSesion(Connection cn) throws SQLException {
        DateTime antes = DateTime.ahora();
        DateTime enBD;
        try (PreparedStatement ps = cn.prepareStatement("SELECT NOW()");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            enBD = Fechas.desdeTimestamp(rs.getTimestamp(1));
        }
        DateTime despues = DateTime.ahora();
        boolean coincide = !enBD.esDespuesDe(despues.sumarMinutos(1)) && !antes.esDespuesDe(enBD.sumarMinutos(1));
        comprobar("NOW() de MySQL (" + enBD + ") = DateTime.ahora() (" + antes + ")", coincide);
    }

    /** Una fecha enviada con PreparedStatement vuelve igual (no se corre por la zona horaria). */
    private static void probarFechaIdaYVuelta(Connection cn) throws SQLException {
        DateTime original = new DateTime(17, 11, 2026, 23, 59, 30);
        try (PreparedStatement ps = cn.prepareStatement("SELECT CAST(? AS DATETIME)")) {
            ps.setTimestamp(1, Fechas.aTimestamp(original));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                DateTime leida = Fechas.desdeTimestamp(rs.getTimestamp(1));
                comprobar("DATETIME por la BD ida y vuelta (" + leida + ")", original.equals(leida));
            }
        }
    }

    /** Lee la primera función para ver que DATE y TIME llegan bien como DateTime. */
    private static void probarLeerUnaFuncion(Connection cn) throws SQLException {
        String sql = """
                SELECT f.id_funcion, f.fecha, f.hora_inicio, p.titulo
                FROM funcion f
                JOIN pelicula p ON p.id_pelicula = f.id_pelicula
                ORDER BY f.id_funcion
                LIMIT 1
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                comprobar("leer una función (la tabla está vacía)", false);
                return;
            }
            DateTime fecha = Fechas.desdeDate(rs.getDate("fecha"));
            DateTime hora = Fechas.desdeTime(rs.getTime("hora_inicio"));
            comprobar("leer función " + rs.getInt("id_funcion") + ": " + rs.getString("titulo")
                    + " el " + fecha.soloFecha() + " a las " + hora.soloHora(), true);
        }
    }

    // ---------- apoyo ----------

    private static void mostrarConteo(Connection cn, String tabla) throws SQLException {
        // el nombre de tabla viene de este mismo archivo, no del usuario
        String total = textoDe(cn, "SELECT COUNT(*) FROM " + tabla);
        comprobar("tabla " + tabla + ": " + total + " filas", true);
    }

    private static String textoDe(Connection cn, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void comprobar(String descripcion, boolean ok) {
        if (!ok) fallas++;
        System.out.println((ok ? "OK    " : "FALLA ") + descripcion);
    }

    private static void terminar() {
        System.out.println(fallas == 0 ? "\nTodo OK." : "\n" + fallas + " comprobación(es) con FALLA.");
    }
}
