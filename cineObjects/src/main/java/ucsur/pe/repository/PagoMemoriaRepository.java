package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.TransaccionPago;
import java.util.List;

/** Implementación en memoria de PagoRepository (una lista hace de tabla). */
public class PagoMemoriaRepository extends RepositorioMemoria<TransaccionPago> implements PagoRepository {
    @Override public void guardar(TransaccionPago pago) { agregar(pago); }
    @Override public List<TransaccionPago> listarPorCompra(Compra compra) { return filtrar(p -> p.getCompra() == compra); }
    @Override public int siguienteId() { return generarId(); }
}
