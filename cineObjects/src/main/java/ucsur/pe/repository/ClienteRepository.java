package ucsur.pe.repository;

import ucsur.pe.model.Cliente;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Cliente: define QUÉ se puede guardar y buscar, no CÓMO.
 * Lo cumple ClienteJdbcRepository (MySQL).
 * Las búsquedas que solo filtran listarTodos() están aquí como métodos default, para no repetirlas en cada implementación.
 */
public interface ClienteRepository {
    void guardar(Cliente cliente);
    Optional<Cliente> buscarPorId(int idCliente);
    List<Cliente> listarTodos();

    /**
     * Busca una cuenta registrada (no invitada) por su correo, sin distinguir mayúsculas ni espacios alrededor.
     * En la BD el correo es único solo para registrados: un invitado puede repetirlo en varias compras.
     */
    default Optional<Cliente> buscarPorCorreo(String correo) {
        if (correo == null) return Optional.empty();
        String buscado = correo.trim();
        return listarTodos().stream()
                .filter(c -> !c.esInvitado() && c.getCorreo() != null && c.getCorreo().equalsIgnoreCase(buscado))
                .findFirst();
    }

    /** Busca una cuenta registrada por su DNI (los invitados no tienen DNI). */
    default Optional<Cliente> buscarPorDni(String dni) {
        if (dni == null) return Optional.empty();
        String buscado = dni.trim();
        return listarTodos().stream().filter(c -> buscado.equals(c.getDni())).findFirst();
    }
}
