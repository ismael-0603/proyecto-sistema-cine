package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import ucsur.pe.model.Empleado;
import ucsur.pe.service.Sesion;

/**
 * Lo común a las pantallas de administración: el encabezado en modo panel, el menú lateral con la sección
 * marcada y el administrador de la sesión, que todas las acciones de los controllers Admin*Controller reciben.
 */
public abstract class PantallaAdmin extends Pantalla {

    @FXML protected EncabezadoVista encabezadoController;
    @FXML protected AdminMenuVista menuController;

    protected Empleado admin;

    protected PantallaAdmin(Navegador nav) {
        super(nav);
    }

    /** Prepara el marco; si no hay un administrador con sesión, manda a iniciar sesión y devuelve false. */
    protected boolean prepararAdmin(String seccion) {
        Sesion sesion = estado().getSesion();
        admin = sesion == null ? null : sesion.empleadoEnPanel();
        if (admin == null || !admin.esAdministrador()) {
            Platform.runLater(() -> nav.ir("iniciar-sesion"));
            return false;
        }
        encabezadoController.modoPanel("Administración", "Gestión del cine · "
                + (admin.getSede() == null ? "" : "Sede " + admin.getSede().getNombre()));
        menuController.marcar(seccion);
        return true;
    }
}
