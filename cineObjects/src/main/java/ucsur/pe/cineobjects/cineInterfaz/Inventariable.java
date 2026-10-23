package ucsur.pe.cineobjects.cineInterfaz;

/**
 *
 * @author user
 */
public interface Inventariable {
    boolean verificarStock(int cant);
    void descontarStock(int cant);
    void reponerStock(int cant);
}