package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author user
 */
public class Cliente extends Persona{
    private int idCliente;
    private DateTime fechaRegistro;
    private int puntos = 0;
    private int nivelSocio; // Ejemplos: 0=Plata - 1=Oro - 2=Diamante... algo asi:v
    private boolean esInvitado;
    private List<Compra> historialCompras;
    
    public Cliente(){
    }

    public Cliente(int idCliente, DateTime fechaRegistro, int nivelSocio, boolean esInvitado, List historialCompras, String dni, String nombres, String apellidos, DateTime fechaNac, String correo, String telefono, String contra, boolean estado) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra, estado);
        this.idCliente = idCliente;
        this.fechaRegistro = fechaRegistro;
        this.puntos = 0;
        this.nivelSocio = nivelSocio;
        this.esInvitado = esInvitado;
        this.historialCompras = new ArrayList<>();
    }

    public Cliente(String dni, String nombres, String apellidos, DateTime fechaNac, String correo, String telefono, String contra, boolean estado) {
        super(dni, nombres, apellidos, fechaNac, correo, telefono, contra, estado);
    }
    
    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public DateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(DateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public int getNivelSocio() {
        return nivelSocio;
    }

    public void setNivelSocio(int nivelSocio) {
        this.nivelSocio = nivelSocio;
    }

    public boolean isEsInvitado() {
        return esInvitado;
    }

    public void setEsInvitado(boolean esInvitado) {
        this.esInvitado = esInvitado;
    }

    public List getHistorialCompras() {
        return historialCompras;
    }

    public void setHistorialCompras(List historialCompras) {
        this.historialCompras = historialCompras;
    }
    
}
