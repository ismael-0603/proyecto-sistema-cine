package ucsur.pe.repository;

import ucsur.pe.db.CatalogoDao;
import ucsur.pe.model.Cupon;
import java.util.List;
import java.util.Optional;

/** Implementa CuponRepository con MySQL (tabla cupon); carga los cupones con sus usos una vez al crearse. */
public class CuponJdbcRepository extends RepositorioJdbc<Cupon> implements CuponRepository {
    private final CatalogoDao dao;

    public CuponJdbcRepository(CatalogoDao dao) {
        super("el cupón", Cupon::getIdCupon);
        this.dao = dao;
        cargar(dao.listarCupones());
    }

    @Override public void guardar(Cupon cupon) { guardarCon(cupon, dao::insertarCupon, dao::actualizarCupon); }

    @Override public List<Cupon> listarTodos() { return todos(); }

    @Override public Optional<Cupon> buscarPorId(int idCupon) { return porId(idCupon); }
}
