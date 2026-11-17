package ucsur.pe.repository;

import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;

/** Implementación en memoria de CalificacionRepository (una lista hace de tabla). */
public class CalificacionMemoriaRepository extends RepositorioMemoria<Calificacion> implements CalificacionRepository {
    @Override public void guardar(Calificacion calificacion) {
        if (calificacion != null && calificacion.getIdCalificacion() == 0) {
            calificacion.asignarId(generarId()); // simula el AUTO_INCREMENT
        }
        agregar(calificacion);
    }
    @Override public boolean existePorCompra(Compra compra) { return buscarPrimero(c -> c.getCompra() == compra).isPresent(); }
}
