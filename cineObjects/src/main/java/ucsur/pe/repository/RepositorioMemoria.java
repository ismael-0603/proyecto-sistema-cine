package ucsur.pe.repository;

import ucsur.pe.exception.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ObjIntConsumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Por qué existe: junta lo común de los repositorios que guardan en memoria (una lista que hace de tabla
 * y un contador que simula el AUTO_INCREMENT de MySQL). Igual que en la BD, un objeto nuevo llega con id 0
 * y al guardarlo recibe el siguiente id con su método asignarId. Los servicios no notan la diferencia
 * con la versión Jdbc, porque solo conocen las interfaces.
 */
abstract class RepositorioMemoria<T> {
    private final List<T> datos = new ArrayList<>();
    private final ToIntFunction<T> id;
    private final ObjIntConsumer<T> asignarId;
    private int ultimoId = 0;

    /**
     * @param id        cómo leer el id del objeto (por ejemplo Pelicula::getIdPelicula)
     * @param asignarId cómo ponerle el id generado (por ejemplo Pelicula::asignarId)
     */
    protected RepositorioMemoria(ToIntFunction<T> id, ObjIntConsumer<T> asignarId) {
        this.id = id;
        this.asignarId = asignarId;
    }

    /**
     * Guarda el objeto; si ya estaba guardado, no lo repite (equivale a un UPDATE).
     * Si es nuevo y tiene id 0, recibe el siguiente id (simula el AUTO_INCREMENT).
     * Las demos pueden cargar datos de prueba con id puesto a mano: el contador sigue desde el mayor.
     */
    protected void agregar(T objeto) {
        if (objeto == null) {
            throw new RepositoryException("No se puede guardar un valor nulo");
        }
        if (datos.contains(objeto)) {
            return;
        }
        int clave = id.applyAsInt(objeto);
        if (clave <= 0) {
            asignarId.accept(objeto, ++ultimoId);
        } else {
            ultimoId = Math.max(ultimoId, clave);
        }
        datos.add(objeto);
    }

    protected Optional<T> buscarPrimero(Predicate<T> condicion) {
        return datos.stream().filter(condicion).findFirst();
    }

    protected Optional<T> porId(int clave) {
        return buscarPrimero(o -> id.applyAsInt(o) == clave);
    }

    protected List<T> filtrar(Predicate<T> condicion) {
        return datos.stream().filter(condicion).toList();
    }

    protected List<T> todos() {
        return List.copyOf(datos);
    }
}
