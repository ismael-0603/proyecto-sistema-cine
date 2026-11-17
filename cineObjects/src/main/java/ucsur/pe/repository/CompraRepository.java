package ucsur.pe.repository;

import ucsur.pe.model.Compra;
import ucsur.pe.model.Funcion;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Compra: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple CompraMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface CompraRepository {
    /** Guarda la compra; al guardar una compra CONFIRMADA sin serie le asigna la siguiente (como trg_compra_serie). */
    void guardar(Compra compra);
    Optional<Compra> buscarPorId(int idCompra);
    List<Compra> listarTodas();
    /** Busca por la serie del comprobante (B001-000123), sin distinguir mayúsculas (RF-33). */
    Optional<Compra> buscarPorSerie(String serieNumero);
    /** Compras de un correo, sea de socio o de invitado (RF-33). */
    List<Compra> listarPorCorreo(String correo);
    /** Compras con al menos una entrada de la función (para reembolsar si el cine la cancela, RF-24). */
    List<Compra> listarPorFuncion(Funcion funcion);
    int siguienteId();
}
