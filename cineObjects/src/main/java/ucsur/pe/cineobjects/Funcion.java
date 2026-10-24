package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Representa una proyección y administra su horario, estado, disponibilidad y entradas emitidas.
 * Requiere fecha, hora, idioma, película y sala; el estado solo puede ser uno de los estados definidos.
 */
public class Funcion {
    public static final String DISPONIBLE = "DISPONIBLE";
    public static final String AGOTADA = "AGOTADA";
    public static final String EN_CURSO = "EN_CURSO";
    public static final String FINALIZADA = "FINALIZADA";
    public static final String CANCELADA = "CANCELADA";

    private int idFuncion;
    private DateTime fecha;      // solo fecha: DateTime(dia, mes, anio)
    private DateTime horaInicio; // solo hora: DateTime(hora, minuto)
    private String idioma;
    private String estado;
    private Pelicula pelicula;
    private Sala sala;
    /** Entradas asociadas a la función; permite detectar butacas ocupadas o bloqueadas. */
    private List<Entrada> entradasEmitidas;

    public Funcion(int idFuncion, DateTime fecha, DateTime horaInicio, String idioma,
                   String estado, Pelicula pelicula, Sala sala) {
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (horaInicio == null) throw new IllegalArgumentException("La hora de inicio es obligatoria");
        if (idioma == null || idioma.trim().isEmpty() || idioma.trim().length() > 30) {
            throw new IllegalArgumentException("El idioma es obligatorio (máx. 30 caracteres)");
        }
        if (pelicula == null) throw new IllegalArgumentException("La película es obligatoria");
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        this.idFuncion = idFuncion;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.idioma = idioma.trim();
        this.pelicula = pelicula;
        this.sala = sala;
        this.entradasEmitidas = new ArrayList<>();
        setEstado(estado != null ? estado : DISPONIBLE);
    }

    // Combina la fecha y la hora de inicio para comparar el horario completo.
    /** Combina la fecha y la hora guardadas por separado para comparar el horario completo. */
    public DateTime obtenerFechaHoraInicio() {
        return new DateTime(fecha.getDia(), fecha.getMes(), fecha.getAnio(),
                horaInicio.getHora(), horaInicio.getMinuto());
    }

    // La duración puede llevar el fin al día siguiente.
    /** Suma la duración a la fecha y hora de inicio, incluso si termina al día siguiente. */
    public DateTime obtenerFechaHoraFin() {
        return obtenerFechaHoraInicio().sumarMinutos(pelicula.getDuracion());
    }

    // Devuelve únicamente la hora de fin para mostrarla sin la fecha.
    public DateTime calcularHoraFin() {
        return horaInicio.sumarMinutosAHora(pelicula.getDuracion());
    }

    // El estado se deriva del horario y la disponibilidad; CANCELADA es un estado permanente.
    /** Actualiza el estado según la hora actual, las entradas disponibles y si fue cancelada. */
    public void actualizarEstado() {
        if (CANCELADA.equals(estado)) return;
        DateTime ahora = DateTime.ahora();
        if (ahora.esDespuesDe(obtenerFechaHoraFin())) {
            estado = FINALIZADA;
        } else if (!ahora.esAntesDe(obtenerFechaHoraInicio())) {
            estado = EN_CURSO;
        } else if (consultarDisponibilidad() == 0) {
            estado = AGOTADA;
        } else {
            estado = DISPONIBLE;
        }
    }

    public boolean estaDisponible() {
        actualizarEstado();
        return DISPONIBLE.equals(estado);
    }

    public void setEstado(String estado) {
        if (!DISPONIBLE.equals(estado) && !AGOTADA.equals(estado) && !EN_CURSO.equals(estado)
                && !FINALIZADA.equals(estado) && !CANCELADA.equals(estado)) {
            throw new IllegalArgumentException("Estado de función inválido: " + estado);
        }
        this.estado = estado;
    }

    // Consultas de idioma, formato y disponibilidad de asientos.
    public boolean esSubtitulada() {
        return idioma.toUpperCase().contains("SUB");
    }

    public String obtenerFormato() {
        return sala.getTipoSala().getFormato();
    }

    /** Una butaca ocupada por una venta o por un bloqueo vigente no está disponible. */
    public boolean esButacaDisponible(Butaca butaca) {
        if (butaca == null || butaca.getSala() != this.sala) {
            return false;
        }
        for (Entrada e : entradasEmitidas) {
            if (e.getButaca() != butaca) continue;
            if (Entrada.VENDIDA.equals(e.getEstado())) return false;
            if (Entrada.BLOQUEADA.equals(e.getEstado()) && !e.haExpirado()) return false;
        }
        return true;
    }

    /** Lista las butacas libres de la sala para esta función. */
    public List<Butaca> obtenerButacasDisponibles() {
        List<Butaca> disponibles = new ArrayList<>();
        for (Butaca b : sala.obtenerMapaButacas()) {
            if (esButacaDisponible(b)) {
                disponibles.add(b);
            }
        }
        return disponibles;
    }

    /** Cuenta cuántas butacas se pueden elegir en este momento. */
    public int consultarDisponibilidad() {
        return obtenerButacasDisponibles().size();
    }
    
    /** Quita una entrada de la función para dejar la butaca libre sin marcarla como anulada. */
    public void liberarEntrada(Entrada entrada) {
        entradasEmitidas.remove(entrada);
        actualizarEstado();
    }

    /**
     * Recomienda butacas contiguas cercanas al centro de una fila, excluyendo las accesibles.
     * Si no encuentra un grupo contiguo, devuelve las primeras butacas libres disponibles.
     */
    public List<Butaca> recomendarMejoresAsientos(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        Map<String, List<Butaca>> porFila = new LinkedHashMap<>();
        for (Butaca b : obtenerButacasDisponibles()) {
            if (b.esAccesible()) continue;
            porFila.computeIfAbsent(b.getFila(), k -> new ArrayList<>()).add(b);
        }

        List<Butaca> mejorGrupo = null;
        double mejorDistancia = Double.MAX_VALUE;
        for (Map.Entry<String, List<Butaca>> fila : porFila.entrySet()) {
            List<Butaca> libres = fila.getValue();
            libres.sort((a, b) -> Integer.compare(a.getNumero(), b.getNumero()));
            double centro = centroDeFila(fila.getKey());
            for (int i = 0; i + cantidad <= libres.size(); i++) {
                int primero = libres.get(i).getNumero();
                int ultimo = libres.get(i + cantidad - 1).getNumero();
                if (ultimo - primero != cantidad - 1) continue; // no son contiguas
                double distancia = Math.abs((primero + ultimo) / 2.0 - centro);
                if (distancia < mejorDistancia) {
                    mejorDistancia = distancia;
                    mejorGrupo = new ArrayList<>(libres.subList(i, i + cantidad));
                }
            }
        }
        if (mejorGrupo != null) return mejorGrupo;

        // No hay grupo contiguo suficiente, así que se completa con butacas libres separadas.
        List<Butaca> sueltas = new ArrayList<>();
        for (List<Butaca> libres : porFila.values()) {
            for (Butaca b : libres) {
                if (sueltas.size() < cantidad) sueltas.add(b);
            }
        }
        return sueltas;
    }

    /** Calcula el centro de una fila usando los números de sus butacas. */
    private double centroDeFila(String fila) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Butaca b : sala.obtenerMapaButacas()) {
            if (b.getFila().equals(fila)) {
                min = Math.min(min, b.getNumero());
                max = Math.max(max, b.getNumero());
            }
        }
        return (min + max) / 2.0;
    }

    /** Registra una entrada solo si pertenece a esta función y su butaca aún está libre. */
    public void registrarEntrada(Entrada entrada) {
        if (entrada == null) {
            throw new IllegalArgumentException("La entrada no puede ser nula");
        }
        if (entrada.getFuncion() != this) {
            throw new IllegalArgumentException("La entrada pertenece a otra función");
        }
        if (entrada.getButaca().getSala() != this.sala) {
            throw new IllegalArgumentException("La butaca no pertenece a la sala de la función");
        }
        actualizarEstado();
        if (!DISPONIBLE.equals(estado)) {
            throw new IllegalStateException("La función no está disponible para la venta (" + estado + ")");
        }
        if (!esButacaDisponible(entrada.getButaca())) {
            throw new IllegalStateException("La butaca " + entrada.getButaca().getCodigoAsiento() + " ya está ocupada");
        }
        entradasEmitidas.add(entrada);
        actualizarEstado(); // si era la última butaca, pasa a AGOTADA
    }

    public int getIdFuncion() { return idFuncion; }
    public DateTime getFecha() { return fecha; }
    public DateTime getHoraInicio() { return horaInicio; }
    public String getIdioma() { return idioma; }
    public String getEstado() { return estado; }
    public Pelicula getPelicula() { return pelicula; }
    public Sala getSala() { return sala; }
    public List<Entrada> getEntradasEmitidas() { return new ArrayList<>(entradasEmitidas); }

    @Override
    public String toString() {
        return pelicula + " | " + fecha.soloFecha() + " " + horaInicio.soloHora() + " | " + sala;
    }
}
