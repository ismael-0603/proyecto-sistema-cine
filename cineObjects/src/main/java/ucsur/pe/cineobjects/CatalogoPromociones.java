package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: es la lista de promociones del cine, para el caso de uso "Consultar promociones"
 * (RF-28): muestra al cliente las que puede aprovechar en una fecha. Es el equivalente de CatalogoCupones.
 */
public class CatalogoPromociones {
    private final List<Promocion> promociones = new ArrayList<>();

    /** Agrega la promoción al catálogo; si ya estaba, no la repite. */
    public void registrar(Promocion promocion) {
        if (promocion == null) {
            throw new IllegalArgumentException("La promoción no puede ser nula");
        }
        if (!promociones.contains(promocion)) {
            promociones.add(promocion);
        }
    }

    /** Promociones activas, dentro de sus fechas y válidas para el día de la semana de la fecha indicada. */
    public List<Promocion> consultarVigentes(DateTime fecha) {
        List<Promocion> vigentes = new ArrayList<>();
        if (fecha == null) return vigentes;
        for (Promocion p : promociones) {
            if (p.validarVigencia(fecha) && p.aplicaEnDia(fecha)) {
                vigentes.add(p);
            }
        }
        return vigentes;
    }

    /** Devuelve una copia de todas las promociones, incluidas las vencidas o inactivas. */
    public List<Promocion> getPromociones() { return new ArrayList<>(promociones); }
}
