package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import ucsur.pe.repository.SalaRepository;
import ucsur.pe.repository.SedeRepository;
import java.util.List;

import static ucsur.pe.service.PermisosAdmin.exigirAdmin;

/**
 * Por qué existe: casos de uso del administrador sobre la infraestructura del cine: sedes (RF-42) y salas
 * con su mapa de butacas (RF-39). «Eliminar» es una baja lógica (INACTIVA): las funciones y compras antiguas
 * siguen apuntando a la sede o sala, y para sacarlas de uso no deben tener funciones pendientes.
 */
public class InfraestructuraAdminService {
    private final SedeRepository sedes;
    private final SalaRepository salas;

    public InfraestructuraAdminService(SedeRepository sedes, SalaRepository salas) {
        this.sedes = sedes;
        this.salas = salas;
    }

    // ---------- Sedes ----------
    public Sede registrarSede(Empleado admin, String nombre, String direccion, String distrito,
                              String telefono, String horarioAtencion) {
        exigirAdmin(admin);
        exigirNombreSedeLibre(nombre, null);
        Sede sede = new Sede(0, nombre, direccion, distrito, telefono, horarioAtencion, Sede.ACTIVA);
        sedes.guardar(sede); // la base le genera el id (AUTO_INCREMENT)
        return sede;
    }

    public void actualizarSede(Empleado admin, Sede sede, String nombre, String direccion, String distrito,
                               String telefono, String horarioAtencion) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        exigirNombreSedeLibre(nombre, sede);
        sede.actualizarDatos(nombre, direccion, distrito, telefono, horarioAtencion);
        sedes.guardar(sede);
    }

    private void exigirNombreSedeLibre(String nombre, Sede propia) {
        if (nombre == null || nombre.isBlank()) return;
        String buscado = nombre.trim();
        for (Sede s : sedes.listarTodas()) {
            if (!s.equals(propia) && s.getNombre().equalsIgnoreCase(buscado)) {
                throw new ReglaNegocioException("Ya existe una sede con el nombre " + buscado);
            }
        }
    }

    /** Abre, cierra o pone en mantenimiento una sede; para cerrarla no debe tener funciones pendientes. */
    public void cambiarEstadoSede(Empleado admin, Sede sede, String estado) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (!Sede.ACTIVA.equals(estado) && sede.tieneFuncionesPendientes()) {
            throw new ReglaNegocioException("La sede tiene funciones pendientes; cancélelas antes de cerrarla");
        }
        sede.setEstado(estado);
        sedes.guardar(sede);
    }

    /** «Eliminar» sede: baja lógica (INACTIVA). */
    public void darDeBajaSede(Empleado admin, Sede sede) {
        cambiarEstadoSede(admin, sede, Sede.INACTIVA);
    }

    public List<Sede> listarSedes(Empleado admin) {
        exigirAdmin(admin);
        return sedes.listarTodas();
    }

    // ---------- Salas ----------
    /**
     * Crea una sala con su mapa de butacas: {@code filas} filas de {@code lateral} | {@code central} | {@code lateral}
     * butacas separadas por dos pasillos. La capacidad es el total de butacas.
     */
    public Sala registrarSala(Empleado admin, Sede sede, int numero, TipoSala tipoSala,
                              int filas, int lateral, int central) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        if (filas <= 0 || lateral < 0 || central <= 0) {
            throw new IllegalArgumentException("Indique filas, butacas laterales y butacas centrales válidas");
        }
        Sala sala = new Sala(0, numero, filas * (2 * lateral + central), Sala.ACTIVA, sede, tipoSala);
        sala.generarButacas(filas, lateral, central);
        sede.agregarSala(sala); // rechaza un número de sala repetido en la sede
        try {
            salas.guardar(sala);
            sedes.guardar(sede);
        } catch (RuntimeException e) {
            sede.quitarSala(sala);
            throw e;
        }
        return sala;
    }

    public void actualizarSala(Empleado admin, Sala sala, int numero, TipoSala tipoSala) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        sala.getSede().actualizarSala(sala, numero, tipoSala);
        salas.guardar(sala);
    }

    /** Cambia filas y anchos de bloque (y la capacidad) de una sala que todavía no tiene funciones. */
    public void redistribuirSala(Empleado admin, Sala sala, int filas, int lateral, int central) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        sala.redistribuir(filas, lateral, central);
        salas.guardar(sala);
    }

    /** Activa, desactiva o pone en mantenimiento una sala; para sacarla de uso no debe tener funciones pendientes. */
    public void cambiarEstadoSala(Empleado admin, Sala sala, String estado) {
        exigirAdmin(admin);
        if (sala == null) throw new IllegalArgumentException("La sala es obligatoria");
        if (!Sala.ACTIVA.equals(estado) && sala.tieneFuncionesPendientes()) {
            throw new ReglaNegocioException("La sala tiene funciones pendientes; cancélelas antes de sacarla de uso");
        }
        sala.setEstado(estado);
        salas.guardar(sala);
    }

    /** «Eliminar» sala: baja lógica (INACTIVA). */
    public void darDeBajaSala(Empleado admin, Sala sala) {
        cambiarEstadoSala(admin, sala, Sala.INACTIVA);
    }

    /** Formatos de sala (2D, 3D...) para el formulario de salas. */
    public List<TipoSala> listarTiposSala(Empleado admin) {
        exigirAdmin(admin);
        return salas.listarTiposSala();
    }

    public List<Sala> listarSalas(Empleado admin, Sede sede) {
        exigirAdmin(admin);
        if (sede == null) throw new IllegalArgumentException("La sede es obligatoria");
        return sede.getSalas();
    }
}
