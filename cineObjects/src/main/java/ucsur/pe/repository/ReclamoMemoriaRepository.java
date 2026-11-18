package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Reclamo;
import java.util.List;

/** Implementación en memoria de ReclamoRepository (una lista hace de tabla). */
public class ReclamoMemoriaRepository extends RepositorioMemoria<Reclamo> implements ReclamoRepository {
    public ReclamoMemoriaRepository() { super(Reclamo::getIdReclamo, Reclamo::asignarId); }

    @Override public void guardar(Reclamo reclamo) { agregar(reclamo); } // si es nuevo, recibe su id (AUTO_INCREMENT)
    @Override public List<Reclamo> listarPorCompra(Compra compra) { return filtrar(r -> r.getCompra() == compra); }
    @Override public List<Reclamo> listarTodos() { return todos(); }
}
