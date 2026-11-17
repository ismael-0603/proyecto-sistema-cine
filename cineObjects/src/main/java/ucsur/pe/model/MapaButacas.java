package ucsur.pe.model;

import java.util.List;

/**
 * Por qué existe: es lo que muestra la pantalla «1. Asientos»: todas las butacas de la sala (para dibujar
 * filas, bloques y pasillos), cuáles están libres y la sugerencia del sistema ya marcada (RF-07, RF-29).
 * Si no se pudo sugerir (no alcanzan las butacas libres), {@code sugeridas} está vacía y {@code aviso} dice por qué.
 */
public record MapaButacas(List<Butaca> todas, List<Butaca> libres, List<Butaca> sugeridas, String aviso) {

    public MapaButacas {
        todas = List.copyOf(todas);
        libres = List.copyOf(libres);
        sugeridas = List.copyOf(sugeridas);
    }

    /** Indica si la butaca está libre en este momento. */
    public boolean estaLibre(Butaca butaca) {
        return libres.contains(butaca);
    }

    /** Indica si la butaca forma parte de la sugerencia. */
    public boolean esSugerida(Butaca butaca) {
        return sugeridas.contains(butaca);
    }
}
