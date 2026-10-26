package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: es la lista de cupones del cine, para buscar uno por el código que escribe el cliente.
 */
public class CatalogoCupones {
    private final List<Cupon> cupones = new ArrayList<>();

    /** Agrega el cupón; rechaza un código que ya pertenece a otro cupón. */
    public void registrar(Cupon cupon) {
        if (cupon == null) {
            throw new IllegalArgumentException("El cupón no puede ser nulo");
        }
        Cupon existente = buscarPorCodigo(cupon.getCodigo());
        if (existente == cupon) return; // ya está registrado
        if (existente != null) {
            throw new IllegalArgumentException("Ya existe un cupón con el código " + cupon.getCodigo());
        }
        cupones.add(cupon);
    }

    /** Devuelve el cupón con ese código o null si no existe. */
    public Cupon buscarPorCodigo(String codigo) {
        if (codigo == null) return null;
        for (Cupon c : cupones) {
            if (c.getCodigo().equalsIgnoreCase(codigo.trim())) {
                return c;
            }
        }
        return null;
    }

    public List<Cupon> getCupones() { return new ArrayList<>(cupones); }
}
