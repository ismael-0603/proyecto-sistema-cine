package ucsur.pe.repository;

import ucsur.pe.exception.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Por qué existe: junta lo común de los repositorios que guardan en memoria (una lista que hace de tabla
 * y un contador que simula el AUTO_INCREMENT). Cuando exista la BD, cada repositorio tendrá su versión
 * Jdbc y los servicios no cambiarán, porque solo conocen las interfaces.
 */
abstract class RepositorioMemoria<T> {
    private final List<T> datos = new ArrayList<>();
    private int ultimoId = 0;

    /** Guarda el objeto; si ya estaba guardado, no lo repite (equivale a un UPDATE). */
    protected void agregar(T objeto) {
        if (objeto == null) {
            throw new RepositoryException("No se puede guardar un valor nulo");
        }
        if (!datos.contains(objeto)) {
            datos.add(objeto);
        }
    }

    protected Optional<T> buscarPrimero(Predicate<T> condicion) {
        return datos.stream().filter(condicion).findFirst();
    }

    protected List<T> filtrar(Predicate<T> condicion) {
        return datos.stream().filter(condicion).toList();
    }

    protected List<T> todos() {
        return List.copyOf(datos);
    }

    /** Simula el AUTO_INCREMENT de la BD. */
    protected int generarId() {
        return ++ultimoId;
    }
}
