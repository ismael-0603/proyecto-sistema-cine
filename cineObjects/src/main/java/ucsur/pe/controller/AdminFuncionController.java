package ucsur.pe.controller;

import ucsur.pe.model.Compra;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.model.Sala;
import ucsur.pe.service.FuncionAdminService;
import ucsur.pe.service.ResultadoCancelacion;
import ucsur.pe.util.ConversorDinero;
import java.util.List;

/** Acciones del administrador sobre las funciones de su sede (wireframe 18): programar, editar, cancelar y reactivar. */
public class AdminFuncionController extends ControllerBase {
    private final FuncionAdminService servicio;

    public AdminFuncionController(FuncionAdminService servicio) {
        this.servicio = servicio;
    }

    public Respuesta<List<Funcion>> listarFunciones(Empleado admin) {
        return ejecutar("", () -> servicio.listarFunciones(admin));
    }

    public Respuesta<Funcion> programarFuncion(Empleado admin, Pelicula pelicula, Sala sala, DateTime fecha,
                                               DateTime hora, String idioma) {
        return ejecutar("Función programada", () -> servicio.programarFuncion(admin, pelicula, sala, fecha, hora, idioma));
    }

    public Respuesta<Void> editarFuncion(Empleado admin, Funcion funcion, DateTime fecha, DateTime hora, String idioma) {
        return ejecutar("Función actualizada", () -> servicio.editarFuncion(admin, funcion, fecha, hora, idioma));
    }

    /** Cancela la función y reembolsa automáticamente a quienes compraron; el mensaje resume lo ocurrido. */
    public Respuesta<ResultadoCancelacion> cancelarFuncion(Empleado admin, Funcion funcion) {
        Respuesta<ResultadoCancelacion> r = ejecutar("Función cancelada", () -> servicio.cancelarFuncion(admin, funcion));
        return r.ok() ? Respuesta.exito("Función cancelada: " + r.dato(), r.dato()) : r;
    }

    /** «Reintentar» del aviso de cancelación: vuelve a pedir el reembolso de una compra pendiente. */
    public Respuesta<Long> reintentarReembolso(Empleado admin, Compra compra) {
        Respuesta<Long> r = ejecutar("", () -> servicio.reintentarReembolso(admin, compra));
        return r.ok() ? Respuesta.exito("Reembolso hecho: " + ConversorDinero.formatear(r.dato()), r.dato()) : r;
    }

    public Respuesta<Void> reactivarFuncion(Empleado admin, Funcion funcion) {
        return ejecutar("Función reactivada", () -> servicio.reactivarFuncion(admin, funcion));
    }
}
