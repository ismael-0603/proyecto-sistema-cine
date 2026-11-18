package ucsur.pe.model;

import java.util.ArrayList;
import java.util.List;
/**
 * Por qué existe: es una sala de una sede con sus butacas y funciones.
 * Evita que dos funciones se crucen, contando 15 minutos de limpieza, y genera su mapa de butacas
 * con dos pasillos: un bloque lateral a cada lado y el bloque central, el más ancho (por ejemplo 4 | 10 | 4).
 */
public class Sala {
    // Estados de una sala (constantes: se usan como Sala.NOMBRE y su valor no cambia).
    public static final String ACTIVA = "ACTIVA"; // en uso; puede tener funciones
    public static final String INACTIVA = "INACTIVA"; // fuera de uso
    public static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO"; // en reparación temporal
    public static final int BUFFER_LIMPIEZA_MIN = 15; // minutos de limpieza entre el fin de una función y la siguiente
    public static final int MAX_FILAS = 26; // filas de la A a la Z
    
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

    /**
     * Crea todas las butacas de la sala con dos pasillos: por fila, {@code lateral} butacas a la izquierda,
     * {@code central} en el medio y {@code lateral} a la derecha, numeradas de izquierda a derecha (1, 2, 3...).
     * El total debe ser igual a la capacidad. Las butacas para discapacidad quedan junto a los pasillos
     * en la primera fila (y en la última si la sala tiene 6 filas o más); las preferenciales, en el centro
     * del bloque central alrededor de la fila ideal.
     */
    public void generarButacas(int filas, int lateral, int central) {
        if (!butacas.isEmpty()) {
            throw new IllegalStateException("La sala ya tiene butacas; use redistribuir para cambiarlas");
        }
        validarDistribucion(filas, lateral, central, capacidad);
        int porFila = 2 * lateral + central;
        int ideal = indiceFilaIdeal(filas);
        int ultimaFila = filas - 1;
        for (int f = 0; f < filas; f++) {
            String letra = String.valueOf((char) ('A' + f));
            boolean filaAccesible = (f == 0) || (f == ultimaFila && filas >= 6);
            for (int n = 1; n <= porFila; n++) {
                String bloque = (n <= lateral) ? Butaca.IZQUIERDA
                        : (n <= lateral + central) ? Butaca.CENTRO : Butaca.DERECHA;
                String tipo = Butaca.ESTANDAR;
                boolean juntoAPasillo = lateral > 0 ? (n == lateral || n == lateral + central + 1)
                                                    : (n == 1 || n == porFila);
                int posEnCentro = n - lateral; // 1..central dentro del bloque central
                if (filaAccesible && juntoAPasillo) {
                    tipo = Butaca.DISCAPACIDAD;
                } else if (Butaca.CENTRO.equals(bloque) && Math.abs(f - ideal) <= 1
                        && posEnCentro > central / 4 && posEnCentro <= central - central / 4) {
                    tipo = Butaca.PREFERENCIAL;
                }
                butacas.add(new Butaca(0, letra, n, tipo, bloque, this));
            }
        }
    }

    /**
     * Cambia la distribución (y con ella la capacidad) de una sala que aún no tiene funciones:
     * si ya tiene, sus entradas apuntan a las butacas actuales y no se pueden reemplazar.
     */
    public void redistribuir(int filas, int lateral, int central) {
        if (!funciones.isEmpty()) {
            throw new IllegalStateException("No se puede cambiar la distribución de una sala con funciones programadas");
        }
        int nuevaCapacidad = filas * (2 * lateral + central);
        validarDistribucion(filas, lateral, central, nuevaCapacidad);
        this.butacas = new ArrayList<>();
        this.capacidad = nuevaCapacidad;
        generarButacas(filas, lateral, central);
    }

    /** Comprueba que la distribución sea válida y que su total coincida con la capacidad esperada. */
    private static void validarDistribucion(int filas, int lateral, int central, int capacidadEsperada) {
        if (filas <= 0 || filas > MAX_FILAS) {
            throw new IllegalArgumentException("La sala debe tener entre 1 y " + MAX_FILAS + " filas");
        }
        if (lateral < 0) {
            throw new IllegalArgumentException("Las butacas laterales no pueden ser negativas");
        }
        if (central <= 0) {
            throw new IllegalArgumentException("El bloque central debe tener al menos una butaca");
        }
        if (lateral > 0 && central <= lateral) {
            throw new IllegalArgumentException("El bloque central debe ser más ancho que los laterales");
        }
        int total = filas * (2 * lateral + central);
        if (total != capacidadEsperada) {
            throw new IllegalArgumentException("La distribución da " + total + " butacas y la capacidad es " + capacidadEsperada);
        }
    }

    /**
     * Fila con mejor vista: a unos dos tercios del camino desde la pantalla hacia el fondo
     * (en una sala de 9 filas, la F). Devuelve su posición empezando en 0.
     */
    public static int indiceFilaIdeal(int cantidadFilas) {
        if (cantidadFilas <= 1) return 0;
        return (int) Math.round((cantidadFilas - 1) * 2.0 / 3.0);
    }

    /** Filas de la sala en orden desde la pantalla (A, B, C...). */
    public List<String> obtenerFilas() {
        List<String> filas = new ArrayList<>();
        for (Butaca b : butacas) {
            if (!filas.contains(b.getFila())) filas.add(b.getFila());
        }
        // Primero por longitud y luego alfabético: A..Z antes que AA.
        filas.sort((a, b) -> a.length() != b.length() ? Integer.compare(a.length(), b.length()) : a.compareTo(b));
        return filas;
    }

    /** Devuelve la lista de butacas sin exponer la lista interna de la sala. */
    public List<Butaca> obtenerMapaButacas() {
        return new ArrayList<>(butacas); // copia: no se modifica desde afuera
    }

    /** Solo una sala ACTIVA puede ofrecer funciones y asientos. */
    public boolean estaOperativa() {
        return ACTIVA.equals(estado);
    }

    /** Cambia el número visible y el formato; la sede comprueba que el número no se repita. */
    void actualizarDatos(int numero, TipoSala tipoSala) {
        if (numero <= 0) throw new IllegalArgumentException("El número de sala debe ser mayor a 0");
        if (tipoSala == null) throw new IllegalArgumentException("El tipo de sala es obligatorio");
        this.numero = numero;
        this.tipoSala = tipoSala;
    }

    /** Indica si tiene funciones que aún no terminan y no fueron canceladas (impiden darla de baja). */
    public boolean tieneFuncionesPendientes() {
        for (Funcion f : funciones) {
            if (f.estaPendiente()) return true;
        }
        return false;
    }

    /** Cambia el estado; solo admite ACTIVA, INACTIVA o EN_MANTENIMIENTO. */
    public void setEstado(String estado) {
        if (!ACTIVA.equals(estado) && !INACTIVA.equals(estado) && !EN_MANTENIMIENTO.equals(estado)) {
            throw new IllegalArgumentException("Estado de sala inválido: " + estado);
        }
        this.estado = estado;
    }
    
    /**
     * Registra una función si pertenece a esta sala y no cruza otro horario ni su tiempo de limpieza.
     * No es pública: se programa con Sede.agregarFuncion, que además la agrega a la cartelera.
     */
    void agregarFuncion(Funcion nuevaFuncion) {
        if (nuevaFuncion == null) {
            throw new IllegalArgumentException("La función no puede ser nula");
        }
        if (nuevaFuncion.getSala() != this) {
            throw new IllegalArgumentException("La función pertenece a otra sala");
        }
        if (funciones.contains(nuevaFuncion)) {
            return; // Ya está registrada
        }
        validarHorarioLibre(nuevaFuncion, nuevaFuncion.obtenerFechaHoraInicio());
        funciones.add(nuevaFuncion);
    }

    /**
     * Comprueba que una función que empezaría en {@code inicioNueva} no se cruce con otra de la sala
     * (ni con su limpieza). Ignora a la propia función, para poder reprogramarla o reactivarla.
     */
    void validarHorarioLibre(Funcion nuevaFuncion, DateTime inicioNueva) {
        // Se incluye el tiempo de limpieza posterior al término de la nueva función.
        DateTime finNueva = inicioNueva.sumarMinutos(nuevaFuncion.getPelicula().getDuracion());
        DateTime finNuevaConBuffer = finNueva.sumarMinutos(BUFFER_LIMPIEZA_MIN);

        for (Funcion existente : funciones) {
            // Una función cancelada ya no ocupa el horario de la sala.
            if (existente == nuevaFuncion || Funcion.CANCELADA.equals(existente.getEstado())) {
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
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una sala nueva nace con id 0. */
    public void asignarId(int idSala) {
        this.idSala = Ids.asignar(this.idSala, idSala, "la sala");
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
