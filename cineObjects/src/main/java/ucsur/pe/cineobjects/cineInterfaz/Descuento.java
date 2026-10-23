package ucsur.pe.cineobjects.cineInterfaz;

/**
 *
 * @author user
 */
import ucsur.pe.cineobjects.DateTime;

public interface Descuento {
    boolean validarVigencia(DateTime fecha);
    double calcularDescuento(double total);
    void activar();
    void desactivar();
}
