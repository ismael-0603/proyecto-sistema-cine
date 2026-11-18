package ucsur.pe.repository;

import ucsur.pe.db.SedeDao;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementa SalaRepository con MySQL. No vuelve a leer la BD: usa las mismas salas que cargó
 * SedeJdbcRepository (están dentro de cada sede), para que haya un solo objeto por sala.
 */
public class SalaJdbcRepository extends RepositorioJdbc<Sala> implements SalaRepository {
    private final SedeDao dao;
    private final List<TipoSala> tiposSala;

    /** Recibe los formatos que ya cargó SedeJdbcRepository, para usar los mismos objetos TipoSala. */
    public SalaJdbcRepository(SedeDao dao, SedeRepository sedes, List<TipoSala> tiposSala) {
        super("la sala", Sala::getIdSala);
        this.dao = dao;
        this.tiposSala = List.copyOf(tiposSala);
        List<Sala> salas = new ArrayList<>();
        for (Sede sede : sedes.listarTodas()) salas.addAll(sede.getSalas());
        cargar(salas);
    }

    /** Una sala nueva se inserta con sus butacas; una existente se actualiza (y sus butacas si se redistribuyó). */
    @Override public void guardar(Sala sala) { guardarCon(sala, dao::insertarSala, dao::actualizarSala); }
    @Override public Optional<Sala> buscarPorId(int idSala) { return porId(idSala); }
    @Override public List<TipoSala> listarTiposSala() { return tiposSala; }
}
