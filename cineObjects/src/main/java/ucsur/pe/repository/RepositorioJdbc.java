package ucsur.pe.repository;

import ucsur.pe.exception.RepositoryException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Por qué existe: junta lo común de los repositorios que guardan en MySQL. Es un "mapa de identidad":
 * cada id de la BD corresponde a UN solo objeto Java. Hace falta porque el modelo y los servicios comparan
 * objetos con == (por ejemplo, compra.getCliente() != cliente o butaca.getSala() != sala): si la BD
 * devolviera dos objetos para la misma fila, esas reglas fallarían.
 * Las lecturas salen del mapa (cargado al arrancar) y las escrituras van a la BD por el DAO.
 */
abstract class RepositorioJdbc<T> {
    private final Map<Integer, T> cargados = new LinkedHashMap<>();
    private final ToIntFunction<T> id;
    private final String nombre; // para los mensajes: "la película", "el cupón"...

    protected RepositorioJdbc(String nombre, ToIntFunction<T> id) {
        this.nombre = nombre;
        this.id = id;
    }

    /**
     * Arma un mapa id -> objeto (en el orden de la lista). Los DAO lo piden al leer una tabla que apunta a otra
     * (por ejemplo, empleado.id_rol): con el mapa, la fila se une al objeto ya cargado en vez de crear otro.
     */
    protected static <E> Map<Integer, E> porIdDe(Collection<E> objetos, ToIntFunction<E> id) {
        Map<Integer, E> mapa = new LinkedHashMap<>();
        for (E objeto : objetos) {
            mapa.put(id.applyAsInt(objeto), objeto);
        }
        return mapa;
    }

    /** Registra en el mapa los objetos leídos de la BD. */
    protected void cargar(Collection<T> objetos) {
        for (T objeto : objetos) {
            cargados.put(id.applyAsInt(objeto), objeto);
        }
    }

    /**
     * Un objeto nuevo (id 0) se inserta: la base genera su id (AUTO_INCREMENT) y el DAO se lo asigna con
     * asignarId; luego entra al mapa. Si es el mismo objeto que ya está en el mapa, se actualiza.
     * Rechaza otro objeto con un id ya usado (habría dos objetos para la misma fila) y un id puesto a mano
     * que no viene de la base.
     */
    protected void guardarCon(T objeto, Consumer<T> insertar, Consumer<T> actualizar) {
        if (objeto == null) {
            throw new RepositoryException("No se puede guardar un valor nulo");
        }
        int clave = id.applyAsInt(objeto);
        if (clave <= 0) {
            insertar.accept(objeto);
            registrarNuevo(objeto);
            return;
        }
        T existente = cargados.get(clave);
        if (existente == objeto) {
            actualizar.accept(objeto);
        } else if (existente != null) {
            throw new RepositoryException("Ya existe " + nombre + " con id " + clave);
        } else {
            throw new RepositoryException("El id de " + nombre + " lo genera la base: créelo con id 0 (llegó con " + clave + ")");
        }
    }

    /** Registra en el mapa un objeto que se acaba de insertar (el DAO ya le asignó el id generado por la base). */
    private void registrarNuevo(T objeto) {
        int clave = id.applyAsInt(objeto);
        if (clave <= 0 || cargados.containsKey(clave)) {
            throw new RepositoryException("La base no asignó un id válido a " + nombre);
        }
        cargados.put(clave, objeto);
    }

    protected Optional<T> porId(int clave) {
        return Optional.ofNullable(cargados.get(clave));
    }

    protected List<T> filtrar(Predicate<T> condicion) {
        return cargados.values().stream().filter(condicion).toList();
    }

    protected List<T> todos() {
        return List.copyOf(cargados.values());
    }
}
