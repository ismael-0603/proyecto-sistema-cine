package ucsur.pe.cineobjects;

import java.util.ArrayList;
import java.util.List;
/**
 *
 * @author user
 */
public class Sede {
    public static final String ACTIVA = "ACTIVA";
    public static final String INACTIVA = "INACTIVA";
    public static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO";

    private int idSede;
    private String nombre;
    private String direccion;
    private String distrito;
    private String telefono;
    private String horarioAtencion;
    private String estado; // en la BD es BOOLEAN: ACTIVA = 1, INACTIVA = 0
    private List<Sala> salas;
    private List<Funcion> cartelera;

    public Sede(int idSede, String nombre, String direccion, String distrito,
                String telefono, String horarioAtencion, String estado) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.trim().length() > 60) {
            throw new IllegalArgumentException("El nombre es obligatorio (máx. 60 caracteres)");
        }
        if (direccion == null || direccion.trim().isEmpty() || direccion.trim().length() > 150) {
            throw new IllegalArgumentException("La dirección es obligatoria (máx. 150 caracteres)");
        }
        if (distrito == null || distrito.trim().isEmpty() || distrito.trim().length() > 60) {
            throw new IllegalArgumentException("El distrito es obligatorio (máx. 60 caracteres)");
        }
        if (telefono != null && telefono.length() > 20) {
            throw new IllegalArgumentException("El teléfono no puede superar 20 caracteres");
        }
        if (horarioAtencion != null && horarioAtencion.length() > 100) {
            throw new IllegalArgumentException("El horario no puede superar 100 caracteres");
        }
        this.idSede = idSede;
        this.nombre = nombre.trim();
        this.direccion = direccion.trim();
        this.distrito = distrito.trim();
        this.telefono = telefono;
        this.horarioAtencion = horarioAtencion;
        this.salas = new ArrayList<>();
        this.cartelera = new ArrayList<>();
        setEstado(estado != null ? estado : ACTIVA);
    }

    public void agregarSala(Sala sala) {
        if (sala == null) {
            throw new IllegalArgumentException("La sala no puede ser nula");
        }
        if (sala.getSede() != this) {
            throw new IllegalArgumentException("La sala pertenece a otra sede");
        }
        for (Sala s : salas) {
            if (s == sala) return; // ya está agregada
            if (s.getNumero() == sala.getNumero()) {
                throw new IllegalArgumentException("Ya existe la sala número " + sala.getNumero() + " en esta sede");
            }
        }
        salas.add(sala);
    }

    public void agregarFuncion(Funcion funcion) {
        if (funcion == null) {
            throw new IllegalArgumentException("La función no puede ser nula");
        }
        if (funcion.getSala().getSede() != this) {
            throw new IllegalArgumentException("La función se proyecta en una sala de otra sede");
        }
        if (!cartelera.contains(funcion)) {
            cartelera.add(funcion);
        }
    }

    public boolean estaOperativa() {
        return ACTIVA.equals(estado);
    }

    public List<Sala> consultarSalasDisponibles() {
        List<Sala> disponibles = new ArrayList<>();
        if (!estaOperativa()) return disponibles;
        for (Sala s : salas) {
            if (s.estaOperativa()) {
                disponibles.add(s);
            }
        }
        return disponibles;
    }

    // Funciones disponibles para la venta, de películas en cartelera y en salas operativas
    public List<Funcion> consultarCartelera() {
        List<Funcion> activas = new ArrayList<>();
        if (!estaOperativa()) return activas;
        for (Funcion f : cartelera) {
            if (f.getSala().estaOperativa()
                    && f.getPelicula().estaEnCartelera()
                    && f.estaDisponible()) {
                activas.add(f);
            }
        }
        return activas;
    }

    // Un parámetro null o vacío significa "no filtrar por ese criterio"
    public List<Funcion> filtrarCartelera(String idioma, String clasificacion, Genero genero) {
        List<Funcion> filtradas = new ArrayList<>();
        for (Funcion f : consultarCartelera()) {
            boolean okIdioma = (idioma == null || idioma.isEmpty()
                    || f.getIdioma().equalsIgnoreCase(idioma));
            boolean okClasif = (clasificacion == null || clasificacion.isEmpty()
                    || f.getPelicula().getClasificacionEdad().equalsIgnoreCase(clasificacion));
            boolean okGenero = f.getPelicula().tieneGenero(genero);

            if (okIdioma && okClasif && okGenero) {
                filtradas.add(f);
            }
        }
        return filtradas;
    }
    
    public void setEstado(String estado) {
        if (!ACTIVA.equals(estado) && !INACTIVA.equals(estado) && !EN_MANTENIMIENTO.equals(estado)) {
            throw new IllegalArgumentException("Estado de sala inválido: " + estado);
        }
        this.estado = estado;
    }

    public int getIdSede() { return idSede; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getDistrito() { return distrito; }
    public String getTelefono() { return telefono; }
    public String getHorarioAtencion() { return horarioAtencion; }
    public String getEstado() { return estado; }
    public List<Sala> getSalas() { return new ArrayList<>(salas); }
    public List<Funcion> getCartelera() { return new ArrayList<>(cartelera); }

    @Override
    public String toString() {
        return nombre + " (" + distrito + ")";
    }
}
