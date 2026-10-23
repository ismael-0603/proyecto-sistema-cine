package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Calificacion {
    private int idCalificacion;
    private int puntuacion; // entre 1 y 5
    private String comentario;
    private DateTime fecha;
    private Compra compra;
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

    private static boolean esPuntuacionValida(int p) {
        return p >= 1 && p <= 5;
    }

    public boolean validarPuntuacion() {
        return esPuntuacionValida(this.puntuacion);
    }

    // Solo se califica una compra CONFIRMADA, y una sola vez
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