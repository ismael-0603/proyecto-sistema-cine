package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author user
 */
public class Sala {
    public static final String ACTIVA = "ACTIVA";
    public static final String INACTIVA = "INACTIVA";
    public static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO";

    private int idSala;
    private int numero;
    private int capacidad;
    private String estado;
    private Sede sede;
    private TipoSala tipoSala;
    private List<Butaca> butacas;

    public Sala(int idSala, int numero, int capacidad, String estado, Sede sede, TipoSala tipoSala) {
        if (numero <= 0) throw new IllegalArgumentException("El número de sala debe ser mayor a 0");
        if (capacidad <= 0) throw new IllegalArgumentException("La capacidad debe ser mayor a 0");
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (tipoSala == null) throw new IllegalArgumentException("El tipo de sala es obligatorio");
        this.idSala = idSala;
        this.numero = numero;
        this.capacidad = capacidad;
        this.sede = sede;
        this.tipoSala = tipoSala;
        this.butacas = new ArrayList<>();
        setEstado(estado != null ? estado : ACTIVA);
    }

    public void agregarButaca(Butaca butaca) {
        if (butaca == null) {
            throw new IllegalArgumentException("La butaca no puede ser nula");
        }
        if (butaca.getSala() != this) {
            throw new IllegalArgumentException("La butaca pertenece a otra sala");
        }
        if (butacas.size() >= capacidad) {
            throw new IllegalStateException("La sala ya alcanzó su capacidad de " + capacidad);
        }
        for (Butaca b : butacas) {
            if (b.getFila().equals(butaca.getFila()) && b.getNumero() == butaca.getNumero()) {
                throw new IllegalArgumentException("Ya existe la butaca " + butaca.getCodigoAsiento());
            }
        }
        butacas.add(butaca);
    }

    public List<Butaca> obtenerMapaButacas() {
        return new ArrayList<>(butacas); // copia: no se modifica desde afuera
    }

    public boolean estaOperativa() {
        return ACTIVA.equals(estado);
    }

    public void setEstado(String estado) {
        if (!ACTIVA.equals(estado) && !INACTIVA.equals(estado) && !EN_MANTENIMIENTO.equals(estado)) {
            throw new IllegalArgumentException("Estado de sala inválido: " + estado);
        }
        this.estado = estado;
    }

    public int getIdSala() { return idSala; }
    public int getNumero() { return numero; }
    public int getCapacidad() { return capacidad; }
    public String getEstado() { return estado; }
    public Sede getSede() { return sede; }
    public TipoSala getTipoSala() { return tipoSala; }

    @Override
    public String toString() {
        return "Sala " + numero + " (" + tipoSala + ")";
    }
}