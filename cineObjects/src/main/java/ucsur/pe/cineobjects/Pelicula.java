package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author user
 */
public class Pelicula {
    public static final String PROXIMAMENTE = "PROXIMAMENTE";
    public static final String EN_CARTELERA = "EN_CARTELERA";
    public static final String FUERA_DE_CARTELERA = "FUERA_DE_CARTELERA";

    private int idPelicula;
    private String titulo;
    private String sinopsis;
    private int duracion; // en minutos, debe ser > 0
    private String clasificacionEdad;
    private String trailer;
    private String poster;
    private DateTime fechaEstreno; // solo fecha: DateTime(dia, mes, anio)
    private String estado;
    private List<Genero> generos;

    public Pelicula(int idPelicula, String titulo, String sinopsis, int duracion,
                    String clasificacionEdad, String trailer, String poster,
                    DateTime fechaEstreno, String estado) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (duracion <= 0) {
            throw new IllegalArgumentException("La duración debe ser mayor a 0");
        }
        if (clasificacionEdad == null || clasificacionEdad.trim().isEmpty()
                || clasificacionEdad.trim().length() > 10) {
            throw new IllegalArgumentException("Clasificación obligatoria (máx. 10 caracteres)");
        }
        this.idPelicula = idPelicula;
        this.titulo = titulo.trim();
        this.sinopsis = sinopsis;
        this.duracion = duracion;
        this.clasificacionEdad = clasificacionEdad.trim();
        this.trailer = trailer;
        this.poster = poster;
        this.fechaEstreno = fechaEstreno;
        setEstado(estado != null ? estado : EN_CARTELERA);
        this.generos = new ArrayList<>();
    }

    public boolean estaEnCartelera() {
        return EN_CARTELERA.equals(this.estado);
    }

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

    public void agregarGenero(Genero genero) {
        if (genero == null) return;
        for (Genero g : generos) {
            if (g.getIdGenero() == genero.getIdGenero()) return; // ya está agregado
        }
        generos.add(genero);
    }

    // Si genero es null, no se filtra (devuelve true)
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

    // Getters
    public int getIdPelicula() { return idPelicula; }
    public String getTitulo() { return titulo; }
    public String getSinopsis() { return sinopsis; }
    public int getDuracion() { return duracion; }
    public String getClasificacionEdad() { return clasificacionEdad; }
    public String getTrailer() { return trailer; }
    public String getPoster() { return poster; }
    public DateTime getFechaEstreno() { return fechaEstreno; }
    public String getEstado() { return estado; }
    public List<Genero> getGeneros() { return generos; }

    public void setEstado(String estado) {
        if (!PROXIMAMENTE.equals(estado) && !EN_CARTELERA.equals(estado)
                && !FUERA_DE_CARTELERA.equals(estado)) {
            throw new IllegalArgumentException("Estado de película inválido: " + estado);
        }
        this.estado = estado;
    }

    @Override
    public String toString() {
        return titulo;
    }
}
