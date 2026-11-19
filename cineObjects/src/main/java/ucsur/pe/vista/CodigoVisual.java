package ucsur.pe.vista;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.Random;

/**
 * Por qué existe: dibuja un recuadro con aspecto de código QR a partir de un texto (el código de la entrada
 * o la referencia del pago), siempre igual para el mismo texto. Es solo la imagen de la pantalla: no es un
 * QR real que se pueda escanear (para eso haría falta una librería como ZXing).
 */
final class CodigoVisual {

    private static final int MODULOS = 25;
    private static final Color AZUL = Color.web("#03132c");

    private CodigoVisual() { }

    static Canvas dibujar(String texto, double tamano) {
        Canvas lienzo = new Canvas(tamano, tamano);
        GraphicsContext g = lienzo.getGraphicsContext2D();
        double m = tamano / MODULOS;
        Random azar = new Random(texto == null ? 0 : texto.hashCode());
        g.setFill(AZUL);
        for (int f = 0; f < MODULOS; f++) {
            for (int c = 0; c < MODULOS; c++) {
                if (enEsquina(f, c)) continue;
                if (azar.nextInt(100) < 45) g.fillRoundRect(c * m, f * m, m * 0.9, m * 0.9, m * 0.5, m * 0.5);
            }
        }
        esquina(g, 0, 0, m);
        esquina(g, 0, MODULOS - 7, m);
        esquina(g, MODULOS - 7, 0, m);
        return lienzo;
    }

    private static boolean enEsquina(int f, int c) {
        boolean arriba = f < 8;
        boolean izquierda = c < 8;
        boolean derecha = c >= MODULOS - 8;
        boolean abajo = f >= MODULOS - 8;
        return (arriba && izquierda) || (arriba && derecha) || (abajo && izquierda);
    }

    /** Los tres cuadros de las esquinas que hacen reconocible a un QR. */
    private static void esquina(GraphicsContext g, int fila, int col, double m) {
        double x = col * m;
        double y = fila * m;
        g.setFill(AZUL);
        g.fillRect(x, y, 7 * m, 7 * m);
        g.setFill(Color.WHITE);
        g.fillRect(x + m, y + m, 5 * m, 5 * m);
        g.setFill(AZUL);
        g.fillRect(x + 2 * m, y + 2 * m, 3 * m, 3 * m);
    }
}
