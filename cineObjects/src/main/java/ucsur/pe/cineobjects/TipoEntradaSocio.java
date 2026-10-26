package ucsur.pe.cineobjects;

/** Tarifa de entrada para socios; otorga 3 punto(s) por entrada. */
public class TipoEntradaSocio extends TipoEntrada {
    public static final int PUNTOS = 3;

    public TipoEntradaSocio(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado);
    }

    public TipoEntradaSocio(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado, int puntosCanje) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
    }

    @Override
    protected int puntosBase() {
        return PUNTOS;
    }
}
