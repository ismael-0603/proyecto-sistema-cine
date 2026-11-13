package ucsur.pe.repository;

import ucsur.pe.model.Cupon;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de CuponRepository (una lista hace de tabla). */
public class CuponMemoriaRepository extends RepositorioMemoria<Cupon> implements CuponRepository {
    @Override public void guardar(Cupon cupon) { agregar(cupon); }
    @Override public Optional<Cupon> buscarPorCodigo(String codigo) {
        return codigo == null ? Optional.empty() : buscarPrimero(c -> c.getCodigo().equalsIgnoreCase(codigo.trim()));
    }
    @Override public List<Cupon> listarTodos() { return todos(); }
}
