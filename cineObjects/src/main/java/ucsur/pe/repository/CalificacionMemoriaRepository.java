package ucsur.pe.repository;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;
import java.util.Optional;

/** Implementación en memoria de CalificacionRepository (una lista hace de tabla). */
public class CalificacionMemoriaRepository extends RepositorioMemoria<Calificacion> implements CalificacionRepository {
    public CalificacionMemoriaRepository() { super(Calificacion::getIdCalificacion, Calificacion::asignarId); }

    @Override public void guardar(Calificacion calificacion) { agregar(calificacion); } // si es nuevo, recibe su id (AUTO_INCREMENT)
    @Override public boolean existePorCompra(Compra compra) { return buscarPorCompra(compra).isPresent(); }
    @Override public Optional<Calificacion> buscarPorCompra(Compra compra) { return buscarPrimero(c -> c.getCompra() == compra); }
}
