package ucsur.pe.repository;

import ucsur.pe.db.PostventaDao;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import java.util.List;

/**
 * Implementa ReclamoRepository con MySQL. Carga todos los reclamos al crearse, cada uno con el mismo
 * objeto Compra que tiene CompraRepository (así la bandeja del administrador ve los de sesiones anteriores).
 * Un reclamo nuevo llega con id 0 y la base le genera uno, como el AUTO_INCREMENT que simula la versión en memoria.
 */
public class ReclamoJdbcRepository extends RepositorioJdbc<Reclamo> implements ReclamoRepository {
    private final PostventaDao dao;

    public ReclamoJdbcRepository(PostventaDao dao, CompraRepository compras) {
        super("el reclamo", Reclamo::getIdReclamo);
        this.dao = dao;
        cargar(dao.listarReclamos(porIdDe(compras.listarTodas(), Compra::getIdCompra)));
    }

    @Override public void guardar(Reclamo reclamo) { guardarCon(reclamo, dao::insertarReclamo, dao::actualizarReclamo); }

    @Override public List<Reclamo> listarPorCompra(Compra compra) { return filtrar(r -> r.getCompra() == compra); }
    @Override public List<Reclamo> listarTodos() { return todos(); }
}
