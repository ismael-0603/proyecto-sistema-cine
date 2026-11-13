package ucsur.pe.controller;

/**
 * Por qué existe: es lo que el controller devuelve a la vista: si la acción salió bien,
 * el mensaje para mostrar y el dato resultante (por ejemplo, la compra o la lista de funciones).
 */
public record Respuesta<T>(boolean ok, String mensaje, T dato) {

    public static <T> Respuesta<T> exito(String mensaje, T dato) {
        return new Respuesta<>(true, mensaje, dato);
    }

    public static <T> Respuesta<T> error(String mensaje) {
        return new Respuesta<>(false, mensaje, null);
    }
}
