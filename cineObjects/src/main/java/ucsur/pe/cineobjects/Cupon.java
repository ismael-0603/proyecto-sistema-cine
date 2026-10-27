package ucsur.pe.cineobjects;

import ucsur.pe.cineInterfaz.CalculoDescuento;

/**
 * Por qué existe: es un código de descuento que escribe el cliente (por ejemplo CINE10),
 * con fechas de vigencia y un límite de usos.
 */
public class Cupon extends DescuentoBase {
    public static final int ORDEN_APLICACION = 2; // se aplica después de las promociones
    private int idCupon;
    private String codigo; // el cliente lo escribe para aplicar el cupón; no distingue mayúsculas
    /** Máximo de compras que pueden usar el cupón; cero significa que no se puede usar. */
    private int limiteUso;
    /** Cantidad de veces que ya se aplicó en compras confirmadas. */
    private int usosRealizados;

    public Cupon(int idCupon, String codigo, CalculoDescuento calculo,
                 DateTime fechaInicio, DateTime fechaFin, int limiteUso, boolean estado) {
        super(calculo, fechaInicio, fechaFin, estado);
        if (codigo == null || codigo.trim().isEmpty() || codigo.trim().length() > 30) {
            throw new IllegalArgumentException("El código es obligatorio (máx. 30 caracteres)");
        }
        if (limiteUso < 0) {
            throw new IllegalArgumentException("El límite de uso no puede ser negativo");
        }
        this.idCupon = idCupon;
        this.codigo = codigo.trim();
        this.limiteUso = limiteUso;
        this.usosRealizados = 0;
    }

    /** Devuelve true si está activo, dentro de fechas y aún tiene usos disponibles. */
    @Override
    public boolean validarVigencia(DateTime fecha) {
        return super.validarVigencia(fecha) && usosRealizados < limiteUso;
    }

    /** Un cupón se aplica a una compra mientras esté vigente en la fecha de esa compra. */
    @Override
    public boolean esAplicable(Compra compra) {
        return compra != null && validarVigencia(compra.getFechaHora());
    }

    /** El cupón va después de las promociones: descuenta sobre lo que ya quedó. */
    @Override
    public int getOrdenAplicacion() {
        return ORDEN_APLICACION;
    }

    /** Suma un uso al cupón y falla si ya alcanzó el límite configurado. */
    @Override
    public void registrarUso() {
        if (usosRealizados >= limiteUso) {
            throw new IllegalStateException("El cupón " + codigo + " alcanzó su límite de uso");
        }
        usosRealizados++;
    }

    public int getIdCupon() { return idCupon; }
    public String getCodigo() { return codigo; }
    public int getLimiteUso() { return limiteUso; }
    public int getUsosRealizados() { return usosRealizados; }

    @Override
    public String toString() {
        return codigo + " (" + describirValor() + ")";
    }
}
