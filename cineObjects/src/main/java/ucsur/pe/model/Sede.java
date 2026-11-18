package ucsur.pe.model;

import java.util.ArrayList;
import java.util.List;
/**
 * Por qué existe: es un local del cine. Reúne sus salas y su cartelera y permite filtrarla.
 */
public class Sede {
    // Estados de una sede (constantes: se usan como Sede.NOMBRE y su valor no cambia).
    public static final String ACTIVA = "ACTIVA"; // abierta al público
    public static final String INACTIVA = "INACTIVA"; // cerrada
    public static final String EN_MANTENIMIENTO = "EN_MANTENIMIENTO"; // cerrada temporalmente por reparación

    private int idSede;
    private String nombre;
    private String direccion;
    private String distrito;
    private String telefono;
    private String horarioAtencion;
    private String estado; // en la BD es VARCHAR(20): ACTIVA, INACTIVA o EN_MANTENIMIENTO
    /** Salas que pertenecen a esta sede. */
    private List<Sala> salas;
    /** Todas las funciones programadas, incluso las agotadas, canceladas o fuera de venta. */
    private List<Funcion> cartelera;

    public Sede(int idSede, String nombre, String direccion, String distrito,
                String telefono, String horarioAtencion, String estado) {
        validarDatos(nombre, direccion, distrito, telefono, horarioAtencion);
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

    /** El administrador corrige los datos de la sede (RF-42); se validan igual que al crearla. */
    public void actualizarDatos(String nombre, String direccion, String distrito, String telefono, String horarioAtencion) {
        validarDatos(nombre, direccion, distrito, telefono, horarioAtencion);
        this.nombre = nombre.trim();
        this.direccion = direccion.trim();
        this.distrito = distrito.trim();
        this.telefono = telefono;
        this.horarioAtencion = horarioAtencion;
    }

    private static void validarDatos(String nombre, String direccion, String distrito, String telefono, String horarioAtencion) {
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
    }

    /** Agrega una sala propia y evita repetirla o duplicar su número dentro de la sede. */
    public void agregarSala(Sala sala) {
        if (sala == null) {
            throw new IllegalArgumentException("La sala no puede ser nula");
        }
        if (!sala.getSede().equals(this)) {
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

    /** Cambia número y formato de una sala propia sin repetir el número de otra sala de la sede (RF-39). */
    public void actualizarSala(Sala sala, int numero, TipoSala tipoSala) {
        if (sala == null || !salas.contains(sala)) {
            throw new IllegalArgumentException("La sala no pertenece a esta sede");
        }
        for (Sala s : salas) {
            if (s != sala && s.getNumero() == numero) {
                throw new IllegalArgumentException("Ya existe la sala número " + numero + " en esta sede");
            }
        }
        sala.actualizarDatos(numero, tipoSala);
    }

    /** Indica si alguna sala tiene funciones que aún no terminan ni fueron canceladas. */
    public boolean tieneFuncionesPendientes() {
        for (Sala s : salas) {
            if (s.tieneFuncionesPendientes()) return true;
        }
        return false;
    }

    /** Agrega una función propia después de validar el horario en su sala. */
    public void agregarFuncion(Funcion funcion) {
    if (funcion == null) {
        throw new IllegalArgumentException("La función no puede ser nula");
    }
    if (!funcion.getSala().getSede().equals(this)) {
        throw new IllegalArgumentException("La función se proyecta en una sala de otra sede");
    }
    if (!cartelera.contains(funcion)) {
        // La sala rechaza la función si se cruza con otra o con el tiempo de limpieza.
        funcion.getSala().agregarFuncion(funcion);
        cartelera.add(funcion);
    }
}

    /** Una sede solo opera si está ACTIVA. */
    public boolean estaOperativa() {
        return ACTIVA.equals(estado);
    }

    /** Devuelve salas activas; una sede inactiva no ofrece salas disponibles. */
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

    /** Devuelve las funciones vendibles en esta sede, con película y sala operativas. */
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

    /** Filtra por idioma, clasificación y género; cada parámetro nulo o vacío omite ese filtro. */
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
    
    /** Cambia el estado; solo admite ACTIVA, INACTIVA o EN_MANTENIMIENTO. */
    public void setEstado(String estado) {
        if (!ACTIVA.equals(estado) && !INACTIVA.equals(estado) && !EN_MANTENIMIENTO.equals(estado)) {
            throw new IllegalArgumentException("Estado de sede inválido: " + estado);
        }
        this.estado = estado;
    }

    /**
     * Asigna el id que generó la base (AUTO_INCREMENT) tras el INSERT; una sede nueva nace con id 0.
     * Ojo: equals y hashCode usan el id, así que la sede se guarda (y recibe su id) antes de usarla como clave
     * del stock de un producto; AdministracionService.registrarSede la guarda apenas la crea.
     */
    public void asignarId(int idSede) {
        this.idSede = Ids.asignar(this.idSede, idSede, "la sede");
    }

    public int getIdSede() { return idSede; }
    public String getNombre() { return nombre; }
    public String getDireccion() { return direccion; }
    public String getDistrito() { return distrito; }
    public String getTelefono() { return telefono; }
    public String getHorarioAtencion() { return horarioAtencion; }
    public String getEstado() { return estado; }
    public List<Sala> getSalas() { return new ArrayList<>(salas); }
    /** Devuelve una copia de las funciones programadas para proteger la lista interna. */
    public List<Funcion> getCartelera() { return new ArrayList<>(cartelera); }

    /**
     * Dos sedes son la misma si tienen el mismo id de la BD (por ejemplo, si se cargan dos veces).
     * Mientras no tengan id (0, aún no guardadas), solo es igual a sí misma.
     * Así el stock por sede de Producto no se pierde al volver a cargar la sede.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sede otra)) return false;
        return this.idSede > 0 && this.idSede == otra.idSede;
    }

    @Override
    public int hashCode() {
        return idSede > 0 ? Integer.hashCode(idSede) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return nombre + " (" + distrito + ")";
    }
}
