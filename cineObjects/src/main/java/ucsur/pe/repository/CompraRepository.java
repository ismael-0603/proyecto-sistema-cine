package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Compra: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumplen CompraMemoriaRepository (demos en memoria) y CompraJdbcRepository (MySQL).
 * Las búsquedas que solo filtran listarTodas() están aquí como métodos default, para no repetirlas en cada implementación.
 */
public interface CompraRepository {
    /** Guarda la compra; al guardar una compra CONFIRMADA sin serie le asigna la siguiente (como trg_compra_serie). */
    void guardar(Compra compra);
    Optional<Compra> buscarPorId(int idCompra);
    List<Compra> listarTodas();

    /** Busca por la serie del comprobante (B001-000123), sin distinguir mayúsculas (RF-33). */
    default Optional<Compra> buscarPorSerie(String serieNumero) {
        if (serieNumero == null) return Optional.empty();
        String buscada = serieNumero.trim();
        return listarTodas().stream()
                .filter(c -> c.getSerieNumero() != null && c.getSerieNumero().equalsIgnoreCase(buscada))
                .findFirst();
    }

    /** Compras de un correo, sea de socio o de invitado (RF-33). */
    default List<Compra> listarPorCorreo(String correo) {
        if (correo == null) return List.of();
        String buscado = correo.trim();
        return listarTodas().stream()
                .filter(c -> c.getCliente().getCorreo() != null && c.getCliente().getCorreo().equalsIgnoreCase(buscado))
                .toList();
    }

    /** Compras con al menos una entrada de la función (para reembolsar si el cine la cancela, RF-24). */
    default List<Compra> listarPorFuncion(Funcion funcion) {
        return listarTodas().stream()
                .filter(c -> c.getEntradas().stream().anyMatch(e -> e.getFuncion() == funcion))
                .toList();
    }
}
