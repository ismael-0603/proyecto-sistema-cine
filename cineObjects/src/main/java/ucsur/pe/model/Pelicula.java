package ucsur.pe.model;

import java.util.ArrayList;
import java.util.List;
/**
 * Por qué existe: guarda los datos de una película (título, duración, clasificación y géneros)
 * para mostrarla en la cartelera.
 */
public class Pelicula {
    // Estados de una película (constantes: se usan como Pelicula.NOMBRE y su valor no cambia).
    public static final String PROXIMAMENTE = "PROXIMAMENTE"; // anunciada; aún no tiene funciones
    public static final String PREESTRENO = "PREESTRENO"; // vende funciones antes de su fecha de estreno
    public static final String EN_CARTELERA = "EN_CARTELERA"; // se está proyectando
    public static final String REESTRENO = "REESTRENO"; // película antigua de vuelta en cines (fechaEstreno es la original)
    public static final String FUERA_DE_CARTELERA = "FUERA_DE_CARTELERA"; // ya no se proyecta (equivale a desactivarla)
    private static final List<String> ESTADOS =
            List.of(PROXIMAMENTE, PREESTRENO, EN_CARTELERA, REESTRENO, FUERA_DE_CARTELERA);
    public static final int MAX_SINOPSIS = 1000; // wireframe 17.1: «Máximo 1000 caracteres»

    private int idPelicula;
    private String titulo;
    private String sinopsis;
    private int duracion; // en minutos, debe ser > 0
    private String clasificacionEdad;
    private String trailer; // enlace al tráiler
    private String poster; // enlace a la imagen del póster
    private DateTime fechaEstreno; // solo fecha: DateTime(dia, mes, anio)
    private String estado;
    /** Géneros de la película; no se agregan dos veces géneros con el mismo identificador. */
    private List<Genero> generos;

    public Pelicula(int idPelicula, String titulo, String sinopsis, int duracion,
                    String clasificacionEdad, String trailer, String poster,
                    DateTime fechaEstreno, String estado) {
        if (duracion <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0");
        }
        this.idPelicula = idPelicula;
        this.titulo = validarTitulo(titulo);
        this.sinopsis = validarSinopsis(sinopsis);
        this.duracion = duracion;
        this.clasificacionEdad = validarClasificacion(clasificacionEdad);
        this.trailer = validarEnlace(trailer, "tráiler");
        this.poster = validarEnlace(poster, "póster");
        this.fechaEstreno = fechaEstreno;
        setEstado(estado != null ? estado : EN_CARTELERA);
        this.generos = new ArrayList<>();
    }

    /** Indica si la película se muestra en la cartelera actual: estrenada, en preestreno o reestrenada. */
    public boolean estaEnCartelera() {
        return EN_CARTELERA.equals(this.estado) || PREESTRENO.equals(this.estado) || REESTRENO.equals(this.estado);
    }

    /** Prepara una descripción legible con clasificación, duración, géneros y estreno. */
    public String obtenerDetalle() {
        StringBuilder nombresGeneros = new StringBuilder();
        for (int i = 0; i < generos.size(); i++) {
            if (i > 0) nombresGeneros.append(", ");
            nombresGeneros.append(generos.get(i).getNombre());
        }
        return String.format("%s [%s] - %d min | Géneros: %s | Estreno: %s | Estado: %s",
                titulo, clasificacionEdad, duracion,
                generos.isEmpty() ? "N/A" : nombresGeneros.toString(),
                fechaEstreno != null ? fechaEstreno.soloFecha() : "N/A",
                estado);
    }

    /**
     * Caso de uso "Actualizar película" (wireframe 17.1): cambia los datos que se muestran en cartelera y el
     * estado, con las mismas validaciones del constructor; si algo es inválido no cambia nada.
     * La duración no se cambia aquí porque las funciones ya programadas dependen de ella para calcular
     * su hora de fin y los cruces de horario. Que el estado nuevo sea posible con las funciones pendientes
     * lo revisa AdministracionService, que conoce las sedes.
     */
    public void actualizarInformacion(String titulo, String sinopsis, String clasificacionEdad,
                                      String trailer, String poster, DateTime fechaEstreno, String estado) {
        String nuevoTitulo = validarTitulo(titulo);
        String nuevaSinopsis = validarSinopsis(sinopsis);
        String nuevaClasificacion = validarClasificacion(clasificacionEdad);
        String nuevoTrailer = validarEnlace(trailer, "tráiler");
        String nuevoPoster = validarEnlace(poster, "póster");
        String nuevoEstado = validarEstado(estado);
        this.titulo = nuevoTitulo;
        this.sinopsis = nuevaSinopsis;
        this.clasificacionEdad = nuevaClasificacion;
        this.trailer = nuevoTrailer;
        this.poster = nuevoPoster;
        this.fechaEstreno = fechaEstreno;
        this.estado = nuevoEstado;
    }

    /** La sinopsis es opcional; como máximo MAX_SINOPSIS caracteres (en la BD es TEXT). */
    private static String validarSinopsis(String sinopsis) {
        if (sinopsis != null && sinopsis.length() > MAX_SINOPSIS) {
            throw new IllegalArgumentException("La sinopsis no puede superar " + MAX_SINOPSIS + " caracteres");
        }
        return sinopsis;
    }

    /** Solo se admiten los cinco estados de arriba (los mismos que ck_pelicula_estado en la BD). */
    private static String validarEstado(String estado) {
        if (!ESTADOS.contains(estado)) {
            throw new IllegalArgumentException("Estado de película inválido: " + estado);
        }
        return estado;
    }

    /** El título es obligatorio y en la BD es VARCHAR(150). */
    private static String validarTitulo(String titulo) {
        if (titulo == null || titulo.trim().isEmpty() || titulo.trim().length() > 150) {
            throw new IllegalArgumentException("El título es obligatorio (máx. 150 caracteres)");
        }
        return titulo.trim();
    }

    /** La clasificación es obligatoria y en la BD es VARCHAR(10). */
    private static String validarClasificacion(String clasificacionEdad) {
        if (clasificacionEdad == null || clasificacionEdad.trim().isEmpty()
                || clasificacionEdad.trim().length() > 10) {
            throw new IllegalArgumentException("Clasificación obligatoria (máx. 10 caracteres)");
        }
        return clasificacionEdad.trim();
    }

    /** Los enlaces son opcionales; en la BD trailer_url y poster_url son VARCHAR(255). */
    private static String validarEnlace(String enlace, String campo) {
        if (enlace != null && enlace.length() > 255) {
            throw new IllegalArgumentException("El enlace del " + campo + " no puede superar 255 caracteres");
        }
        return enlace;
    }

    /** Agrega el género si no se había agregado ya; un valor nulo se ignora. */
    public void agregarGenero(Genero genero) {
        if (genero == null) return;
        for (Genero g : generos) {
            if (g.getIdGenero() == genero.getIdGenero()) return; // ya está agregado
        }
        generos.add(genero);
    }

    /** Reemplaza los géneros por los indicados (sin repetir); una lista nula o vacía deja la película sin géneros. */
    public void cambiarGeneros(List<Genero> nuevos) {
        generos.clear();
        if (nuevos != null) {
            for (Genero g : nuevos) agregarGenero(g);
        }
    }

    /** Comprueba si tiene el género indicado; un valor null significa que no se aplica filtro. */
    public boolean tieneGenero(Genero genero) {
        if (genero == null) return true;
        for (Genero g : generos) {
            if (g.getIdGenero() == genero.getIdGenero()
                    || g.getNombre().equalsIgnoreCase(genero.getNombre())) {
                return true;
            }
        }
        return false;
    }

    /** Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una película nueva nace con id 0. */
    public void asignarId(int idPelicula) {
        this.idPelicula = Ids.asignar(this.idPelicula, idPelicula, "la película");
    }

    // Acceso a los datos de la película.
    public int getIdPelicula() { return idPelicula; }
    public String getTitulo() { return titulo; }
    public String getSinopsis() { return sinopsis; }
    public int getDuracion() { return duracion; }
    public String getClasificacionEdad() { return clasificacionEdad; }
    public String getTrailer() { return trailer; }
    public String getPoster() { return poster; }
    public DateTime getFechaEstreno() { return fechaEstreno; }
    public String getEstado() { return estado; }
    public List<Genero> getGeneros() { return new ArrayList<>(generos); } // copia: la lista interna no se modifica desde fuera

    /** Cambia el estado; solo admite los cinco estados de arriba (los mismos que ck_pelicula_estado en la BD). */
    public void setEstado(String estado) {
        this.estado = validarEstado(estado);
    }

    @Override
    public String toString() {
        return titulo;
    }
}
