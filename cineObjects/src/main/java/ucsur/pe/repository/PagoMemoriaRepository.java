package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import java.util.List;

/** Implementación en memoria de PagoRepository (una lista hace de tabla). */
public class PagoMemoriaRepository extends RepositorioMemoria<TransaccionPago> implements PagoRepository {
    private final CompraRepository compras;

    /** Recibe las compras para guardar juntas la compra confirmada y su pago, como PagoJdbcRepository. */
    public PagoMemoriaRepository(CompraRepository compras) {
        super(TransaccionPago::getIdTransaccion, TransaccionPago::asignarId);
        this.compras = compras;
    }

    @Override public void guardar(TransaccionPago pago) { agregar(pago); }

    /** En memoria no hay transacciones: se guarda la compra (recibe su serie) y luego el pago. */
    @Override
    public void guardarConCompra(TransaccionPago pago) {
        compras.guardar(pago.getCompra());
        agregar(pago);
    }
    @Override public List<TransaccionPago> listarPorCompra(Compra compra) { return filtrar(p -> p.getCompra() == compra); }
}
