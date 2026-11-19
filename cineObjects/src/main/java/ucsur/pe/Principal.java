package ucsur.pe;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import ucsur.pe.correo.CorreoSimulado;
import ucsur.pe.db.DBConnection;
import ucsur.pe.exception.RepositoryException;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.pasarela.PasarelaSimulada;
import ucsur.pe.vista.Navegador;
import java.io.InputStream;

/**
 * Por qué existe: es la entrada de la interfaz gráfica, como App.start en el ejemplo MVCone.
 * Abre la conexión, deja que App arme repositorios, servicios y controllers con la base DB_Cine,
 * carga las fuentes del diseño y muestra la primera pantalla. Al cerrar la ventana cierra la conexión.
 * App sigue siendo el único lugar que decide qué implementaciones se usan.
 */
public class Principal extends Application {

    /** Pesos de Manrope que usa el diseño; cada uno queda como su propia familia ("Manrope SemiBold"...). */
    private static final String[] FUENTES = {"Regular", "Medium", "SemiBold", "Bold", "ExtraBold"};

    private DBConnection db;

    @Override
    public void start(Stage stage) {
        cargarFuentes();
        App app;
        try {
            // 1. Conexión y aplicación con MySQL: cada repositorio carga sus datos al crearse.
            db = DBConnection.getInstance();
            Reloj reloj = new RelojSistema();
            app = new App(db.getConexion(), reloj, new PasarelaSimulada(reloj), new CorreoSimulado());
        } catch (RepositoryException e) {
            Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage()
                    + "\n\nRevise que XAMPP (MySQL) esté encendido y que db.properties tenga los datos correctos.");
            error.setHeaderText("No se pudo abrir el kiosco");
            error.showAndWait();
            Platform.exit();
            return;
        }

        // 2. El navegador muestra las pantallas en esta misma ventana.
        Navegador navegador = new Navegador(stage, app, getHostServices());
        stage.setTitle("Cineplanet · Autoservicio");
        InputStream icono = Principal.class.getResourceAsStream("img/logo-emblema.png");
        if (icono != null) stage.getIcons().add(new Image(icono));
        stage.setMinWidth(1280);
        stage.setMinHeight(720);
        stage.setMaximized(true);
        navegador.empezar();
        stage.show();
    }

    @Override
    public void stop() {
        // 3. Cerrar la conexión al salir (como setOnCloseRequest en MVCone).
        if (db != null) {
            try {
                db.cerrar();
            } catch (RepositoryException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static void cargarFuentes() {
        for (String peso : FUENTES) {
            try (InputStream in = Principal.class.getResourceAsStream("fonts/Manrope-" + peso + ".ttf")) {
                if (in != null) Font.loadFont(in, 14);
            } catch (Exception e) {
                System.err.println("No se pudo cargar Manrope " + peso + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
