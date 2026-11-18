package ucsur.pe.repository;

import ucsur.pe.model.Cliente;
import java.util.List;
import java.util.Optional;

/** Implementación en memoria de ClienteRepository (una lista hace de tabla). */
public class ClienteMemoriaRepository extends RepositorioMemoria<Cliente> implements ClienteRepository {
    public ClienteMemoriaRepository() { super(Cliente::getIdCliente, Cliente::asignarId); }

    @Override public void guardar(Cliente cliente) { agregar(cliente); }
    @Override public Optional<Cliente> buscarPorId(int idCliente) { return porId(idCliente); }
    @Override public List<Cliente> listarTodos() { return todos(); }
}
