package ucsur.pe.cineobjects;

/** Tarifa de entrada reducida; otorga 1 punto(s) por entrada. */
public class TipoEntradaReducida extends TipoEntrada {
    public static final int PUNTOS = 1; // puntos base que gana el cliente por entrada

    public TipoEntradaReducida(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado);
    }

    public TipoEntradaReducida(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado, int puntosCanje) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
    }

    @Override
    protected int puntosBase() {
        return PUNTOS;
    }
}
