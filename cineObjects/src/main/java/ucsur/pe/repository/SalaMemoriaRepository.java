package ucsur.pe.repository;

import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de SalaRepository (una lista hace de tabla). */
public class SalaMemoriaRepository extends RepositorioMemoria<Sala> implements SalaRepository {
    @Override public void guardar(Sala sala) { agregar(sala); }
    @Override public Optional<Sala> buscarPorId(int idSala) { return buscarPrimero(s -> s.getIdSala() == idSala); }
    @Override public List<Sala> listarPorSede(Sede sede) { return filtrar(s -> s.getSede().equals(sede)); }
    @Override public int siguienteId() { return generarId(); }
}
