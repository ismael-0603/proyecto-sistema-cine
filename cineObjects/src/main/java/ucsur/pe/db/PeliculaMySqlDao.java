package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Genero;
import ucsur.pe.model.Pelicula;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Por qué existe: traduce entre las filas de pelicula/genero/pelicula_genero y los objetos del modelo.
 * No aplica reglas de negocio (eso es de PeliculaAdminService), igual que ComprobanteMySqlDao en MVCone.
 */
public class PeliculaMySqlDao implements PeliculaDao {

    private final Connection conexion;

    /** La conexión llega desde afuera (DIP), como en MVCone. */
    public PeliculaMySqlDao(Connection conexion) {
        this.conexion = conexion;
    }

    @Override
    public List<Genero> listarGeneros() {
        String sql = "SELECT id_genero, nombre FROM genero ORDER BY id_genero";
        List<Genero> generos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                generos.add(new Genero(rs.getInt("id_genero"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los géneros.", e);
        }
        return generos;
    }

    @Override
    public List<Pelicula> listarTodas(Map<Integer, Genero> generos) {
        String sql = """
                SELECT id_pelicula, titulo, sinopsis, duracion_min, clasificacion_edad,
                       trailer_url, poster_url, fecha_estreno, estado
                FROM pelicula
                ORDER BY id_pelicula
                """;
        Map<Integer, Pelicula> peliculas = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pelicula p = new Pelicula(rs.getInt("id_pelicula"), rs.getString("titulo"), rs.getString("sinopsis"),
                        rs.getInt("duracion_min"), rs.getString("clasificacion_edad"), rs.getString("trailer_url"),
                        rs.getString("poster_url"), Fechas.desdeDate(rs.getDate("fecha_estreno")), rs.getString("estado"));
                peliculas.put(p.getIdPelicula(), p);
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar las películas.", e);
        }
        // Una sola consulta para los géneros de todas las películas (no una por película)
        String sqlGeneros = "SELECT id_pelicula, id_genero FROM pelicula_genero ORDER BY id_pelicula, id_genero";
        try (PreparedStatement ps = conexion.prepareStatement(sqlGeneros);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Pelicula p = peliculas.get(rs.getInt("id_pelicula"));
                Genero g = generos.get(rs.getInt("id_genero"));
                if (p != null && g != null) {
                    p.agregarGenero(g);
                }
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los géneros de las películas.", e);
        }
        return new ArrayList<>(peliculas.values());
    }

    /**
     * Inserta la película y sus géneros en una transacción. El id que genera la base (AUTO_INCREMENT)
     * se asigna a la película recién después del commit.
     */
    @Override
    public void insertar(Pelicula pelicula) {
        String sql = """
                INSERT INTO pelicula (titulo, sinopsis, duracion_min, clasificacion_edad,
                                      trailer_url, poster_url, fecha_estreno, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        int[] id = {0};
        Transaccion.ejecutar(conexion, "No se pudo registrar la película " + pelicula.getTitulo() + ".", () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, pelicula.getTitulo());
                ps.setString(2, pelicula.getSinopsis());
                ps.setInt(3, pelicula.getDuracion());
                ps.setString(4, pelicula.getClasificacionEdad());
                ps.setString(5, pelicula.getTrailer());
                ps.setString(6, pelicula.getPoster());
                ps.setDate(7, Fechas.aDate(pelicula.getFechaEstreno()));
                ps.setString(8, pelicula.getEstado());
                ps.executeUpdate();
                id[0] = Claves.generada(ps);
            }
            guardarGeneros(pelicula, id[0]);
        });
        pelicula.asignarId(id[0]);
    }

    @Override
    public void actualizar(Pelicula pelicula) {
        String sql = """
                UPDATE pelicula
                SET titulo = ?, sinopsis = ?, duracion_min = ?, clasificacion_edad = ?,
                    trailer_url = ?, poster_url = ?, fecha_estreno = ?, estado = ?
                WHERE id_pelicula = ?
                """;
        Transaccion.ejecutar(conexion, "No se pudo actualizar la película " + pelicula.getTitulo() + ".", () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setString(1, pelicula.getTitulo());
                ps.setString(2, pelicula.getSinopsis());
                ps.setInt(3, pelicula.getDuracion());
                ps.setString(4, pelicula.getClasificacionEdad());
                ps.setString(5, pelicula.getTrailer());
                ps.setString(6, pelicula.getPoster());
                ps.setDate(7, Fechas.aDate(pelicula.getFechaEstreno()));
                ps.setString(8, pelicula.getEstado());
                ps.setInt(9, pelicula.getIdPelicula());
                ps.executeUpdate();
            }
            guardarGeneros(pelicula, pelicula.getIdPelicula());
        });
    }

    // ---------- apoyo ----------

    /** Reemplaza los géneros de la película por los que tiene en Java (va dentro de la transacción). */
    private void guardarGeneros(Pelicula pelicula, int idPelicula) throws SQLException {
        try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM pelicula_genero WHERE id_pelicula = ?")) {
            borrar.setInt(1, idPelicula);
            borrar.executeUpdate();
        }
        String sql = "INSERT INTO pelicula_genero (id_pelicula, id_genero) VALUES (?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            for (Genero g : pelicula.getGeneros()) {
                ps.setInt(1, idPelicula);
                ps.setInt(2, g.getIdGenero());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
