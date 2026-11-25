package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: traduce entre las tablas reclamo y calificacion y los objetos del modelo.
 * Los permisos (quién reclama, quién atiende) los revisa PostventaService, no este DAO.
 */
public class PostventaMySqlDao implements PostventaDao {

    private final Connection conexion;

    public PostventaMySqlDao(Connection conexion) {
        this.conexion = conexion;
    }

    // ---------- Reclamos ----------

    @Override
    public List<Reclamo> listarReclamos(Map<Integer, Compra> compras) {
        String sql = """
                SELECT id_reclamo, tipo, descripcion, fecha, estado, respuesta, id_compra
                FROM reclamo
                ORDER BY fecha, id_reclamo
                """;
        List<Reclamo> reclamos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                reclamos.add(Reclamo.restaurar(rs.getInt("id_reclamo"), rs.getString("tipo"), rs.getString("descripcion"),
                        Fechas.desdeTimestamp(rs.getTimestamp("fecha")), rs.getString("estado"), rs.getString("respuesta"),
                        compraDe(compras, rs.getInt("id_compra"))));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los reclamos.", e);
        }
        return reclamos;
    }

    @Override
    public void insertarReclamo(Reclamo reclamo) {
        String sql = """
                INSERT INTO reclamo (tipo, descripcion, fecha, estado, respuesta, id_compra)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, reclamo.getTipo());
            ps.setString(2, reclamo.getDescripcion());
            ps.setTimestamp(3, Fechas.aTimestamp(reclamo.getFecha()));
            ps.setString(4, reclamo.getEstado());
            ps.setString(5, reclamo.getRespuesta());
            ps.setInt(6, reclamo.getCompra().getIdCompra());
            ps.executeUpdate();
            reclamo.asignarId(Claves.generada(ps)); // la base genera el id (AUTO_INCREMENT)
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar el reclamo de la compra #" + reclamo.getCompra().getIdCompra() + ".", e);
        }
    }

    @Override
    public void actualizarReclamo(Reclamo reclamo) {
        try (PreparedStatement ps = conexion.prepareStatement(
                "UPDATE reclamo SET estado = ?, respuesta = ? WHERE id_reclamo = ?")) {
            ps.setString(1, reclamo.getEstado());
            ps.setString(2, reclamo.getRespuesta());
            ps.setInt(3, reclamo.getIdReclamo());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar el reclamo #" + reclamo.getIdReclamo() + ".", e);
        }
    }

    // ---------- Calificaciones ----------

    @Override
    public List<Calificacion> listarCalificaciones(Map<Integer, Compra> compras) {
        String sql = "SELECT id_calificacion, puntuacion, comentario, fecha, id_compra FROM calificacion ORDER BY id_calificacion";
        List<Calificacion> calificaciones = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                calificaciones.add(Calificacion.restaurar(rs.getInt("id_calificacion"), rs.getInt("puntuacion"),
                        rs.getString("comentario"), Fechas.desdeTimestamp(rs.getTimestamp("fecha")),
                        compraDe(compras, rs.getInt("id_compra"))));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar las calificaciones.", e);
        }
        return calificaciones;
    }

    @Override
    public void insertarCalificacion(Calificacion calificacion) {
        String sql = """
                INSERT INTO calificacion (puntuacion, comentario, fecha, id_compra)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, calificacion.getPuntuacion());
            ps.setString(2, calificacion.getComentario());
            ps.setTimestamp(3, Fechas.aTimestamp(calificacion.getFecha()));
            ps.setInt(4, calificacion.getCompra().getIdCompra());
            ps.executeUpdate();
            calificacion.asignarId(Claves.generada(ps)); // la base genera el id (AUTO_INCREMENT)
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new RepositoryException("Esta compra ya fue calificada.", e); // uq_calificacion_compra
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar la calificación.", e);
        }
    }

    // ---------- apoyo ----------

    private static Compra compraDe(Map<Integer, Compra> compras, int idCompra) {
        Compra compra = compras.get(idCompra);
        if (compra == null) {
            throw new RepositoryException("No se encontró la compra con id " + idCompra + " en la base de datos.");
        }
        return compra;
    }
}
