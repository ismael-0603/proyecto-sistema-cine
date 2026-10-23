package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Sala {
    private int idSala;
    private int numero;
    private int capacidad;
    private boolean estado; //Boolean o String ;V para poner Operativo/En mantenimiento y no solo true o false

    public Sala(int idSala, int numero, int capacidad, boolean estado) {
        this.idSala = idSala;
        this.numero = numero;
        this.capacidad = capacidad;
        this.estado = estado;
    }

    public int getIdSala() {
        return idSala;
    }

    public void setIdSala(int idSala) {
        this.idSala = idSala;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(int capacidad) {
        this.capacidad = capacidad;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }
    
    
    
}
