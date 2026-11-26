package ucsur.pe.db;

import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
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
 * Por qué existe: traduce entre las tablas de infraestructura (sede, sala, butaca, funcion, tipo_sala)
 * y los objetos del modelo. Lee con una consulta por tabla (no una por fila), como listarTodos de MVCone,
 * y arma las relaciones en Java usando los métodos del modelo (agregarSala, agregarButaca, agregarFuncion),
 * así los datos de la BD pasan por las mismas validaciones que los creados desde las pantallas.
 */
public class SedeMySqlDao implements SedeDao {

    private final Connection conexion;
    private final Reloj reloj; // las funciones lo necesitan para calcular su estado actual

    public SedeMySqlDao(Connection conexion, Reloj reloj) {
        this.conexion = conexion;
        this.reloj = reloj;
    }

    // ---------- Lectura ----------

    @Override
    public List<TipoSala> listarTiposSala() {
        String sql = "SELECT id_tipo_sala, formato, descripcion, tarifa_adicional FROM tipo_sala ORDER BY id_tipo_sala";
        List<TipoSala> tipos = new ArrayList<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.add(new TipoSala(rs.getInt("id_tipo_sala"), rs.getString("formato"),
                        rs.getString("descripcion"), Dinero.leer(rs, "tarifa_adicional")));
            }
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar los tipos de sala.", e);
        }
        return tipos;
    }

    @Override
    public Carga cargarSedes(Map<Integer, TipoSala> tiposSala, Map<Integer, Pelicula> peliculas,
                             Map<Integer, TipoEntrada> tiposEntrada) {
        try {
            Map<Integer, Sede> sedes = leerSedes();
            Map<Integer, Sala> salas = leerSalas(sedes, tiposSala);
            Map<Integer, Butaca> butacas = leerButacas(salas);
            Map<Integer, Funcion> funciones = leerFunciones(salas, peliculas);
            Map<Integer, Entrada> entradas = leerEntradasOcupadas(funciones, butacas, tiposEntrada);
            return new Carga(new ArrayList<>(sedes.values()), entradas);
        } catch (SQLException e) {
            throw new RepositoryException("No se pudieron cargar las sedes, salas y funciones.", e);
        }
    }

    private Map<Integer, Sede> leerSedes() throws SQLException {
        String sql = """
                SELECT id_sede, nombre, direccion, distrito, telefono, horario_atencion, estado
                FROM sede
                ORDER BY id_sede
                """;
        Map<Integer, Sede> sedes = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sede s = new Sede(rs.getInt("id_sede"), rs.getString("nombre"), rs.getString("direccion"),
                        rs.getString("distrito"), rs.getString("telefono"), rs.getString("horario_atencion"),
                        rs.getString("estado"));
                sedes.put(s.getIdSede(), s);
            }
        }
        return sedes;
    }

    private Map<Integer, Sala> leerSalas(Map<Integer, Sede> sedes, Map<Integer, TipoSala> tiposSala) throws SQLException {
        String sql = "SELECT id_sala, numero, capacidad, estado, id_sede, id_tipo_sala FROM sala ORDER BY id_sede, numero";
        Map<Integer, Sala> salas = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sede sede = exigir(sedes, rs.getInt("id_sede"), "la sede");
                TipoSala tipo = exigir(tiposSala, rs.getInt("id_tipo_sala"), "el tipo de sala");
                Sala sala = new Sala(rs.getInt("id_sala"), rs.getInt("numero"), rs.getInt("capacidad"),
                        rs.getString("estado"), sede, tipo);
                sede.agregarSala(sala);
                salas.put(sala.getIdSala(), sala);
            }
        }
        return salas;
    }

    private Map<Integer, Butaca> leerButacas(Map<Integer, Sala> salas) throws SQLException {
        // Orden del mapa: fila (A..Z antes que AA) y número, igual que Sala.generarButacas
        String sql = """
                SELECT id_butaca, fila, numero, tipo, bloque, id_sala
                FROM butaca
                ORDER BY id_sala, CHAR_LENGTH(fila), fila, numero
                """;
        Map<Integer, Butaca> butacas = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Sala sala = exigir(salas, rs.getInt("id_sala"), "la sala");
                Butaca b = new Butaca(rs.getInt("id_butaca"), rs.getString("fila"), rs.getInt("numero"),
                        rs.getString("tipo"), rs.getString("bloque"), sala);
                sala.agregarButaca(b);
                butacas.put(b.getIdButaca(), b);
            }
        }
        return butacas;
    }

    private Map<Integer, Funcion> leerFunciones(Map<Integer, Sala> salas, Map<Integer, Pelicula> peliculas)
            throws SQLException {
        String sql = """
                SELECT id_funcion, fecha, hora_inicio, idioma, estado, id_pelicula, id_sala
                FROM funcion
                ORDER BY fecha, hora_inicio, id_funcion
                """;
        Map<Integer, Funcion> funciones = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id_funcion");
                Sala sala = exigir(salas, rs.getInt("id_sala"), "la sala");
                Pelicula pelicula = exigir(peliculas, rs.getInt("id_pelicula"), "la película");
                Funcion f = new Funcion(id, Fechas.desdeDate(rs.getDate("fecha")),
                        Fechas.desdeTime(rs.getTime("hora_inicio")), rs.getString("idioma"), rs.getString("estado"),
                        pelicula, sala, reloj);
                try {
                    sala.getSede().agregarFuncion(f); // valida que no se cruce con otra función de la sala
                } catch (IllegalStateException | IllegalArgumentException e) {
                    throw new RepositoryException("La función #" + id + " de la base de datos no es válida: "
                            + e.getMessage(), e);
                }
                funciones.put(id, f);
            }
        }
        return funciones;
    }

    /**
     * Solo las entradas que ocupan una butaca: VENDIDA o BLOQUEADA (las vencidas no ocupan, lo decide la función).
     * Las demás (anuladas, expiradas) se leen con su compra cuando haga falta.
     */
    private Map<Integer, Entrada> leerEntradasOcupadas(Map<Integer, Funcion> funciones, Map<Integer, Butaca> butacas,
                                                       Map<Integer, TipoEntrada> tiposEntrada) throws SQLException {
        String sql = """
                SELECT id_entrada, codigo_qr, precio, fecha_emision, expira_bloqueo, estado,
                       id_funcion, id_butaca, id_tipo_entrada
                FROM entrada
                WHERE estado IN ('BLOQUEADA', 'VENDIDA')
                ORDER BY id_entrada
                """;
        Map<Integer, Entrada> entradas = new LinkedHashMap<>();
        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Entrada e = Entrada.restaurar(rs.getInt("id_entrada"),
                        exigir(funciones, rs.getInt("id_funcion"), "la función"),
                        exigir(butacas, rs.getInt("id_butaca"), "la butaca"),
                        exigir(tiposEntrada, rs.getInt("id_tipo_entrada"), "el tipo de entrada"),
                        rs.getString("estado"), Dinero.leer(rs, "precio"), rs.getString("codigo_qr"),
                        Fechas.desdeTimestamp(rs.getTimestamp("fecha_emision")),
                        Fechas.desdeTimestamp(rs.getTimestamp("expira_bloqueo")));
                entradas.put(e.getIdEntrada(), e);
            }
        }
        return entradas;
    }

    // ---------- Sedes ----------

    /** Inserta la sede sin id y le asigna el que genera la base (AUTO_INCREMENT). */
    @Override
    public void insertarSede(Sede sede) {
        String sql = """
                INSERT INTO sede (nombre, direccion, distrito, telefono, horario_atencion, estado)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            llenarSede(ps, sede, 1);
            ps.executeUpdate();
            sede.asignarId(Claves.generada(ps));
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar la sede " + sede.getNombre() + ".", e);
        }
    }

    @Override
    public void actualizarSede(Sede sede) {
        String sql = """
                UPDATE sede
                SET nombre = ?, direccion = ?, distrito = ?, telefono = ?, horario_atencion = ?, estado = ?
                WHERE id_sede = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            llenarSede(ps, sede, 1);
            ps.setInt(7, sede.getIdSede());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar la sede " + sede.getNombre() + ".", e);
        }
    }

    /** Los 6 datos de la sede desde la posición indicada (los comparten el INSERT y el UPDATE). */
    private void llenarSede(PreparedStatement ps, Sede sede, int desde) throws SQLException {
        ps.setString(desde, sede.getNombre());
        ps.setString(desde + 1, sede.getDireccion());
        ps.setString(desde + 2, sede.getDistrito());
        ps.setString(desde + 3, sede.getTelefono());
        ps.setString(desde + 4, sede.getHorarioAtencion());
        ps.setString(desde + 5, sede.getEstado());
    }

    // ---------- Salas y butacas ----------

    /**
     * Inserta la sala y sus butacas en una transacción. Los ids que genera la base se asignan a los objetos
     * recién después del commit: si algo se revierte, ningún objeto queda con el id de una fila que no existe.
     */
    @Override
    public void insertarSala(Sala sala) {
        String sql = """
                INSERT INTO sala (numero, capacidad, estado, id_sede, id_tipo_sala)
                VALUES (?, ?, ?, ?, ?)
                """;
        int[] idSala = {0};
        Map<Butaca, Integer> idsButacas = new LinkedHashMap<>();
        Transaccion.ejecutar(conexion, "No se pudo registrar la sala " + sala.getNumero() + ".", () -> {
            try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, sala.getNumero());
                ps.setInt(2, sala.getCapacidad());
                ps.setString(3, sala.getEstado());
                ps.setInt(4, sala.getSede().getIdSede());
                ps.setInt(5, sala.getTipoSala().getIdTipoSala());
                ps.executeUpdate();
                idSala[0] = Claves.generada(ps);
            }
            insertarButacasNuevas(sala, idSala[0], idsButacas);
        });
        sala.asignarId(idSala[0]);
        idsButacas.forEach(Butaca::asignarId);
    }

    @Override
    public void actualizarSala(Sala sala) {
        String sql = """
                UPDATE sala SET numero = ?, capacidad = ?, estado = ?, id_tipo_sala = ?
                WHERE id_sala = ?
                """;
        Map<Butaca, Integer> idsButacas = new LinkedHashMap<>();
        Transaccion.ejecutar(conexion, "No se pudo actualizar la sala " + sala.getNumero() + ".", () -> {
            boolean redistribuida = sala.obtenerMapaButacas().stream().anyMatch(b -> b.getIdButaca() == 0);
            if (redistribuida) {
                // Sala.redistribuir crea todas las butacas de nuevo (solo se permite sin funciones):
                // primero se borran las anteriores, para que el trigger de capacidad cuente bien.
                try (PreparedStatement borrar = conexion.prepareStatement("DELETE FROM butaca WHERE id_sala = ?")) {
                    borrar.setInt(1, sala.getIdSala());
                    borrar.executeUpdate();
                }
            }
            try (PreparedStatement ps = conexion.prepareStatement(sql)) {
                ps.setInt(1, sala.getNumero());
                ps.setInt(2, sala.getCapacidad());
                ps.setString(3, sala.getEstado());
                ps.setInt(4, sala.getTipoSala().getIdTipoSala());
                ps.setInt(5, sala.getIdSala());
                ps.executeUpdate();
            }
            if (redistribuida) {
                insertarButacasNuevas(sala, sala.getIdSala(), idsButacas);
            }
        });
        idsButacas.forEach(Butaca::asignarId);
    }

    /** Inserta las butacas sin id (0) y anota el id que genera la base (va dentro de la transacción). */
    private void insertarButacasNuevas(Sala sala, int idSala, Map<Butaca, Integer> idsNuevos) throws SQLException {
        String sql = "INSERT INTO butaca (fila, numero, tipo, bloque, id_sala) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Butaca b : sala.obtenerMapaButacas()) {
                if (b.getIdButaca() != 0) continue;
                ps.setString(1, b.getFila());
                ps.setInt(2, b.getNumero());
                ps.setString(3, b.getTipo());
                ps.setString(4, b.getBloque());
                ps.setInt(5, idSala);
                ps.executeUpdate();
                idsNuevos.put(b, Claves.generada(ps));
            }
        }
    }

    // ---------- Funciones ----------

    /** Inserta la función sin id y le asigna el que genera la base (AUTO_INCREMENT). */
    @Override
    public void insertarFuncion(Funcion funcion) {
        String sql = """
                INSERT INTO funcion (fecha, hora_inicio, idioma, estado, id_pelicula, id_sala)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Fechas.aDate(funcion.getFecha()));
            ps.setTime(2, Fechas.aTime(funcion.getHoraInicio()));
            ps.setString(3, funcion.getIdioma());
            ps.setString(4, funcion.getEstado());
            ps.setInt(5, funcion.getPelicula().getIdPelicula());
            ps.setInt(6, funcion.getSala().getIdSala());
            ps.executeUpdate();
            funcion.asignarId(Claves.generada(ps));
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo registrar la función " + funcion + ".", e);
        }
    }

    @Override
    public void actualizarFuncion(Funcion funcion) {
        String sql = """
                UPDATE funcion SET fecha = ?, hora_inicio = ?, idioma = ?, estado = ?
                WHERE id_funcion = ?
                """;
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setDate(1, Fechas.aDate(funcion.getFecha()));
            ps.setTime(2, Fechas.aTime(funcion.getHoraInicio()));
            ps.setString(3, funcion.getIdioma());
            ps.setString(4, funcion.getEstado());
            ps.setInt(5, funcion.getIdFuncion());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("No se pudo actualizar la función " + funcion + ".", e);
        }
    }

    // ---------- apoyo ----------

    /** Busca el objeto ya cargado (que = "la sede", "el tipo de sala"...); si falta, la base tiene una referencia rota y se avisa con claridad. */
    private static <T> T exigir(Map<Integer, T> mapa, int id, String que) {
        T objeto = mapa.get(id);
        if (objeto == null) {
            throw new RepositoryException("No se encontró " + que + " con id " + id + " en la base de datos.");
        }
        return objeto;
    }
}
