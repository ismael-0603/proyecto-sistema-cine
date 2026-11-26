package ucsur.pe.vista;

import javafx.application.Platform;
import javafx.fxml.FXML;
import ucsur.pe.model.Empleado;
import ucsur.pe.service.Sesion;

/**
 * Por qué existe: reúne lo común a las pantallas de administración (encabezado en modo panel, menú lateral
 * colapsable y el empleado administrador de la sesión requerido por los controllers de gestión).
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
        Runnable alternar = () -> {
            boolean nuevoOculto = !estado().isMenuAdminOculto();
            estado().setMenuAdminOculto(nuevoOculto);
            menuController.aplicarVisibilidad(!nuevoOculto);
            encabezadoController.actualizarBotonMenu(nuevoOculto);
        };
        menuController.aplicarVisibilidad(!estado().isMenuAdminOculto());
        encabezadoController.configurarMenuLateral(estado().isMenuAdminOculto(), alternar);
        return true;
    }
}
