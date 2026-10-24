package ucsur.pe.cineobjects;

/**
 * Guarda la puntuación y el comentario de una compra.
 * La puntuación debe estar entre 1 y 5, el comentario no puede superar 255 caracteres
 * y solo se registra cuando la compra está confirmada.
 */
public class Calificacion {
    private int idCalificacion;
    private int puntuacion; // Valor permitido: de 1 a 5.
    private String comentario;
    private DateTime fecha;
    private Compra compra;
    /** Evita registrar la misma calificación más de una vez. */
    private boolean registrada;

    public Calificacion(int idCalificacion, int puntuacion, String comentario, Compra compra) {
        if (!esPuntuacionValida(puntuacion)) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5");
        }
        if (comentario != null && comentario.length() > 255) {
            throw new IllegalArgumentException("El comentario no puede superar 255 caracteres");
        }
        if (compra == null) {
            throw new IllegalArgumentException("La compra es obligatoria");
        }
        this.idCalificacion = idCalificacion;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fecha = DateTime.ahora();
        this.compra = compra;
        this.registrada = false;
    }

    /** Comprueba el mismo rango usado al crear una calificación. */
    private static boolean esPuntuacionValida(int p) {
        return p >= 1 && p <= 5;
    }

    /** Permite revisar la puntuación antes de registrar la calificación. */
    public boolean validarPuntuacion() {
        return esPuntuacionValida(this.puntuacion);
    }

    /** Registra la calificación una sola vez y únicamente si la compra está confirmada. */
    public boolean registrar() {
        if (registrada) return false;
        if (!validarPuntuacion() || !Compra.CONFIRMADA.equals(compra.getEstado())) {
            return false;
        }
        this.fecha = DateTime.ahora();
        this.registrada = true;
        return true;
    }

    public int getIdCalificacion() { return idCalificacion; }
    public int getPuntuacion() { return puntuacion; }
    public String getComentario() { return comentario; }
    public DateTime getFecha() { return fecha; }
    public Compra getCompra() { return compra; }
    public boolean isRegistrada() { return registrada; }

    @Override
    public String toString() {
        return puntuacion + "/5" + (comentario != null && !comentario.isEmpty() ? " - " + comentario : "");
    }
}
