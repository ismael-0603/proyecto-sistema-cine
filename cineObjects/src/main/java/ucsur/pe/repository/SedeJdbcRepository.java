package ucsur.pe.repository;

import ucsur.pe.db.SedeDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Butaca;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoEntrada;
import ucsur.pe.model.TipoSala;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementa SedeRepository con MySQL. Al crearse carga una sola vez todas las sedes con sus salas,
 * butacas, funciones y las entradas que ocupan butacas: así todas las pantallas comparten los mismos
 * objetos Funcion y Butaca, igual que en memoria.
 * Guardar una sede escribe sus datos y su cartelera: inserta las funciones nuevas y actualiza
 * solo las que cambiaron (reprogramadas, canceladas, reactivadas o con otro estado).
 */
public class SedeJdbcRepository extends RepositorioJdbc<Sede> implements SedeRepository {
    private final SedeDao dao;
    private final List<TipoSala> tiposSala;
    /** Funciones ya guardadas en la BD (id -> objeto), para saber si una es nueva. */
    private final Map<Integer, Funcion> funciones = new HashMap<>();
    /** Lo último que se escribió de cada función, para no actualizar las que no cambiaron. */
    private final Map<Integer, String> firmaGuardada = new HashMap<>();
    /** Entradas BLOQUEADA o VENDIDA cargadas al arrancar (id -> objeto); las compras deben reutilizarlas. */
    private final Map<Integer, Entrada> entradas;
    /** Butacas por id, para armar las entradas de las compras guardadas. */
    private final Map<Integer, Butaca> butacas = new HashMap<>();

    public SedeJdbcRepository(SedeDao dao, PeliculaRepository peliculas, TipoEntradaRepository tiposEntrada) {
        super("la sede", Sede::getIdSede);
        this.dao = dao;
        this.tiposSala = dao.listarTiposSala();
        SedeDao.Carga carga = dao.cargarSedes(porIdDe(tiposSala, TipoSala::getIdTipoSala),
                porIdDe(peliculas.listarTodas(), Pelicula::getIdPelicula),
                porIdDe(tiposEntrada.listarTodos(), TipoEntrada::getIdTipoEntrada));
        cargar(carga.sedes());
        this.entradas = new HashMap<>(carga.entradas());
        for (Sede sede : carga.sedes()) {
            for (Funcion f : sede.getCartelera()) {
                funciones.put(f.getIdFuncion(), f);
                firmaGuardada.put(f.getIdFuncion(), firma(f));
            }
        }
        indexarButacas();
    }

    @Override
    public void guardar(Sede sede) {
        guardarCon(sede, dao::insertarSede, dao::actualizarSede);
        for (Funcion f : sede.getCartelera()) {
            guardarFuncion(f);
        }
    }

    /** Inserta la función si es nueva o la actualiza si cambió algo desde la última vez. */
    private void guardarFuncion(Funcion f) {
        Funcion existente = funciones.get(f.getIdFuncion());
        if (f.getIdFuncion() <= 0) {
            dao.insertarFuncion(f); // la base genera el id (AUTO_INCREMENT) y el DAO se lo asigna
            funciones.put(f.getIdFuncion(), f);
        } else if (existente == null) {
            throw new RepositoryException("El id de la función lo genera la base: créela con id 0 (llegó con "
                    + f.getIdFuncion() + ")");
        } else if (existente != f) {
            throw new RepositoryException("Ya existe la función con id " + f.getIdFuncion());
        } else if (!firma(f).equals(firmaGuardada.get(f.getIdFuncion()))) {
            dao.actualizarFuncion(f);
        } else {
            return; // no cambió nada
        }
        firmaGuardada.put(f.getIdFuncion(), firma(f));
    }

    /** Los datos de la función que se escriben al actualizarla. */
    private static String firma(Funcion f) {
        return f.getFecha().soloFecha() + "|" + f.getHoraInicio().soloHora() + "|" + f.getIdioma() + "|" + f.getEstado();
    }

    @Override public Optional<Sede> buscarPorId(int idSede) { return porId(idSede); }
    @Override public List<Sede> listarTodas() { return todos(); }

    /** Formatos de sala de la BD (2D, 3D...), para el formulario de salas. */
    public List<TipoSala> listarTiposSala() { return List.copyOf(tiposSala); }

    /**
     * Una entrada BLOQUEADA o VENDIDA que ya cuelga de su función. Las compras la reutilizan al cargarse
     * (en vez de crear otra para la misma butaca), así hay un solo objeto por entrada.
     */
    public Optional<Entrada> buscarEntrada(int idEntrada) { return Optional.ofNullable(entradas.get(idEntrada)); }

    /** Función por id (la misma que está en la cartelera de su sede). */
    public Optional<Funcion> buscarFuncion(int idFuncion) { return Optional.ofNullable(funciones.get(idFuncion)); }

    /** Butaca por id (la misma que está en el mapa de su sala). */
    public Optional<Butaca> buscarButaca(int idButaca) {
        Butaca butaca = butacas.get(idButaca);
        if (butaca == null) { // una sala nueva o redistribuida en esta sesión tiene butacas que aún no estaban
            indexarButacas();
            butaca = butacas.get(idButaca);
        }
        return Optional.ofNullable(butaca);
    }

    private void indexarButacas() {
        butacas.clear();
        for (Sede sede : listarTodas()) {
            for (Sala sala : sede.getSalas()) {
                for (Butaca b : sala.obtenerMapaButacas()) butacas.put(b.getIdButaca(), b);
            }
        }
    }
}
