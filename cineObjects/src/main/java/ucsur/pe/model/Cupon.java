package ucsur.pe.model;

import ucsur.pe.tipo.CalculoDescuento;

/**
 * Por qué existe: es un código de descuento que escribe el cliente (por ejemplo CINE10),
 * con fechas de vigencia, un límite de usos y, si se quiere, un nivel de socio mínimo.
 */
public class Cupon extends DescuentoBase implements DescuentoConUsos {
    public static final String CATEGORIA = "CUPON";
    public static final int ORDEN_APLICACION = 2; // se aplica después de las promociones
    private int idCupon;
    private String codigo; // el cliente lo escribe para aplicar el cupón; no distingue mayúsculas
    /** Máximo de compras que pueden usar el cupón; cero significa que no se puede usar. */
    private int limiteUso;
    /** Cantidad de veces que ya se aplicó en compras confirmadas. */
    private int usosRealizados;

    public Cupon(int idCupon, String codigo, CalculoDescuento calculo,
                 DateTime fechaInicio, DateTime fechaFin, int limiteUso, int nivelSocioMinimo, boolean estado) {
        super(calculo, fechaInicio, fechaFin, nivelSocioMinimo, estado);
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

    /**
     * Solo para la capa de base de datos (ucsur.pe.db): reconstruye un cupón con los usos que ya tuvo
     * (cupon.usos_realizados), que el constructor siempre deja en cero.
     */
    public static Cupon restaurar(int idCupon, String codigo, CalculoDescuento calculo, DateTime fechaInicio,
                                  DateTime fechaFin, int limiteUso, int usosRealizados, int nivelSocioMinimo,
                                  boolean estado) {
        Cupon cupon = new Cupon(idCupon, codigo, calculo, fechaInicio, fechaFin, limiteUso, nivelSocioMinimo, estado);
        if (usosRealizados < 0 || usosRealizados > limiteUso) {
            throw new IllegalArgumentException("Los usos realizados deben estar entre 0 y " + limiteUso); // ck_cupon_usos
        }
        cupon.usosRealizados = usosRealizados;
        return cupon;
    }

    /**
     * El administrador edita el cupón (wireframe 19). El servicio comprueba que el código no lo use otro cupón;
     * el límite no puede quedar por debajo de los usos que ya tuvo.
     */
    public void actualizar(String codigo, CalculoDescuento calculo, DateTime fechaInicio, DateTime fechaFin,
                           int limiteUso, int nivelSocioMinimo) {
        if (codigo == null || codigo.trim().isEmpty() || codigo.trim().length() > 30) {
            throw new IllegalArgumentException("El código es obligatorio (máx. 30 caracteres)");
        }
        if (limiteUso < usosRealizados) {
            throw new IllegalArgumentException("El límite no puede ser menor a los " + usosRealizados + " usos ya realizados");
        }
        cambiarCalculoYVigencia(calculo, fechaInicio, fechaFin);
        cambiarNivelSocioMinimo(nivelSocioMinimo);
        this.codigo = codigo.trim();
        this.limiteUso = limiteUso;
    }

    /** Devuelve true si está activo, dentro de fechas y aún tiene usos disponibles. */
    @Override
    public boolean validarVigencia(DateTime fecha) {
        return super.validarVigencia(fecha) && usosRealizados < limiteUso;
    }

    /** Un cupón se aplica si está vigente en la fecha de la compra y el cliente tiene el nivel pedido. */
    @Override
    public boolean esAplicable(ContextoDescuento contexto) {
        return contexto != null && validarVigencia(contexto.fecha()) && cumpleNivelSocio(contexto);
    }

    /** El cupón va después de las promociones: descuenta sobre lo que ya quedó. */
    @Override
    public int getOrdenAplicacion() {
        return ORDEN_APLICACION;
    }

    /** Un cupón ocupa el lugar de cupón de la compra (columna id_cupon). */
    @Override
    public String getCategoria() {
        return CATEGORIA;
    }

    /** Suma un uso al cupón y falla si ya alcanzó el límite configurado. */
    @Override
    public void registrarUso() {
        if (usosRealizados >= limiteUso) {
            throw new IllegalStateException("El cupón " + codigo + " alcanzó su límite de uso");
        }
        usosRealizados++;
    }

    /** Devuelve un uso al cupón cuando se reembolsa la compra que lo usó. */
    @Override
    public void revertirUso() {
        if (usosRealizados > 0) {
            usosRealizados--;
        }
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; un cupón nuevo nace con id 0. */
    public void asignarId(int idCupon) {
        this.idCupon = Ids.asignar(this.idCupon, idCupon, "el cupón");
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
