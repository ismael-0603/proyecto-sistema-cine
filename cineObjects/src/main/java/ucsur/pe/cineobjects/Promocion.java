package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 * Define un descuento con vigencia, días permitidos y una regla opcional para clientes socios.
 * Valida el tipo y rango del descuento, el periodo y que cada día esté entre lunes (1) y domingo (7).
 */
public class Promocion extends DescuentoBase {
    private int idPromocion;
    private String nombre;
    private String descripcion;
    private boolean soloSocios;
    /** Días en que aplica: 1 es lunes y 7 domingo; una lista vacía permite cualquier día. */
    private List<Integer> diasAplicables;

    public Promocion(int idPromocion, String nombre, String descripcion, String tipoDescuento,
                     double valor, DateTime fechaInicio, DateTime fechaFin,
                     boolean soloSocios, List<Integer> diasAplicables, boolean estado) {
        super(tipoDescuento, valor, fechaInicio, fechaFin, estado);
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 40) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 40 caracteres)");
        }
        if (descripcion != null && descripcion.length() > 200) {
            throw new IllegalArgumentException("La descripción no puede superar 200 caracteres");
        }
        this.idPromocion = idPromocion;
        this.nombre = nombre.trim();
        this.descripcion = descripcion;
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
    }

    /** Si no se configuraron días, la promoción aplica cualquier día de la semana. */
    public boolean aplicaEnDia(DateTime fecha) {
        return diasAplicables.isEmpty() || diasAplicables.contains(fecha.diaDeLaSemana());
    }

    /** Comprueba estado, fechas, día de semana y, cuando corresponde, que el cliente sea socio. */
    @Override
    public boolean esAplicable(Compra compra) {
        if (compra == null || !validarVigencia(compra.getFechaHora())) return false;
        if (!aplicaEnDia(compra.getFechaHora())) return false;
        if (soloSocios) {
            return compra.getCliente().esSocio();
        }
        return true;
    }

    public int getIdPromocion() { return idPromocion; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getValor() { return valor; }
    public boolean isSoloSocios() { return soloSocios; }
    public List<Integer> getDiasAplicables() { return new ArrayList<>(diasAplicables); }

    @Override
    public String toString() {
        return nombre + " (" + describirValor() + ")";
    }
}
