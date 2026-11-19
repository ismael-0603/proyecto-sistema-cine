package ucsur.pe.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/**
 * Por qué existe: es la proyección de una película en una sala, en una fecha y hora.
 * Sabe qué butacas están libres y si todavía se puede vender.
 */
public class Funcion {
    // Estados de una función (constantes: se usan como Funcion.NOMBRE y su valor no cambia).
    public static final String DISPONIBLE = "DISPONIBLE"; // se pueden vender entradas
    public static final String AGOTADA = "AGOTADA"; // no quedan butacas libres
    public static final String EN_CURSO = "EN_CURSO"; // la película empezó y ya cerró la venta (pasó la tolerancia)
    public static final String FINALIZADA = "FINALIZADA"; // la película ya terminó
    public static final String CANCELADA = "CANCELADA"; // el cine la canceló; permite reembolsos

    /**
     * Minutos después del inicio en que todavía se venden entradas (como la tolerancia de 20 minutos
     * de otras cadenas): quien llega un poco tarde aún puede comprar. Pasado este tiempo, la venta cierra.
     */
    public static final int TOLERANCIA_VENTA_MIN = 20;

    private int idFuncion;
    private DateTime fecha;      // solo fecha: DateTime(dia, mes, anio)
    private DateTime horaInicio; // solo hora: DateTime(hora, minuto)
    private String idioma;
    private String estado;
    private Pelicula pelicula;
    private Sala sala;
    /** Entradas asociadas a la función; permite detectar butacas ocupadas o bloqueadas. */
    private List<Entrada> entradasEmitidas;
    private final Reloj reloj; // de dónde sale la hora actual para saber si ya empezó o terminó

    /** Recibe el reloj (el real o uno fijo en pruebas) para calcular si la función ya empezó o terminó. */
    public Funcion(int idFuncion, DateTime fecha, DateTime horaInicio, String idioma,
                   String estado, Pelicula pelicula, Sala sala, Reloj reloj) {
        if (reloj == null) throw new IllegalArgumentException("El reloj es obligatorio");
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
        this.reloj = reloj;
        String estadoInicial = (estado != null) ? estado : DISPONIBLE;
        validarEstado(estadoInicial);
        this.estado = estadoInicial;
    }

    /** Combina la fecha y la hora guardadas por separado para comparar el horario completo. */
    public DateTime obtenerFechaHoraInicio() {
        return new DateTime(fecha.getDia(), fecha.getMes(), fecha.getAnio(),
                horaInicio.getHora(), horaInicio.getMinuto());
    }

    /** Suma la duración a la fecha y hora de inicio, incluso si termina al día siguiente. */
    public DateTime obtenerFechaHoraFin() {
        return obtenerFechaHoraInicio().sumarMinutos(pelicula.getDuracion());
    }

    /** Hora en que se deja de vender: el inicio más los minutos de tolerancia. */
    public DateTime obtenerCierreVenta() {
        return obtenerFechaHoraInicio().sumarMinutos(TOLERANCIA_VENTA_MIN);
    }

    /**
     * Calcula el estado actual sin cambiar nada: CANCELADA si el cine la canceló; si no, según la hora
     * (EN_CURSO, FINALIZADA) y las butacas libres (AGOTADA, DISPONIBLE).
     * Durante los primeros minutos de la película (la tolerancia) sigue DISPONIBLE o AGOTADA:
     * recién al cerrar la venta pasa a EN_CURSO.
     */
    private String calcularEstado() {
        if (CANCELADA.equals(estado)) return CANCELADA;
        DateTime ahora = reloj.ahora();
        if (ahora.esDespuesDe(obtenerFechaHoraFin())) return FINALIZADA;
        if (!ahora.esAntesDe(obtenerCierreVenta())) return EN_CURSO;
        if (consultarDisponibilidad() == 0) return AGOTADA;
        return DISPONIBLE;
    }

    /**
     * Revisa, sin cambiar nada, si una compra iniciada en {@code inicioCompra} todavía puede vender
     * entradas de esta función. Devuelve null si puede, o el motivo si no.
     * Lo que cuenta es cuándo EMPEZÓ la compra: si empezó antes del cierre (inicio + tolerancia), se puede
     * terminar dentro de sus 4 minutos aunque el cierre pase mientras tanto; si empezó justo al cierre o
     * después, ya no. No mira las butacas libres: eso se revisa aparte (si la propia compra bloqueó las
     * últimas, la función figura AGOTADA y aun así debe poder pagarse).
     */
    public String motivoVentaCerrada(DateTime inicioCompra) {
        String actual = calcularEstado();
        if (CANCELADA.equals(actual)) return "la función " + this + " fue cancelada";
        if (FINALIZADA.equals(actual)) return "la función " + this + " ya terminó";
        if (inicioCompra == null || !inicioCompra.esAntesDe(obtenerCierreVenta())) {
            return "la venta de la función " + this + " cerró a las " + obtenerCierreVenta().soloHora()
                    + (inicioCompra != null ? " y la compra empezó a las " + inicioCompra.soloHora() : "");
        }
        return null;
    }

    /** Indica si se puede vender (estado DISPONIBLE) tras actualizar el estado. */
    public boolean estaDisponible() {
        return DISPONIBLE.equals(calcularEstado());
    }

    /** Una función pendiente es la que aún no terminó y no fue cancelada. */
    public boolean estaPendiente() {
        String actual = calcularEstado();
        return !CANCELADA.equals(actual) && !FINALIZADA.equals(actual);
    }

    /**
     * Cancela la función (por ejemplo, por una falla técnica); permite reembolsar sus entradas.
     * Reemplaza al antiguo setEstado: los demás estados los calcula getEstado() según la hora
     * y las butacas libres, así que no se ponen a mano.
     */
    public void cancelar() {
        if (FINALIZADA.equals(calcularEstado())) {
            throw new IllegalStateException("No se puede cancelar una función que ya terminó");
        }
        this.estado = CANCELADA;
    }

    /**
     * Vuelve a poner a la venta una función cancelada, solo si todavía no empezó y su horario
     * sigue libre en la sala (otra función pudo ocuparlo mientras estuvo cancelada).
     */
    public void reactivar() {
        if (!CANCELADA.equals(estado)) {
            throw new IllegalStateException("Solo se reactiva una función CANCELADA");
        }
        if (!reloj.ahora().esAntesDe(obtenerFechaHoraInicio())) {
            throw new IllegalStateException("No se puede reactivar una función cuya fecha ya pasó");
        }
        sala.validarHorarioLibre(this, obtenerFechaHoraInicio());
        this.estado = DISPONIBLE;
    }

    /**
     * Cambia fecha, hora e idioma de una función que aún no vendió ni bloqueó entradas
     * (quien ya compró eligió ese horario). La sala vuelve a revisar que no se cruce con otra.
     */
    public void reprogramar(DateTime nuevaFecha, DateTime nuevaHora, String nuevoIdioma) {
        if (nuevaFecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (nuevaHora == null) throw new IllegalArgumentException("La hora de inicio es obligatoria");
        if (nuevoIdioma == null || nuevoIdioma.trim().isEmpty() || nuevoIdioma.trim().length() > 30) {
            throw new IllegalArgumentException("El idioma es obligatorio (máx. 30 caracteres)");
        }
        if (CANCELADA.equals(estado)) {
            throw new IllegalStateException("Reactive la función antes de editarla");
        }
        for (Entrada e : entradasEmitidas) {
            if (Entrada.VENDIDA.equals(e.getEstado())
                    || (Entrada.BLOQUEADA.equals(e.getEstado()) && !e.bloqueoVencido(reloj.ahora()))) {
                throw new IllegalStateException("La función ya tiene entradas vendidas o reservadas; no se puede editar");
            }
        }
        DateTime nuevoInicio = new DateTime(nuevaFecha.getDia(), nuevaFecha.getMes(), nuevaFecha.getAnio(),
                nuevaHora.getHora(), nuevaHora.getMinuto());
        if (!reloj.ahora().esAntesDe(nuevoInicio)) {
            throw new IllegalArgumentException("La nueva fecha y hora deben ser futuras");
        }
        sala.validarHorarioLibre(this, nuevoInicio);
        this.fecha = nuevaFecha;
        this.horaInicio = nuevaHora;
        this.idioma = nuevoIdioma.trim();
    }

    /** Valida que el estado inicial sea uno de los cinco definidos (en la BD, ck_funcion_estado). */
    private static void validarEstado(String estado) {
        if (!DISPONIBLE.equals(estado) && !AGOTADA.equals(estado) && !EN_CURSO.equals(estado)
                && !FINALIZADA.equals(estado) && !CANCELADA.equals(estado)) {
            throw new IllegalArgumentException("Estado de función inválido: " + estado);
        }
    }

    /** Una butaca ocupada por una venta o por un bloqueo vigente no está disponible. */
    public boolean esButacaDisponible(Butaca butaca) {
        if (butaca == null || butaca.getSala() != this.sala) {
            return false;
        }
        for (Entrada e : entradasEmitidas) {
            if (e.getButaca() != butaca) continue;
            if (Entrada.VENDIDA.equals(e.getEstado())) return false;
            // Un bloqueo vencido ya no ocupa la butaca; consultar no lo expira (eso lo hace la compra o la BD).
            if (Entrada.BLOQUEADA.equals(e.getEstado()) && !e.bloqueoVencido(reloj.ahora())) return false;
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
    
    /** Quita una entrada de la función para dejar la butaca libre sin marcarla como anulada; solo la usa Entrada. */
    void liberarEntrada(Entrada entrada) {
        entradasEmitidas.remove(entrada);
    }

    // Pesos de la recomendación: cuánto "cuesta" alejarse de lo ideal (menor puntaje = mejor grupo).
    private static final double PESO_FILA = 1.0;     // por cada fila de distancia a la fila ideal
    private static final double PESO_LATERAL = 3.0;  // si todo el grupo queda en un bloque lateral
    private static final double PESO_PASILLO = 2.5;  // por cada pasillo que cruza el grupo
    private static final double PESO_CENTRO = 0.5;   // por cada butaca de distancia al centro de su bloque

    /** Recomienda sin butacas accesibles y sin butacas ya elegidas (ver la versión completa). */
    public List<Butaca> recomendarMejoresAsientos(int cantidad) {
        return recomendarMejoresAsientos(cantidad, false, List.of());
    }

    /**
     * Recomienda las mejores butacas libres para un grupo (RF-29), como lo haría un cine real:
     * <ol>
     *   <li>Busca butacas seguidas en una misma fila y elige el grupo con menor puntaje: lo más cerca posible
     *       de la fila ideal (a dos tercios del fondo), dentro del bloque central y centrado en él.</li>
     *   <li>Un grupo que cruza un pasillo o que cae en un bloque lateral se acepta, pero con penalidad.</li>
     *   <li>Si ninguna fila tiene tantas butacas seguidas, toma una por una las mejores butacas libres.</li>
     * </ol>
     * @param incluirAccesibles true si el cliente necesita butacas para discapacidad; si es false, no se proponen
     * @param yaElegidas butacas que la compra ya preseleccionó: no se vuelven a recomendar
     * @throws IllegalStateException si no hay suficientes butacas libres para la cantidad pedida
     */
    public List<Butaca> recomendarMejoresAsientos(int cantidad, boolean incluirAccesibles, Collection<Butaca> yaElegidas) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        List<Butaca> libres = new ArrayList<>();
        for (Butaca b : obtenerButacasDisponibles()) {
            if (b.esAccesible() && !incluirAccesibles) continue;
            if (yaElegidas != null && yaElegidas.contains(b)) continue;
            libres.add(b);
        }
        if (libres.size() < cantidad) {
            throw new IllegalStateException("Solo quedan " + libres.size() + " butacas libres; no alcanzan para "
                    + cantidad + " entradas");
        }

        List<String> filas = sala.obtenerFilas();
        int filaIdeal = Sala.indiceFilaIdeal(filas.size());
        Map<String, List<Butaca>> libresPorFila = new LinkedHashMap<>();
        for (Butaca b : libres) {
            libresPorFila.computeIfAbsent(b.getFila(), k -> new ArrayList<>()).add(b);
        }

        // 1 y 2. Grupos de butacas seguidas (números consecutivos) en una misma fila.
        List<Butaca> mejorGrupo = null;
        double mejorPuntaje = Double.MAX_VALUE;
        for (Map.Entry<String, List<Butaca>> entrada : libresPorFila.entrySet()) {
            List<Butaca> fila = entrada.getValue();
            fila.sort(Comparator.comparingInt(Butaca::getNumero));
            int distanciaFila = Math.abs(filas.indexOf(entrada.getKey()) - filaIdeal);
            for (int i = 0; i + cantidad <= fila.size(); i++) {
                List<Butaca> grupo = fila.subList(i, i + cantidad);
                if (grupo.get(cantidad - 1).getNumero() - grupo.get(0).getNumero() != cantidad - 1) {
                    continue; // hay una butaca ocupada en medio: no están seguidas
                }
                double puntaje = distanciaFila * PESO_FILA + puntajeEnFila(grupo);
                if (puntaje < mejorPuntaje) {
                    mejorPuntaje = puntaje;
                    mejorGrupo = new ArrayList<>(grupo);
                }
            }
        }
        if (mejorGrupo != null) return mejorGrupo;

        // 3. No hay tantas butacas seguidas: se eligen las mejores una por una.
        libres.sort(Comparator.comparingDouble(
                b -> Math.abs(filas.indexOf(b.getFila()) - filaIdeal) * PESO_FILA + puntajeEnFila(List.of(b))));
        return new ArrayList<>(libres.subList(0, cantidad));
    }

    /** Penalidad del grupo dentro de su fila: pasillos que cruza, bloque lateral y distancia al centro del bloque. */
    private double puntajeEnFila(List<Butaca> grupo) {
        int pasillos = 0;
        int enCentro = 0;
        for (int i = 0; i < grupo.size(); i++) {
            if (grupo.get(i).estaEnBloqueCentral()) enCentro++;
            if (i > 0 && !grupo.get(i).getBloque().equals(grupo.get(i - 1).getBloque())) pasillos++;
        }
        double puntaje = pasillos * PESO_PASILLO;
        if (enCentro == 0) puntaje += PESO_LATERAL;

        // El centro de referencia es el del bloque donde está la mayor parte del grupo.
        String bloque = (enCentro * 2 >= grupo.size()) ? Butaca.CENTRO : grupo.get(0).getBloque();
        double centroGrupo = (grupo.get(0).getNumero() + grupo.get(grupo.size() - 1).getNumero()) / 2.0;
        puntaje += Math.abs(centroGrupo - centroDeBloque(grupo.get(0).getFila(), bloque)) * PESO_CENTRO;
        return puntaje;
    }

    /** Centro (por número de butaca) de un bloque dentro de una fila, usando todas sus butacas, libres u ocupadas. */
    private double centroDeBloque(String fila, String bloque) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (Butaca b : sala.obtenerMapaButacas()) {
            if (b.getFila().equals(fila) && b.getBloque().equals(bloque)) {
                min = Math.min(min, b.getNumero());
                max = Math.max(max, b.getNumero());
            }
        }
        return (min == Integer.MAX_VALUE) ? 0 : (min + max) / 2.0;
    }

    /**
     * Registra una entrada solo si pertenece a esta función, ya trae su hora de expiración y su butaca
     * aún está libre. No es pública: solo Entrada la llama al bloquearse, así ninguna butaca queda
     * bloqueada para siempre por una entrada sin vencimiento.
     */
    void registrarEntrada(Entrada entrada, DateTime inicioCompra) {
        // Solo la llama Entrada sobre sí misma, y su constructor ya garantiza que es de esta función y sala.
        if (!Entrada.BLOQUEADA.equals(entrada.getEstado()) || entrada.getExpiraBloqueo() == null) {
            throw new IllegalStateException("La entrada debe llegar bloqueada y con su hora de expiración");
        }
        String motivo = motivoVentaCerrada(inicioCompra);
        if (motivo != null) {
            throw new IllegalStateException("No se puede vender: " + motivo);
        }
        if (!esButacaDisponible(entrada.getButaca())) {
            throw new IllegalStateException("La butaca " + entrada.getButaca().getCodigoAsiento() + " ya está ocupada");
        }
        entradasEmitidas.add(entrada); // si era la última butaca, getEstado() ya responde AGOTADA
    }

    /**
     * Cuelga de la función una entrada leída de la base de datos. No vuelve a validar la venta
     * (eso se hizo cuando se vendió): solo la llama Entrada.restaurar, que ya comprobó que es de esta función.
     * Así, esButacaDisponible decide según el estado de la entrada y su bloqueo.
     */
    void restaurarEntrada(Entrada entrada) {
        if (entrada.getFuncion() != this) {
            throw new IllegalArgumentException("La entrada es de otra función");
        }
        if (!entradasEmitidas.contains(entrada)) {
            entradasEmitidas.add(entrada);
        }
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una función nueva nace con id 0. */
    public void asignarId(int idFuncion) {
        this.idFuncion = Ids.asignar(this.idFuncion, idFuncion, "la función");
    }

    public int getIdFuncion() { return idFuncion; }
    public DateTime getFecha() { return fecha; }
    public DateTime getHoraInicio() { return horaInicio; }
    public String getIdioma() { return idioma; }
    /** Estado actual, calculado al momento de consultarlo. */
    public String getEstado() { return calcularEstado(); }
    public Pelicula getPelicula() { return pelicula; }
    public Sala getSala() { return sala; }

    @Override
    public String toString() {
        return pelicula + " | " + fecha.soloFecha() + " " + horaInicio.soloHora() + " | " + sala;
    }
}
