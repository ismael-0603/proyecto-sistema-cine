package ucsur.pe.repository;

import ucsur.pe.model.Cliente;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de ClienteRepository (una lista hace de tabla). */
public class ClienteMemoriaRepository extends RepositorioMemoria<Cliente> implements ClienteRepository {
    @Override public void guardar(Cliente cliente) { agregar(cliente); }
    @Override public Optional<Cliente> buscarPorCorreo(String correo) {
        return buscarPrimero(c -> !c.esInvitado() && c.getCorreo() != null && c.getCorreo().equalsIgnoreCase(correo));
    }
    @Override public List<Cliente> listarTodos() { return todos(); }
    @Override public int siguienteId() { return generarId(); }
}
