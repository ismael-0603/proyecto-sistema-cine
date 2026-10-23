package ucsur.pe.cineobjects;

/**
 *
 * @author user
 */
public class Pelicula {
    private int idPelicula;
    private String titulo;
    private String sinopsis;
    private int duracion;
    private String clasificacionEdad;
    private String trailer;
    private String poster;
    private String estado; // Estreno, Preventa, eso :V
    
    public Pelicula(){
    }

    public Pelicula(int idPelicula, String titulo, String sinopsis, int duracion, String clasificacionEdad, String trailer, String poster, String estado) {
        this.idPelicula = idPelicula;
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.duracion = duracion;
        this.clasificacionEdad = clasificacionEdad;
        this.trailer = trailer;
        this.poster = poster;
        this.estado = estado;
    }

    public int getIdPelicula() {
        return idPelicula;
    }

    public void setIdPelicula(int idPelicula) {
        this.idPelicula = idPelicula;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public int getDuracion() {
        return duracion;
    }

    public void setDuracion(int duracion) {
        this.duracion = duracion;
    }

    public String getClasificacionEdad() {
        return clasificacionEdad;
    }

    public void setClasificacionEdad(String clasificacionEdad) {
        this.clasificacionEdad = clasificacionEdad;
    }

    public String getTrailer() {
        return trailer;
    }

    public void setTrailer(String trailer) {
        this.trailer = trailer;
    }

    public String getPoster() {
        return poster;
    }

    public void setPoster(String poster) {
        this.poster = poster;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

}
