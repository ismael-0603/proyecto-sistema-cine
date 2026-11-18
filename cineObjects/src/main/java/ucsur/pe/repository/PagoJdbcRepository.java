package ucsur.pe.repository;

import ucsur.pe.db.CompraDao;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import java.util.List;

/**
 * Implementa PagoRepository con MySQL (tabla transaccion_pago). Parte de los pagos que se cargaron
 * junto con las compras, así cada pago apunta al mismo objeto Compra.
 */
public class PagoJdbcRepository extends RepositorioJdbc<TransaccionPago> implements PagoRepository {
    private final CompraDao dao;
    private final CompraJdbcRepository compras;

    public PagoJdbcRepository(CompraDao dao, CompraJdbcRepository compras) {
        super("el pago", TransaccionPago::getIdTransaccion);
        this.dao = dao;
        this.compras = compras;
        cargar(compras.pagosCargados());
    }

    @Override public void guardar(TransaccionPago pago) { guardarCon(pago, dao::insertarPago, dao::actualizarPago); }

    /** Compra y pago en una sola transacción (CompraMySqlDao.guardarCompraConPago); el pago siempre es nuevo. */
    @Override
    public void guardarConCompra(TransaccionPago pago) {
        guardarCon(pago, p -> compras.guardarConPago(p.getCompra(), p), p -> {
            throw new RepositoryException("El pago #" + p.getIdTransaccion() + " ya estaba guardado");
        });
    }
    @Override public List<TransaccionPago> listarPorCompra(Compra compra) { return filtrar(p -> p.getCompra() == compra); }
}
