package ucsur.pe.repository;

import ucsur.pe.db.CatalogoDao;
import ucsur.pe.model.TipoEntrada;
import java.util.List;
import java.util.Optional;

/** Implementa TipoEntradaRepository con MySQL (tabla tipo_entrada); carga las tarifas una vez al crearse. */
public class TipoEntradaJdbcRepository extends RepositorioJdbc<TipoEntrada> implements TipoEntradaRepository {
    private final CatalogoDao dao;

    public TipoEntradaJdbcRepository(CatalogoDao dao) {
        super("el tipo de entrada", TipoEntrada::getIdTipoEntrada);
        this.dao = dao;
        cargar(dao.listarTiposEntrada());
    }

    @Override public void guardar(TipoEntrada tipo) { guardarCon(tipo, dao::insertarTipoEntrada, dao::actualizarTipoEntrada); }
    @Override public Optional<TipoEntrada> buscarPorId(int idTipoEntrada) { return porId(idTipoEntrada); }
    @Override public List<TipoEntrada> listarActivos() { return filtrar(TipoEntrada::isEstado); }
    @Override public List<TipoEntrada> listarTodos() { return todos(); }
}
