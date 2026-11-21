package ucsur.pe.controller;

import ucsur.pe.model.Empleado;
import ucsur.pe.model.Sala;
import ucsur.pe.model.Sede;
import ucsur.pe.model.TipoSala;
import ucsur.pe.service.InfraestructuraAdminService;
import java.util.List;

/** Acciones del administrador sobre sedes y salas (pantallas Sedes y Salas): registrar, editar y dar de baja. */
public class AdminInfraestructuraController extends ControllerBase {
    private final InfraestructuraAdminService servicio;

    public AdminInfraestructuraController(InfraestructuraAdminService servicio) {
        this.servicio = servicio;
    }

    // ---------- Sedes ----------
    public Respuesta<List<Sede>> listarSedes(Empleado admin) {
        return ejecutar("", () -> servicio.listarSedes(admin));
    }

    public Respuesta<Sede> registrarSede(Empleado admin, String nombre, String direccion, String distrito,
                                         String telefono, String horario) {
        return ejecutar("Sede registrada", () -> servicio.registrarSede(admin, nombre, direccion, distrito, telefono, horario));
    }

    public Respuesta<Void> actualizarSede(Empleado admin, Sede sede, String nombre, String direccion, String distrito,
                                          String telefono, String horario) {
        return ejecutar("Sede actualizada", () -> servicio.actualizarSede(admin, sede, nombre, direccion, distrito, telefono, horario));
    }

    public Respuesta<Void> cambiarEstadoSede(Empleado admin, Sede sede, String estado) {
        return ejecutar("Estado de la sede: " + estado, () -> servicio.cambiarEstadoSede(admin, sede, estado));
    }

    public Respuesta<Void> darDeBajaSede(Empleado admin, Sede sede) {
        return ejecutar("Sede dada de baja", () -> servicio.darDeBajaSede(admin, sede));
    }

    // ---------- Salas ----------
    public Respuesta<List<TipoSala>> listarTiposSala(Empleado admin) {
        return ejecutar("", () -> servicio.listarTiposSala(admin));
    }

    public Respuesta<List<Sala>> listarSalas(Empleado admin, Sede sede) {
        return ejecutar("", () -> servicio.listarSalas(admin, sede));
    }

    public Respuesta<Sala> registrarSala(Empleado admin, Sede sede, int numero, TipoSala tipoSala,
                                         int filas, int lateral, int central) {
        return ejecutar("Sala registrada", () -> servicio.registrarSala(admin, sede, numero, tipoSala, filas, lateral, central));
    }

    public Respuesta<Void> actualizarSala(Empleado admin, Sala sala, int numero, TipoSala tipoSala) {
        return ejecutar("Sala actualizada", () -> servicio.actualizarSala(admin, sala, numero, tipoSala));
    }

    public Respuesta<Void> redistribuirSala(Empleado admin, Sala sala, int filas, int lateral, int central) {
        return ejecutar("Butacas redistribuidas", () -> servicio.redistribuirSala(admin, sala, filas, lateral, central));
    }

    public Respuesta<Void> cambiarEstadoSala(Empleado admin, Sala sala, String estado) {
        return ejecutar("Estado de la sala: " + estado, () -> servicio.cambiarEstadoSala(admin, sala, estado));
    }

    public Respuesta<Void> darDeBajaSala(Empleado admin, Sala sala) {
        return ejecutar("Sala dada de baja", () -> servicio.darDeBajaSala(admin, sala));
    }
}
