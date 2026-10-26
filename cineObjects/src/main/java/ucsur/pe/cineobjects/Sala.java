package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 * Por qué existe: es una sala de una sede con sus butacas y funciones.
 * Evita que dos funciones se crucen, contando 15 minutos de limpieza.
 */
public class Sala {
    // Estados de una sala (constantes: se usan como Sala.NOMBRE y su valor no cambia).
    public static final String ACTIVA = "ACTIVA"; // en uso; puede tener funciones
    public static final String INACTIVA = "INACTIVA"; // fuera de uso
    public static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO"; // en reparación temporal
    public static final int BUFFER_LIMPIEZA_MIN = 15; // minutos de limpieza entre el fin de una función y la siguiente
    
    private int idSala;
    private int numero; // número visible de la sala dentro de su sede
    private int capacidad;
    private String estado;
    private Sede sede;
    private TipoSala tipoSala;
    /** Butacas físicas configuradas para esta sala. */
    private List<Butaca> butacas;
    /** Funciones asignadas; se usan para comprobar cruces de horario y limpieza. */
    private List<Funcion> funciones;

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
        this.funciones = new ArrayList<>();
        setEstado(estado != null ? estado : ACTIVA);
    }

    /** Agrega una butaca de esta sala sin superar la capacidad ni repetir fila y número. */
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

    /** Devuelve la lista de butacas sin exponer la lista interna de la sala. */
    public List<Butaca> obtenerMapaButacas() {
        return new ArrayList<>(butacas); // copia: no se modifica desde afuera
    }

    /** Solo una sala ACTIVA puede ofrecer funciones y asientos. */
    public boolean estaOperativa() {
        return ACTIVA.equals(estado);
    }

    /** Cambia el estado; solo admite ACTIVA, INACTIVA o EN_MANTENIMIENTO. */
    public void setEstado(String estado) {
        if (!ACTIVA.equals(estado) && !INACTIVA.equals(estado) && !EN_MANTENIMIENTO.equals(estado)) {
            throw new IllegalArgumentException("Estado de sala inválido: " + estado);
        }
        this.estado = estado;
    }
    
    /** Registra una función si pertenece a esta sala y no cruza otro horario ni su tiempo de limpieza. */
    public void agregarFuncion(Funcion nuevaFuncion) {
        if (nuevaFuncion == null) {
            throw new IllegalArgumentException("La función no puede ser nula");
        }
        if (nuevaFuncion.getSala() != this) {
            throw new IllegalArgumentException("La función pertenece a otra sala");
        }
        if (funciones.contains(nuevaFuncion)) {
            return; // Ya está registrada
        }

        // Se incluye el tiempo de limpieza posterior al término de la nueva función.
        DateTime inicioNueva = nuevaFuncion.obtenerFechaHoraInicio();
        DateTime finNueva = inicioNueva.sumarMinutos(nuevaFuncion.getPelicula().getDuracion());
        DateTime finNuevaConBuffer = finNueva.sumarMinutos(BUFFER_LIMPIEZA_MIN);

        for (Funcion existente : funciones) {
            // Una función cancelada ya no ocupa el horario de la sala.
            if (Funcion.CANCELADA.equals(existente.getEstado())) {
                continue;
            }

            DateTime inicioExistente = existente.obtenerFechaHoraInicio();
            DateTime finExistente = inicioExistente.sumarMinutos(existente.getPelicula().getDuracion());
            DateTime finExistenteConBuffer = finExistente.sumarMinutos(BUFFER_LIMPIEZA_MIN);

            // Hay cruce si cada intervalo empieza antes de que termine el otro (incluida limpieza).
            if (inicioNueva.esAntesDe(finExistenteConBuffer) && inicioExistente.esAntesDe(finNuevaConBuffer)) {
                throw new IllegalStateException(String.format(
                        "Solapamiento en Sala %d: la función '%s' (%s - %s) choca con '%s' (%s - %s, más %d min de limpieza hasta %s)",
                        this.numero,
                        nuevaFuncion.getPelicula().getTitulo(), inicioNueva.soloHora(), finNueva.soloHora(),
                        existente.getPelicula().getTitulo(), inicioExistente.soloHora(), finExistente.soloHora(),
                        BUFFER_LIMPIEZA_MIN, finExistenteConBuffer.soloHora()
                ));
            }
        }

        funciones.add(nuevaFuncion);
    }

    /** Devuelve una copia de las funciones asignadas a la sala. */
    public List<Funcion> getFunciones() {
        return new ArrayList<>(funciones);
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
