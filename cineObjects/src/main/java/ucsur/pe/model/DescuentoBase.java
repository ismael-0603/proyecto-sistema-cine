package ucsur.pe.model;

import ucsur.pe.tipo.CalculoDescuento;

/**
 * Por qué existe: junta lo común de Cupon y Promocion (forma de cálculo, fechas y si está activo)
 * para no repetirlo. Cada hija decide cuándo se aplica a una compra; el cálculo lo hace su CalculoDescuento.
 */
public abstract class DescuentoBase implements Descuento, Activable {
    private final CalculoDescuento calculo; // porcentaje, monto fijo...: cada forma calcula a su manera
    private final DateTime fechaInicio; // solo fecha: DateTime(dia, mes, anio)
    private final DateTime fechaFin;    // solo fecha: DateTime(dia, mes, anio)
    private boolean estado; // habilitado o no; es independiente de las fechas

    protected DescuentoBase(CalculoDescuento calculo, DateTime fechaInicio, DateTime fechaFin, boolean estado) {
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
        this.estado = estado;
    }

    /** Comprueba que el descuento esté habilitado y que la fecha esté dentro de su periodo. */
    @Override
    public boolean validarVigencia(DateTime fecha) {
        return estado && fecha != null && fecha.estaEnRangoFecha(fechaInicio, fechaFin);
    }

    /** Un descuento inactivo no descuenta; si está activo, su forma de cálculo decide cuánto. */
    @Override
    public double calcularDescuento(double total) {
        if (!estado) return 0.0;
        return calculo.calcular(total);
    }

    // esAplicable y getOrdenAplicacion vienen de la interfaz Descuento y quedan abstractos:
    // cada hija (Cupon, Promocion) los define a su manera.

    /** Por defecto usar el descuento no cambia nada; Cupon lo redefine para contar sus usos. */
    @Override
    public void registrarUso() { }

    /** Por defecto no hay nada que deshacer; Cupon lo redefine para devolver el uso. */
    @Override
    public void revertirUso() { }

    /** Habilita el descuento. */
    @Override
    public void activar() { this.estado = true; }

    /** Deshabilita el descuento. */
    @Override
    public void desactivar() { this.estado = false; }

    public CalculoDescuento getCalculo() { return calculo; }
    /** Texto para la columna tipo_descuento de la BD, como "PORCENTAJE". */
    public String getTipoDescuento() { return calculo.getTipo(); }
    /** Porcentaje o monto en soles, según la forma de cálculo. */
    public double getValor() { return calculo.getValor(); }
    public DateTime getFechaInicio() { return fechaInicio; }
    public DateTime getFechaFin() { return fechaFin; }
    @Override
    public boolean isEstado() { return estado; }

    /** Texto del valor, como "10%" o "S/ 5.00". */
    protected String describirValor() {
        return calculo.describir();
    }
}
