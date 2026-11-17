package ucsur.pe.db;

import ucsur.pe.model.DateTime;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Por qué existe: traduce entre nuestra clase DateTime y los tipos de JDBC (DATE, TIME, DATETIME).
 * Es el único lugar que conoce java.sql.Date/Time/Timestamp: el modelo sigue usando solo DateTime.
 * Convierte por texto ("2026-11-17 10:30:00") para que la hora no se corra por zonas horarias.
 * Todos los métodos aceptan null y devuelven null (columnas opcionales como fecha_entrega).
 */
public final class Fechas {

    private Fechas() { }

    // ---------- DateTime -> JDBC (para INSERT y UPDATE) ----------

    /** Solo la fecha, para columnas DATE (fecha de función, fecha_nac, fecha_inicio...). */
    public static Date aDate(DateTime fecha) {
        if (fecha == null) return null;
        return Date.valueOf(textoFecha(fecha));
    }

    /** Solo la hora, para columnas TIME (hora_inicio de la función). */
    public static Time aTime(DateTime hora) {
        if (hora == null) return null;
        return Time.valueOf(textoHora(hora));
    }

    /** Fecha y hora con segundos, para columnas DATETIME (fecha_hora, expira_compra...). */
    public static Timestamp aTimestamp(DateTime fechaHora) {
        if (fechaHora == null) return null;
        return Timestamp.valueOf(textoFecha(fechaHora) + " " + textoHora(fechaHora));
    }

    // ---------- JDBC -> DateTime (al leer un ResultSet) ----------

    /** DATE "yyyy-MM-dd" -> DateTime(dia, mes, anio). */
    public static DateTime desdeDate(Date fecha) {
        if (fecha == null) return null;
        String[] p = fecha.toString().split("-");
        return new DateTime(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
    }

    /** TIME "HH:mm:ss" -> DateTime(hora, minuto), como guarda Funcion su hora de inicio. */
    public static DateTime desdeTime(Time hora) {
        if (hora == null) return null;
        String[] p = hora.toString().split(":");
        return new DateTime(Integer.parseInt(p[0]), Integer.parseInt(p[1]));
    }

    /** DATETIME "yyyy-MM-dd HH:mm:ss[.f]" -> DateTime con segundos (se descartan las fracciones). */
    public static DateTime desdeTimestamp(Timestamp fechaHora) {
        if (fechaHora == null) return null;
        String[] partes = fechaHora.toString().split(" ");
        String[] f = partes[0].split("-");
        String[] h = partes[1].split("\\.")[0].split(":");
        return new DateTime(Integer.parseInt(f[2]), Integer.parseInt(f[1]), Integer.parseInt(f[0]),
                Integer.parseInt(h[0]), Integer.parseInt(h[1]), Integer.parseInt(h[2]));
    }

    // ---------- apoyo ----------

    private static String textoFecha(DateTime d) {
        return String.format("%04d-%02d-%02d", d.getAnio(), d.getMes(), d.getDia());
    }

    private static String textoHora(DateTime d) {
        return String.format("%02d:%02d:%02d", d.getHora(), d.getMinuto(), d.getSegundo());
    }
}
