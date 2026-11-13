package ucsur.pe.repository;

import ucsur.pe.model.Promocion;
import java.util.List;

/** Implementación en memoria de PromocionRepository (una lista hace de tabla). */
public class PromocionMemoriaRepository extends RepositorioMemoria<Promocion> implements PromocionRepository {
    @Override public void guardar(Promocion promocion) { agregar(promocion); }
    @Override public List<Promocion> listarTodas() { return todos(); }
}
