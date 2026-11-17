package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de CompraRepository (una lista hace de tabla). */
public class CompraMemoriaRepository extends RepositorioMemoria<Compra> implements CompraRepository {
    private int correlativoB001 = 0; // como la tabla correlativo de la BD

    @Override public void guardar(Compra compra) {
        agregar(compra);
        if (Compra.CONFIRMADA.equals(compra.getEstado()) && compra.getSerieNumero() == null) {
            compra.asignarSerie(String.format("B001-%06d", ++correlativoB001)); // simula el trigger trg_compra_serie
        }
    }
    @Override public Optional<Compra> buscarPorId(int idCompra) { return buscarPrimero(c -> c.getIdCompra() == idCompra); }
    @Override public List<Compra> listarTodas() { return todos(); }
    @Override public Optional<Compra> buscarPorSerie(String serie) {
        return serie == null ? Optional.empty()
                : buscarPrimero(c -> c.getSerieNumero() != null && c.getSerieNumero().equalsIgnoreCase(serie.trim()));
    }
    @Override public List<Compra> listarPorCorreo(String correo) {
        return correo == null ? List.of()
                : filtrar(c -> c.getCliente().getCorreo() != null && c.getCliente().getCorreo().equalsIgnoreCase(correo.trim()));
    }
    @Override public List<Compra> listarPorFuncion(Funcion funcion) {
        return filtrar(c -> c.getEntradas().stream().anyMatch(e -> e.getFuncion() == funcion));
    }
    @Override public int siguienteId() { return generarId(); }
}
