package ucsur.pe.repository;

import ucsur.pe.db.PersonaDao;
import ucsur.pe.model.Cliente;
import java.util.List;
import java.util.Optional;

/**
 * Implementa ClienteRepository con MySQL. Carga los clientes una vez al crearse (un solo objeto por id).
 * Todo cliente nuevo (socio o invitado) llega con id 0 y la base le genera uno (AUTO_INCREMENT).
 */
public class ClienteJdbcRepository extends RepositorioJdbc<Cliente> implements ClienteRepository {
    private final PersonaDao dao;

    public ClienteJdbcRepository(PersonaDao dao) {
        super("el cliente", Cliente::getIdCliente);
        this.dao = dao;
        cargar(dao.listarClientes());
    }

    @Override public void guardar(Cliente cliente) { guardarCon(cliente, dao::insertarCliente, dao::actualizarCliente); }

    @Override public List<Cliente> listarTodos() { return todos(); }

    @Override public Optional<Cliente> buscarPorId(int idCliente) { return porId(idCliente); }
}
