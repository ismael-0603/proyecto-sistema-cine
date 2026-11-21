package ucsur.pe.service;

import ucsur.pe.exception.ReglaNegocioException;
import ucsur.pe.model.Empleado;
import ucsur.pe.model.Funcion;

/**
 * Por qué existe: las validaciones de permiso que comparten los cinco servicios de administración
 * (películas, funciones, productos, descuentos e infraestructura). Cada servicio la usa en vez de heredarla:
 * no "es un" PermisosAdmin, solo lo necesita. Es de paquete porque solo la usan los servicios.
 */
final class PermisosAdmin {

    private PermisosAdmin() { }

    /** Cuenta activa y permiso ADMINISTRAR: un administrador desactivado ya no puede operar. */
    static void exigirAdmin(Empleado admin) {
        if (admin == null || !admin.esAdministradorActivo()) {
            throw new ReglaNegocioException("Solo un administrador puede realizar esta operación");
        }
    }

    /** El administrador solo gestiona las funciones de la sede donde trabaja. */
    static void exigirFuncionDeSuSede(Empleado admin, Funcion funcion) {
        exigirAdmin(admin);
        if (funcion == null) throw new IllegalArgumentException("La función es obligatoria");
        if (!funcion.getSala().getSede().equals(admin.getSede())) {
            throw new ReglaNegocioException("La función es de otra sede; el administrador gestiona " + admin.getSede().getNombre());
        }
    }
}
