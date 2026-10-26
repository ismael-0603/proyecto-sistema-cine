package ucsur.pe.cineobjects;

/**
 * Por qué existe: arma el comprobante de una compra confirmada (serie, fecha e importes).
 * Si cambia el formato, solo se toca esta clase.
 */
public class Comprobante {
    private final String serieNumero;
    private final DateTime fecha;
    private final ResumenImporte importe;

    public Comprobante(String serieNumero, DateTime fecha, ResumenImporte importe) {
        if (serieNumero == null) {
            throw new IllegalStateException("La compra aún no tiene comprobante (la base asigna la serie al confirmarla)");
        }
        this.serieNumero = serieNumero;
        this.fecha = fecha;
        this.importe = importe;
    }

    /** Texto del comprobante en una línea. */
    public String generarTexto() {
        return "Comprobante " + serieNumero + " | Fecha: " + fecha + " | " + importe;
    }

    public String getSerieNumero() { return serieNumero; }
    public DateTime getFecha() { return fecha; }
    public ResumenImporte getImporte() { return importe; }

    @Override
    public String toString() {
        return generarTexto();
    }
}
