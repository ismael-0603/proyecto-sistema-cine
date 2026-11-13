package ucsur.pe.repository;

import ucsur.pe.model.Cliente;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia de Cliente: define QUÉ se puede guardar y buscar, no CÓMO.
 * Hoy lo cumple ClienteMemoriaRepository; con la base de datos lo cumplirá una versión Jdbc.
 */
public interface ClienteRepository {
    void guardar(Cliente cliente);
    /** Busca una cuenta registrada (no invitada) por su correo; en la BD el correo es único solo para registrados. */
    Optional<Cliente> buscarPorCorreo(String correo);
    List<Cliente> listarTodos();
    int siguienteId();
}
