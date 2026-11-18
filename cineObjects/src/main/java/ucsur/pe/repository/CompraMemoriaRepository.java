package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de CompraRepository (una lista hace de tabla). */
public class CompraMemoriaRepository extends RepositorioMemoria<Compra> implements CompraRepository {
    public CompraMemoriaRepository() { super(Compra::getIdCompra, Compra::asignarId); }

    private int correlativoB001 = 0; // como la tabla correlativo de la BD

    @Override public void guardar(Compra compra) {
        agregar(compra);
        if (Compra.CONFIRMADA.equals(compra.getEstado()) && compra.getSerieNumero() == null) {
            compra.asignarSerie(String.format("B001-%06d", ++correlativoB001)); // simula el trigger trg_compra_serie
        }
    }
    @Override public Optional<Compra> buscarPorId(int idCompra) { return porId(idCompra); }
    @Override public List<Compra> listarTodas() { return todos(); }
}
