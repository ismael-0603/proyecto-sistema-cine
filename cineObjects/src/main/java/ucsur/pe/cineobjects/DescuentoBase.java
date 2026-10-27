package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.CalculoDescuento;
import ucsur.pe.cineInterfaz.Descuento;

/**
 * Por qué existe: junta lo común de Cupon y Promocion (forma de cálculo, fechas y si está activo)
 * para no repetirlo. Cada hija decide cuándo se aplica a una compra; el cálculo lo hace su CalculoDescuento.
 */
public abstract class DescuentoBase implements Descuento {
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

    /**
     * Convierte lo guardado en la BD (columnas tipo_descuento y valor) en su forma de cálculo.
     * Es el único lugar que traduce ese texto; el resto del código trabaja con el objeto.
     */
    public static CalculoDescuento crearCalculo(String tipoDescuento, double valor) {
        return switch (tipoDescuento == null ? "" : tipoDescuento) {
            case CalculoPorcentaje.TIPO -> new CalculoPorcentaje(valor);
            case CalculoMontoFijo.TIPO -> new CalculoMontoFijo(valor);
            default -> throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        };
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
    public boolean isEstado() { return estado; }

    /** Texto del valor, como "10%" o "S/ 5.00". */
    protected String describirValor() {
        return calculo.describir();
    }
}
