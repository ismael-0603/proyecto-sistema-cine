package ucsur.pe.repository;

import ucsur.pe.model.Pedido;
import ucsur.pe.model.Sede;
import java.util.List;

/** Implementación en memoria de PedidoRepository (una lista hace de tabla). */
public class PedidoMemoriaRepository extends RepositorioMemoria<Pedido> implements PedidoRepository {
    public PedidoMemoriaRepository() { super(Pedido::getIdPedido, Pedido::asignarId); }

    @Override public void guardar(Pedido pedido) { agregar(pedido); } // si es nuevo, recibe su id (AUTO_INCREMENT)
    @Override public List<Pedido> listarPorSede(Sede sede) { return filtrar(p -> sede.equals(p.getSede())); }
}
