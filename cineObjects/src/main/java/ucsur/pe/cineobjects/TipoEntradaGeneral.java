package ucsur.pe.cineobjects;

/** Tarifa de entrada general; otorga 2 punto(s) por entrada. */
public class TipoEntradaGeneral extends TipoEntrada {
    public static final int PUNTOS = 2; // puntos base que gana el cliente por entrada

    public TipoEntradaGeneral(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado);
    }

    public TipoEntradaGeneral(int idTipoEntrada, String nombre, String descripcion,
                            double tarifa, boolean requiereSocio, boolean estado, int puntosCanje) {
        super(idTipoEntrada, nombre, descripcion, tarifa, requiereSocio, estado, puntosCanje);
    }

    @Override
    protected int puntosBase() {
        return PUNTOS;
    }
}
