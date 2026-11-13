package ucsur.pe.repository;

import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de SedeRepository (una lista hace de tabla). */
public class SedeMemoriaRepository extends RepositorioMemoria<Sede> implements SedeRepository {
    @Override public void guardar(Sede sede) { agregar(sede); }
    @Override public Optional<Sede> buscarPorId(int idSede) { return buscarPrimero(s -> s.getIdSede() == idSede); }
    @Override public List<Sede> listarTodas() { return todos(); }
}
