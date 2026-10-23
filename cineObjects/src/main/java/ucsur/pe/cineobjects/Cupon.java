package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Cupon {
    private int idCupon;
    private String codigo;
    private double descuento;   //monto o porcentaje a descontar
    private int limiteUso; // cant max de usos 
    private int usosRealizados;
    private String estado; // Vigente, Agotado, Vencido, etc

    public Cupon(int idCupon, String codigo, double descuento, int limiteUso, int usosRealizados, String estado) {
        this.idCupon = idCupon;
        this.codigo = codigo;
        this.descuento = descuento;
        this.limiteUso = limiteUso;
        this.usosRealizados = usosRealizados;
        this.estado = estado;
    }
    
    public int getIdCupon() {
        return idCupon;
    }

    public void setIdCupon(int idCupon) {
        this.idCupon = idCupon;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public double getDescuento() {
        return descuento;
    }

    public void setDescuento(double descuento) {
        this.descuento = descuento;
    }

    public int getLimiteUso() {
        return limiteUso;
    }

    public void setLimiteUso(int limiteUso) {
        this.limiteUso = limiteUso;
    }

    public int getUsosRealizados() {
        return usosRealizados;
    }

    public void setUsosRealizados(int usosRealizados) {
        this.usosRealizados = usosRealizados;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
    
}
