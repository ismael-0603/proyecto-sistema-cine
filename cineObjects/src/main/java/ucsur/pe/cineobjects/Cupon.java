package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Cupon {
    public static final String PORCENTAJE = "PORCENTAJE";
    public static final String MONTO_FIJO = "MONTO_FIJO";

    private int idCupon;
    private String codigo;
    private String tipoDescuento;
    private double descuento;
    private DateTime fechaInicio; // solo fecha: DateTime(dia, mes, anio)
    private DateTime fechaFin;    // solo fecha: DateTime(dia, mes, anio)
    private int limiteUso;
    private int usosRealizados;
    private boolean estado;

    public Cupon(int idCupon, String codigo, String tipoDescuento, double descuento,
                 DateTime fechaInicio, DateTime fechaFin, int limiteUso, boolean estado) {
        if (codigo == null || codigo.trim().isEmpty() || codigo.trim().length() > 30) {
            throw new IllegalArgumentException("El código es obligatorio (máx. 30 caracteres)");
        }
        if (!PORCENTAJE.equals(tipoDescuento) && !MONTO_FIJO.equals(tipoDescuento)) {
            throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        }
        if (descuento < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        if (PORCENTAJE.equals(tipoDescuento) && descuento > 100) {
            throw new IllegalArgumentException("Un porcentaje no puede superar 100");
        }
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (fechaFin.esAntesDe(fechaInicio)) {
            throw new IllegalArgumentException("La fecha fin no puede ser menor a la fecha inicio");
        }
        if (limiteUso < 0) {
            throw new IllegalArgumentException("El límite de uso no puede ser negativo");
        }
        this.idCupon = idCupon;
        this.codigo = codigo.trim();
        this.tipoDescuento = tipoDescuento;
        this.descuento = descuento;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.limiteUso = limiteUso;
        this.usosRealizados = 0;
        this.estado = estado;
    }

    public boolean estaActivo() {
        return this.estado;
    }

    // Activo, con usos disponibles y dentro del rango de fechas
    public boolean validarVigencia(DateTime fecha) {
        return estaActivo() && usosRealizados < limiteUso
                && fecha != null && fecha.estaEnRangoFecha(fechaInicio, fechaFin);
    }

    // El descuento nunca supera el total
    public double calcularDescuento(double total) {
        if (!estaActivo() || total <= 0) return 0.0;
        if (PORCENTAJE.equals(tipoDescuento)) {
            return total * (descuento / 100.0);
        }
        return Math.min(total, descuento); // MONTO_FIJO
    }

    public void registrarUso() {
        if (usosRealizados >= limiteUso) {
            throw new IllegalStateException("El cupón " + codigo + " alcanzó su límite de uso");
        }
        usosRealizados++;
    }

    public void activar() { this.estado = true; }
    public void desactivar() { this.estado = false; }
    public void setEstado(boolean estado) { this.estado = estado; }

    public int getIdCupon() { return idCupon; }
    public String getCodigo() { return codigo; }
    public String getTipoDescuento() { return tipoDescuento; }
    public double getDescuento() { return descuento; }
    public DateTime getFechaInicio() { return fechaInicio; }
    public DateTime getFechaFin() { return fechaFin; }
    public int getLimiteUso() { return limiteUso; }
    public int getUsosRealizados() { return usosRealizados; }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        String desc = PORCENTAJE.equals(tipoDescuento)
                ? String.format("%.0f%%", descuento) : "S/ " + String.format("%.2f", descuento);
        return codigo + " (" + desc + ")";
    }
}
