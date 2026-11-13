package ucsur.pe.repository;

import ucsur.pe.model.Pedido;
import ucsur.pe.model.Sede;
import java.util.List;

/** Implementación en memoria de PedidoRepository (una lista hace de tabla). */
public class PedidoMemoriaRepository extends RepositorioMemoria<Pedido> implements PedidoRepository {
    @Override public void guardar(Pedido pedido) {
        if (pedido != null && pedido.getIdPedido() == 0) {
            pedido.asignarId(generarId());
        }
        agregar(pedido);
    }
    @Override public List<Pedido> listarPorSede(Sede sede) { return filtrar(p -> sede.equals(p.getSede())); }
}
