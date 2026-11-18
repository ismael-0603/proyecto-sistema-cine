package ucsur.pe.repository;

import ucsur.pe.db.PostventaDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Calificacion;
import ucsur.pe.model.Compra;

/**
 * Implementa CalificacionRepository con MySQL. Carga las calificaciones al crearse, cada una con el mismo
 * objeto Compra que tiene CompraRepository. Una calificación no se edita: guardar solo inserta.
 */
public class CalificacionJdbcRepository extends RepositorioJdbc<Calificacion> implements CalificacionRepository {
    private final PostventaDao dao;

    public CalificacionJdbcRepository(PostventaDao dao, CompraRepository compras) {
        super("la calificación", Calificacion::getIdCalificacion);
        this.dao = dao;
        cargar(dao.listarCalificaciones(porIdDe(compras.listarTodas(), Compra::getIdCompra)));
    }

    @Override
    public void guardar(Calificacion calificacion) {
        guardarCon(calificacion, dao::insertarCalificacion, c -> {
            throw new RepositoryException("Una calificación ya registrada no se modifica");
        });
    }

    @Override
    public boolean existePorCompra(Compra compra) {
        return !filtrar(c -> c.getCompra() == compra).isEmpty();
    }
}
