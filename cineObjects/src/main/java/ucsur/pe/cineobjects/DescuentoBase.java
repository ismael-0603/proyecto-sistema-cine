package ucsur.pe.cineobjects;

import ucsur.pe.cineobjects.cineInterfaz.Descuento;

/**
 * Base común de los descuentos comerciales (cupones y promociones).
 * Centraliza tipo, valor, periodo y estado; cada subclase define cuándo es aplicable a una compra.
 */
public abstract class DescuentoBase implements Descuento {
    public static final String PORCENTAJE = "PORCENTAJE";
    public static final String MONTO_FIJO = "MONTO_FIJO";

    protected final String tipoDescuento;
    protected final double valor;
    protected final DateTime fechaInicio; // solo fecha: DateTime(dia, mes, anio)
    protected final DateTime fechaFin;    // solo fecha: DateTime(dia, mes, anio)
    protected boolean estado;

    protected DescuentoBase(String tipoDescuento, double valor, DateTime fechaInicio,
                            DateTime fechaFin, boolean estado) {
        if (!PORCENTAJE.equals(tipoDescuento) && !MONTO_FIJO.equals(tipoDescuento)) {
            throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        }
        if (valor < 0) {
            throw new IllegalArgumentException("El valor del descuento no puede ser negativo");
        }
        if (PORCENTAJE.equals(tipoDescuento) && valor > 100) {
            throw new IllegalArgumentException("Un porcentaje no puede superar 100");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (fechaFin.esAntesDe(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser menor a la fecha inicio");
        }
        this.tipoDescuento = tipoDescuento;
        this.valor = valor;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    /** Comprueba que el descuento esté habilitado y que la fecha esté dentro de su periodo. */
    @Override
    public boolean validarVigencia(DateTime fecha) {
        return estado && fecha != null && fecha.estaEnRangoFecha(fechaInicio, fechaFin);
    }

    /** Calcula el descuento; el monto fijo se limita al total y un descuento inactivo no descuenta. */
    @Override
    public double calcularDescuento(double total) {
        if (!estado || total <= 0) return 0.0;
        if (PORCENTAJE.equals(tipoDescuento)) {
            return total * (valor / 100.0);
        }
        return Math.min(total, valor); // MONTO_FIJO
    }

    /** Cada tipo de descuento define sus propias condiciones para aplicarse a una compra. */
    public abstract boolean esAplicable(Compra compra);

    @Override
    public void activar() { this.estado = true; }

    @Override
    public void desactivar() { this.estado = false; }

    public String getTipoDescuento() { return tipoDescuento; }
    public DateTime getFechaInicio() { return fechaInicio; }
    public DateTime getFechaFin() { return fechaFin; }
    public boolean isEstado() { return estado; }

    /** Texto del valor, como "10%" o "S/ 5.00". */
    protected String describirValor() {
        return PORCENTAJE.equals(tipoDescuento)
                ? String.format("%.0f%%", valor) : "S/ " + String.format("%.2f", valor);
    }
}
