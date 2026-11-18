package ucsur.pe.repository;

import ucsur.pe.db.CatalogoDao;
import ucsur.pe.model.Promocion;
import java.util.List;
import java.util.Optional;

/** Implementa PromocionRepository con MySQL (promocion y promocion_dia); carga las promociones al crearse. */
public class PromocionJdbcRepository extends RepositorioJdbc<Promocion> implements PromocionRepository {
    private final CatalogoDao dao;

    public PromocionJdbcRepository(CatalogoDao dao) {
        super("la promoción", Promocion::getIdPromocion);
        this.dao = dao;
        cargar(dao.listarPromociones());
    }

    @Override public void guardar(Promocion promocion) { guardarCon(promocion, dao::insertarPromocion, dao::actualizarPromocion); }
    @Override public List<Promocion> listarTodas() { return todos(); }

    @Override public Optional<Promocion> buscarPorId(int idPromocion) { return porId(idPromocion); }
}
