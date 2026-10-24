package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 * Define un descuento con vigencia, días permitidos y una regla opcional para clientes socios.
 * Valida el tipo y rango del descuento, el periodo y que cada día esté entre lunes (1) y domingo (7).
 */
public class Promocion {
    public static final String PORCENTAJE = "PORCENTAJE";
    public static final String MONTO_FIJO = "MONTO_FIJO";

    private int idPromocion;
    private String nombre;
    private String descripcion;
    private String tipoDescuento;
    private double valor;
    private DateTime fechaInicio; // solo fecha: DateTime(dia, mes, anio)
    private DateTime fechaFin;    // solo fecha: DateTime(dia, mes, anio)
    private boolean soloSocios;
    /** Días en que aplica: 1 es lunes y 7 domingo; una lista vacía permite cualquier día. */
    private List<Integer> diasAplicables;
    private boolean estado;

    public Promocion(int idPromocion, String nombre, String descripcion, String tipoDescuento,
                     double valor, DateTime fechaInicio, DateTime fechaFin,
                     boolean soloSocios, List<Integer> diasAplicables, boolean estado) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 40) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 40 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres");
        }
        if (!PORCENTAJE.equals(tipoDescuento) && !MONTO_FIJO.equals(tipoDescuento)) {
            throw new IllegalArgumentException("Tipo de descuento inválido: " + tipoDescuento);
        }
        if (valor < 0) {
            throw new IllegalArgumentException("El valor no puede ser negativo");
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
        this.idPromocion = idPromocion;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
        this.tipoDescuento = tipoDescuento;
        this.valor = valor;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.soloSocios = soloSocios;
        this.diasAplicables = new ArrayList<>();
        if (diasAplicables != null) {
            for (Integer d : diasAplicables) {
                if (d == null || d < 1 || d > 7) {
                    throw new IllegalArgumentException("Los días deben estar entre 1 (lunes) y 7 (domingo)");
                }
                if (!this.diasAplicables.contains(d)) this.diasAplicables.add(d);
            }
        }
        this.estado = estado;
    }

    /** Comprueba que la promoción esté activa y que la fecha esté dentro del periodo definido. */
    public boolean validarVigencia(DateTime fecha) {
        return estado && fecha != null && fecha.estaEnRangoFecha(fechaInicio, fechaFin);
    }

    /** Si no se configuraron días, la promoción aplica cualquier día de la semana. */
    public boolean aplicaEnDia(DateTime fecha) {
        return diasAplicables.isEmpty() || diasAplicables.contains(fecha.diaDeLaSemana());
    }

    /** Comprueba estado, fechas, día de semana y, cuando corresponde, que el cliente sea socio. */
    public boolean esAplicable(Compra compra) {
        if (compra == null || !validarVigencia(compra.getFechaHora())) return false;
        if (!aplicaEnDia(compra.getFechaHora())) return false;
        if (soloSocios) {
            return compra.getCliente().esSocio();
        }
        return true;
    }

    /** Calcula el descuento y limita los montos fijos al total recibido. */
    public double calcularDescuento(double total) {
        if (!estado || total <= 0) return 0.0;
        if (PORCENTAJE.equals(tipoDescuento)) {
            return total * (valor / 100.0);
        }
        return Math.min(total, valor); // MONTO_FIJO
    }

    /** Habilita la promoción; las demás condiciones de vigencia se revisan al aplicarla. */
    public void activar() { this.estado = true; }
    /** Deshabilita la promoción para nuevas compras. */
    public void desactivar() { this.estado = false; }

    public int getIdPromocion() { return idPromocion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getTipoDescuento() { return tipoDescuento; }
    public double getValor() { return valor; }
    public DateTime getFechaInicio() { return fechaInicio; }
    public DateTime getFechaFin() { return fechaFin; }
    public boolean isSoloSocios() { return soloSocios; }
    public List<Integer> getDiasAplicables() { return new ArrayList<>(diasAplicables); }
    public boolean isEstado() { return estado; }

    @Override
    public String toString() {
        String desc = PORCENTAJE.equals(tipoDescuento)
                ? String.format("%.0f%%", valor) : "S/ " + String.format("%.2f", valor);
        return nombre + " (" + desc + ")";
    }
}
