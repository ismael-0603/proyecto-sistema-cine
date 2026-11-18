package ucsur.pe.repository;

import ucsur.pe.model.TipoEntrada;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de TipoEntradaRepository (una lista hace de tabla). */
public class TipoEntradaMemoriaRepository extends RepositorioMemoria<TipoEntrada> implements TipoEntradaRepository {
    public TipoEntradaMemoriaRepository() { super(TipoEntrada::getIdTipoEntrada, TipoEntrada::asignarId); }

    @Override public void guardar(TipoEntrada tipoEntrada) { agregar(tipoEntrada); }
    @Override public Optional<TipoEntrada> buscarPorId(int idTipoEntrada) { return porId(idTipoEntrada); }
    @Override public List<TipoEntrada> listarActivos() { return filtrar(TipoEntrada::isEstado); }
    @Override public List<TipoEntrada> listarTodos() { return todos(); }
}
