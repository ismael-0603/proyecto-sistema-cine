package ucsur.pe.vista;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Funcion;
import ucsur.pe.model.Pelicula;
import ucsur.pe.util.ConversorDinero;
import java.net.URL;

/**
 * Por qué existe: convertir datos a texto o imagen es trabajo de la pantalla, no del modelo.
 * Reúne los formatos que se repiten en varias pantallas: dinero, fechas en español y el póster.
 * Las fechas salen siempre de DateTime (la clase propia del curso).
 */
public final class Formato {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final String[] MESES = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    private Formato() { }

    /** 4720 -> "S/ 47.20" */
    public static String dinero(long centavos) {
        return ConversorDinero.formatear(centavos);
    }

    /** "Lunes 16 de noviembre" */
    public static String fechaLarga(DateTime fecha) {
        return DIAS[fecha.diaDeLaSemana() - 1] + " " + fecha.getDia() + " de " + MESES[fecha.getMes() - 1];
    }

    /** "16 de noviembre de 2026" */
    public static String fechaConAnio(DateTime fecha) {
        return fecha.getDia() + " de " + MESES[fecha.getMes() - 1] + " de " + fecha.getAnio();
    }

    /** "Lun 16" */
    public static String diaCorto(DateTime fecha) {
        return DIAS[fecha.diaDeLaSemana() - 1].substring(0, 3) + " " + fecha.getDia();
    }

    /** "16/11/2026" */
    public static String fecha(DateTime fecha) {
        return String.format("%02d/%02d/%04d", fecha.getDia(), fecha.getMes(), fecha.getAnio());
    }

    /** "124 min" -> "2 h 4 min" */
    public static String duracion(int minutos) {
        int h = minutos / 60;
        int m = minutos % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    /** "CP Mall del Sur · Sala 5 · 2D Subtitulada · 16/11/2026 · 15:40" */
    public static String detalleFuncion(Funcion f) {
        return f.getSala().getSede().getNombre() + " · Sala " + f.getSala().getNumero() + " · "
                + f.getSala().getTipoSala().getFormato() + " " + capitalizar(f.getIdioma()) + " · "
                + fecha(f.getFecha()) + " · " + f.getHoraInicio().soloHora();
    }

    /** [2] -> "Válida los martes"; [1..7] o vacía -> "Válida todos los días"; [6, 7] -> "Válida sábado y domingo" */
    public static String diasAplicables(java.util.List<Integer> dias) {
        if (dias == null || dias.isEmpty() || dias.size() == 7) return "Válida todos los días";
        if (dias.size() == 1) {
            String d = DIAS[dias.get(0) - 1].toLowerCase();
            return "Válida los " + (d.endsWith("s") ? d : d + "s");
        }
        java.util.List<String> nombres = new java.util.ArrayList<>();
        for (int d : dias.stream().sorted().toList()) nombres.add(DIAS[d - 1].toLowerCase());
        String ultimos = nombres.remove(nombres.size() - 1);
        return "Válida " + String.join(", ", nombres) + " y " + ultimos;
    }

    // ---------- Leer lo que se escribe en los formularios (trabajo de la pantalla) ----------

    /** "16/11/2026" -> DateTime(16, 11, 2026); DateTime valida que la fecha exista. */
    public static DateTime leerFecha(String texto, String campo) {
        String limpio = texto == null ? "" : texto.replace(" ", "");
        String[] partes = limpio.split("[/-]");
        if (partes.length != 3) throw new IllegalArgumentException(campo + ": usa el formato dd/mm/aaaa.");
        try {
            return new DateTime(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]), Integer.parseInt(partes[2]));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + ": usa el formato dd/mm/aaaa.");
        }
    }

    /** "15:40" -> DateTime(15, 40) */
    public static DateTime leerHora(String texto, String campo) {
        String[] partes = (texto == null ? "" : texto.trim()).split(":");
        if (partes.length != 2) throw new IllegalArgumentException(campo + ": usa el formato hh:mm.");
        try {
            return new DateTime(Integer.parseInt(partes[0].trim()), Integer.parseInt(partes[1].trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + ": usa el formato hh:mm.");
        }
    }

    /** "124" -> 124 */
    public static int leerEntero(String texto, String campo) {
        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " debe ser un número entero.");
        }
    }

    /** "32.50" -> 3250 (con ConversorDinero, que valida el formato). */
    public static long leerDinero(String texto, String campo) {
        try {
            return ConversorDinero.aCentavos(texto);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(campo + ": " + e.getMessage());
        }
    }

    /** Valor de un descuento para mostrar: "20 %" o "S/ 10.00". */
    public static String valorDescuento(String tipo, long valor) {
        return "PORCENTAJE".equals(tipo) ? valor + " %" : dinero(valor);
    }

    /** "PORCENTAJE" -> "Porcentaje"; "MONTO_FIJO" -> "Monto fijo" */
    public static String tipoDescuento(String tipo) {
        return capitalizar(tipo);
    }

    /** Nivel de socio mínimo: 0 = todos. */
    public static String nivelSocio(int nivel) {
        return nivel <= 0 ? "Todos los clientes" : nivel == 1 ? "Cualquier socio" : "Socio nivel " + nivel + "+";
    }

    /** "16/11 15:40" */
    public static String fechaHoraCorta(DateTime f) {
        return String.format("%02d/%02d %s", f.getDia(), f.getMes(), f.soloHora());
    }

    /** "16/11/2026 18:05" */
    public static String fechaHora(DateTime f) {
        return fecha(f) + " " + f.soloHora();
    }

    /** Estado de la compra como lo ve el cliente. */
    public static String estadoCompra(String estado) {
        return switch (estado) {
            case "CONFIRMADA" -> "Pagada";
            case "REEMBOLSADA" -> "Reembolsada";
            case "CANCELADA" -> "Cancelada";
            case "EXPIRADA" -> "Expirada";
            case "EN_PROCESO" -> "En proceso";
            default -> capitalizar(estado);
        };
    }

    /** Estado del reclamo como lo ve el cliente. */
    public static String estadoReclamo(String estado) {
        return switch (estado) {
            case "REGISTRADO" -> "Registrado";
            case "EN_REVISION" -> "En revisión";
            case "RESUELTO" -> "Respondido";
            case "RECHAZADO" -> "Rechazado";
            default -> capitalizar(estado);
        };
    }

    /** Tipo de reclamo como aparece en los wireframes 13.1 y 14. */
    public static String tipoReclamo(String tipo) {
        return switch (tipo) {
            case "ATENCION" -> "Atención";
            case "PRODUCTO_DULCERIA" -> "Producto de dulcería";
            case "SALA_PROYECCION" -> "Sala o proyección";
            default -> capitalizar(tipo);
        };
    }

    /** 1335 -> "1 335" */
    public static String miles(int n) {
        return String.format("%,d", n).replace(',', ' ').replace('.', ' ');
    }

    /** "ana.torres@correo.com" -> "a•••••@correo.com" (para no mostrar el correo completo en pantalla). */
    public static String correoOculto(String correo) {
        if (correo == null || !correo.contains("@")) return correo;
        int arroba = correo.indexOf('@');
        return correo.charAt(0) + "•••••" + correo.substring(arroba);
    }

    /** "SUBTITULADA" -> "Subtitulada" */
    public static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return "";
        String t = texto.trim().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /**
     * El póster de la película con el tamaño pedido. La BD guarda la ruta («posters/akira.jpg»), que se busca
     * dentro de los recursos del proyecto; si la imagen todavía no se agregó, se dibuja un póster con el título.
     */
    public static Node poster(Pelicula pelicula, double ancho, double alto) {
        return poster(pelicula.getTitulo(), pelicula.getPoster(), ancho, alto);
    }

    /** Igual que el anterior, con el título y la ruta sueltos (vista previa del formulario de película). */
    public static Node poster(String tituloPelicula, String ruta, double ancho, double alto) {
        Image imagen = cargarPoster(ruta);
        if (imagen != null && !imagen.isError()) {
            ImageView vista = new ImageView(imagen);
            vista.setFitWidth(ancho);
            vista.setFitHeight(alto);
            vista.setSmooth(true);
            // Llena todo el recuadro recortando lo que sobra (sin franjas vacías si el póster no es 2:3)
            if (imagen.getProgress() >= 1) recortarParaLlenar(vista, imagen, ancho, alto);
            else imagen.progressProperty().addListener((o, antes, ahora) -> {
                if (ahora.doubleValue() >= 1) recortarParaLlenar(vista, imagen, ancho, alto);
            });
            StackPane marco = new StackPane(vista);
            marco.setPrefSize(ancho, alto);
            marco.setMinSize(ancho, alto);
            marco.setMaxSize(ancho, alto);
            Rectangle esquinas = new Rectangle(ancho, alto);
            esquinas.setArcWidth(16);
            esquinas.setArcHeight(16);
            marco.setClip(esquinas);
            return marco;
        }
        Label titulo = new Label(tituloPelicula);
        titulo.getStyleClass().add("poster-reemplazo-titulo");
        titulo.setMaxWidth(ancho - 28);
        titulo.setAlignment(Pos.CENTER);
        StackPane reemplazo = new StackPane(titulo);
        reemplazo.getStyleClass().add("poster-reemplazo");
        reemplazo.setAlignment(Pos.CENTER);
        reemplazo.setPrefSize(ancho, alto);
        reemplazo.setMinSize(ancho, alto);
        reemplazo.setMaxSize(ancho, alto);
        return reemplazo;
    }

    /** Toma de la imagen el trozo central con la proporción del recuadro (como «cover» en CSS web). */
    private static void recortarParaLlenar(ImageView vista, Image imagen, double ancho, double alto) {
        double w = imagen.getWidth();
        double h = imagen.getHeight();
        if (w <= 0 || h <= 0) return;
        double proporcion = ancho / alto;
        double cw = w;
        double ch = w / proporcion;
        if (ch > h) {
            ch = h;
            cw = h * proporcion;
        }
        vista.setViewport(new javafx.geometry.Rectangle2D((w - cw) / 2, (h - ch) / 2, cw, ch));
    }

    private static Image cargarPoster(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) {
            return new Image(ruta, true); // de internet: se carga en segundo plano
        }
        if (ruta.startsWith("file:")) {
            return new Image(ruta); // archivo elegido en el formulario de película
        }
        URL recurso = Formato.class.getResource("/ucsur/pe/" + ruta);
        return recurso == null ? null : new Image(recurso.toExternalForm()); // del proyecto: al instante
    }
}
