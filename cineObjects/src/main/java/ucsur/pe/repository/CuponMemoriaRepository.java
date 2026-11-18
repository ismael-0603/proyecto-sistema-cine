package ucsur.pe.repository;

import ucsur.pe.model.Cupon;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de CuponRepository (una lista hace de tabla). */
public class CuponMemoriaRepository extends RepositorioMemoria<Cupon> implements CuponRepository {
    public CuponMemoriaRepository() { super(Cupon::getIdCupon, Cupon::asignarId); }

    @Override public void guardar(Cupon cupon) { agregar(cupon); }
    @Override public Optional<Cupon> buscarPorId(int idCupon) { return porId(idCupon); }
    @Override public List<Cupon> listarTodos() { return todos(); }
}
