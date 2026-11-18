package ucsur.pe.model;

/**
 * Por qué existe: todas las tablas de DB_Cine generan su id con AUTO_INCREMENT. Un objeto nuevo nace con id 0
 * y, después del INSERT, la capa de base de datos le asigna el id generado con asignarId. Esta clase junta
 * la validación que comparten todos esos asignarId (que el id no esté ya puesto y que sea positivo),
 * para no repetirla en cada clase del modelo.
 */
final class Ids {

    private Ids() { } // solo tiene un método estático

    /**
     * Devuelve el id nuevo si se puede asignar.
     * @param actual el id que tiene hoy el objeto (0 si aún no se guardó)
     * @param nuevo  el id que generó la base
     * @param que    el objeto, para el mensaje: "la película", "el cupón"...
     */
    static int asignar(int actual, int nuevo, String que) {
        if (actual > 0) {
            throw new IllegalStateException("El id de " + que + " ya fue asignado");
        }
        if (nuevo <= 0) {
            throw new IllegalArgumentException("El id de " + que + " debe ser mayor a 0");
        }
        return nuevo;
    }
}
