package ucsur.pe.repository;

import ucsur.pe.model.Producto;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de ProductoRepository (una lista hace de tabla). */
public class ProductoMemoriaRepository extends RepositorioMemoria<Producto> implements ProductoRepository {
    public ProductoMemoriaRepository() { super(Producto::getIdProducto, Producto::asignarId); }

    @Override public void guardar(Producto producto) { agregar(producto); }
    @Override public Optional<Producto> buscarPorId(int idProducto) { return porId(idProducto); }
    @Override public List<Producto> listarTodos() { return todos(); }
}
