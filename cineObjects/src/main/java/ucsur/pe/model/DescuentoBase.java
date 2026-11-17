package ucsur.pe.model;

import ucsur.pe.tipo.CalculoDescuento;

/**
 * Por qué existe: junta lo común de Cupon y Promocion (forma de cálculo, fechas y si está activo)
 * para no repetirlo. Cada hija decide cuándo se aplica a una compra; el cálculo lo hace su CalculoDescuento.
 */
public abstract class DescuentoBase implements Descuento, Activable {
    private CalculoDescuento calculo; // porcentaje, monto fijo...: cada forma calcula a su manera
    private DateTime fechaInicio; // solo fecha: DateTime(dia, mes, anio)
    private DateTime fechaFin;    // solo fecha: DateTime(dia, mes, anio)
    private boolean estado; // habilitado o no; es independiente de las fechas

    protected DescuentoBase(CalculoDescuento calculo, DateTime fechaInicio, DateTime fechaFin, boolean estado) {
        cambiarCalculoYVigencia(calculo, fechaInicio, fechaFin);
        this.estado = estado;
    }

    /** Lo usan las hijas al editarse: valida y cambia la forma de cálculo y las fechas. */
    protected final void cambiarCalculoYVigencia(CalculoDescuento calculo, DateTime fechaInicio, DateTime fechaFin) {
        if (calculo == null) {
            throw new IllegalArgumentException("La forma de calcular el descuento es obligatoria");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (fechaFin.esAntesDe(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser menor a la fecha inicio");
        }
        this.calculo = calculo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    /** Comprueba que el descuento esté habilitado y que la fecha esté dentro de su periodo. */
    @Override
    public boolean validarVigencia(DateTime fecha) {
        return estado && fecha != null && fecha.estaEnRangoFecha(fechaInicio, fechaFin);
    }

    /** Un descuento inactivo no descuenta; si está activo, su forma de cálculo decide cuánto. */
    @Override
    public long calcularDescuentoCentavos(long totalCentavos) {
        if (!estado) return 0;
        return calculo.calcularCentavos(totalCentavos);
    }

    // esAplicable y getOrdenAplicacion vienen de la interfaz Descuento y quedan abstractos:
    // cada hija (Cupon, Promocion) los define a su manera.

    /** Habilita el descuento. */
    @Override
    public void activar() { this.estado = true; }

    /** Deshabilita el descuento. */
    @Override
    public void desactivar() { this.estado = false; }

    public CalculoDescuento getCalculo() { return calculo; }
    /** Texto para la columna tipo_descuento de la BD, como "PORCENTAJE". */
    public String getTipoDescuento() { return calculo.getTipo(); }
    /** Porcentaje (10 = 10%) o monto en céntimos (500 = S/ 5.00), según la forma de cálculo. */
    public long getValor() { return calculo.getValor(); }
    public DateTime getFechaInicio() { return fechaInicio; }
    public DateTime getFechaFin() { return fechaFin; }
    @Override
    public boolean isEstado() { return estado; }

    /** Texto del valor, como "10%" o "S/ 5.00". */
    protected String describirValor() {
        return calculo.describir();
    }
}
