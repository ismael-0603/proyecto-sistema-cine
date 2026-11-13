package ucsur.pe.controller;

import ucsur.pe.exception.RepositoryException;
import java.util.function.Supplier;

/**
 * Por qué existe: un solo try/catch para todas las acciones, como en el controller del ejemplo:
 * si el modelo o el servicio rechazan algo, la vista recibe el mensaje en vez de una excepción.
 * El controller no valida reglas ni guarda datos; solo recibe la acción y llama al servicio.
 */
abstract class ControllerBase {

    /** Ejecuta una acción que devuelve un dato. */
    protected <T> Respuesta<T> ejecutar(String mensajeExito, Supplier<T> accion) {
        try {
            return Respuesta.exito(mensajeExito, accion.get());
        } catch (IllegalArgumentException | IllegalStateException | RepositoryException e) {
            return Respuesta.error(e.getMessage());
        }
    }

    /** Ejecuta una acción que no devuelve nada. */
    protected Respuesta<Void> ejecutar(String mensajeExito, Runnable accion) {
        return ejecutar(mensajeExito, () -> {
            accion.run();
            return null;
        });
    }
}
