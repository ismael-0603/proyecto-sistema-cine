package ucsur.pe.repository;

import ucsur.pe.model.Promocion;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de PromocionRepository (una lista hace de tabla). */
public class PromocionMemoriaRepository extends RepositorioMemoria<Promocion> implements PromocionRepository {
    public PromocionMemoriaRepository() { super(Promocion::getIdPromocion, Promocion::asignarId); }

    @Override public void guardar(Promocion promocion) { agregar(promocion); }
    @Override public Optional<Promocion> buscarPorId(int idPromocion) { return porId(idPromocion); }
    @Override public List<Promocion> listarTodas() { return todos(); }
}
