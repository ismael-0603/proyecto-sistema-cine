package ucsur.pe.repository;

import ucsur.pe.db.CompraDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Sede;
import java.util.List;
import java.util.Objects;

/**
 * Implementa PedidoRepository con MySQL. En la BD todo pedido pertenece a una compra (pedido.id_compra),
 * así que no guarda una lista propia: los pedidos son los de las compras de CompraJdbcRepository
 * (los mismos objetos). Un pedido nuevo se inserta guardando su compra; uno existente solo actualiza
 * lo que cambia en la dulcería (estado, empleado, fecha de entrega).
 */
public class PedidoJdbcRepository implements PedidoRepository {
    private final CompraDao dao;
    private final CompraJdbcRepository compras;

    public PedidoJdbcRepository(CompraDao dao, CompraJdbcRepository compras) {
        this.dao = dao;
        this.compras = compras;
    }

    @Override
    public void guardar(Pedido pedido) {
        if (pedido == null) {
            throw new RepositoryException("No se puede guardar un valor nulo");
        }
        if (pedido.getIdPedido() <= 0) {
            Compra compra = compras.buscarPorPedido(pedido).orElseThrow(() ->
                    new RepositoryException("El pedido no pertenece a ninguna compra; asígnelo a una compra antes de guardarlo"));
            compras.guardar(compra); // inserta el pedido junto con su compra y le asigna el id
        } else {
            dao.actualizarPedido(pedido);
        }
    }

    /** Pedidos registrados de la sede (como en memoria, solo un pedido registrado tiene sede). */
    @Override
    public List<Pedido> listarPorSede(Sede sede) {
        return compras.listarTodas().stream()
                .map(Compra::getPedido)
                .filter(Objects::nonNull)
                .filter(p -> sede.equals(p.getSede()))
                .toList();
    }
}
