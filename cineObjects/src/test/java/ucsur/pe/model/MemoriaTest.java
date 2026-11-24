package ucsur.pe.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prueba de rendimiento / memoria (atributo de calidad: eficiencia de desempeño).
 *
 * Usa Runtime.getRuntime(), la API nativa de Java, sin librerías extra.
 *
 * Metodología (igual que el ejemplo MVCone):
 *   1. Forzar el GC para limpiar basura antes de medir.
 *   2. Leer la memoria usada ANTES de crear objetos.
 *   3. Crear N objetos y mantenerlos en una List (si no, el GC los elimina).
 *   4. Leer la memoria usada DESPUÉS.
 *   5. Calcular el delta y el promedio por objeto.
 *
 * A diferencia del ejemplo, cada prueba tiene un UMBRAL con assertTrue: no solo mide,
 * también pasa o falla según un criterio medible. El umbral es holgado a propósito, porque
 * la medición es aproximada (la JVM reserva memoria extra y la basura temporal de los String
 * creados en el bucle también cuenta) y varía entre computadoras.
 *
 * Para ver los números: ejecutar las pruebas y revisar la ventana Output de NetBeans.
 */
@DisplayName("Pruebas de memoria: creación de objetos del modelo")
class MemoriaTest {

    // Cantidad de objetos a crear en cada medición
    private static final int CANTIDAD = 10_000;

    // Umbrales (bytes promedio por objeto). Ver el comentario de la clase.
    private static final double UMBRAL_INDIVIDUAL  = 2048;
    private static final double UMBRAL_COMPARACION = 1024;

    // -------------------------------------------------------

    @Test
    @DisplayName("Memoria al crear " + CANTIDAD + " objetos TipoSala (umbral < 2048 bytes/obj)")
    void memoriaCreacionTipoSala() {
        Runtime rt = Runtime.getRuntime();

        // Arrange: limpiar basura antes de medir
        limpiarMemoria(rt);
        long antes = usada(rt);

        // Act: crear y retener los objetos
        List<TipoSala> lista = new ArrayList<>(CANTIDAD);
        for (int i = 0; i < CANTIDAD; i++) {
            lista.add(new TipoSala(i + 1, "Formato " + i, "Descripción " + i, 500 + i));
        }
        long despues = usada(rt);

        // Assert
        double promedio = imprimir("TipoSala", antes, despues, lista.size());
        assertEquals(CANTIDAD, lista.size());
        assertTrue(promedio < UMBRAL_INDIVIDUAL,
                "TipoSala ocupa " + promedio + " bytes/obj; el umbral es " + UMBRAL_INDIVIDUAL);
    }

    @Test
    @DisplayName("Memoria al crear " + CANTIDAD + " objetos Producto (umbral < 2048 bytes/obj)")
    void memoriaCreacionProducto() {
        Runtime rt = Runtime.getRuntime();

        limpiarMemoria(rt);
        long antes = usada(rt);

        List<Producto> lista = new ArrayList<>(CANTIDAD);
        for (int i = 0; i < CANTIDAD; i++) {
            lista.add(new Producto(i + 1, "Producto " + i, "Descripción " + i, "Categoría",
                    1000 + i, i % 2 == 0, true));
        }
        long despues = usada(rt);

        double promedio = imprimir("Producto", antes, despues, lista.size());
        assertEquals(CANTIDAD, lista.size());
        assertTrue(promedio < UMBRAL_INDIVIDUAL,
                "Producto ocupa " + promedio + " bytes/obj; el umbral es " + UMBRAL_INDIVIDUAL);
    }

    @Test
    @DisplayName("Comparación de memoria: TipoSala vs Producto (umbral < 1024 bytes/obj)")
    void comparacionMemoriaEntreEntidades() {
        Runtime rt = Runtime.getRuntime();

        // Aquí se usan textos literales (sin concatenar) para medir casi solo el objeto,
        // sin la basura temporal de los String que se arman en el bucle.

        // --- TipoSala ---
        limpiarMemoria(rt);
        long antes = usada(rt);
        List<TipoSala> tipos = new ArrayList<>(CANTIDAD);
        for (int i = 0; i < CANTIDAD; i++) {
            tipos.add(new TipoSala(i + 1, "3D", "Proyección 3D", 500));
        }
        long deltaTipoSala = usada(rt) - antes;

        // --- Producto ---
        limpiarMemoria(rt);
        antes = usada(rt);
        List<Producto> productos = new ArrayList<>(CANTIDAD);
        for (int i = 0; i < CANTIDAD; i++) {
            productos.add(new Producto(i + 1, "Canchita", "Canchita salada", "Canchita", 1250, false, true));
        }
        long deltaProducto = usada(rt) - antes;

        double promTipoSala = (double) deltaTipoSala / CANTIDAD;
        double promProducto = (double) deltaProducto / CANTIDAD;

        System.out.println("\n===== COMPARACIÓN DE MEMORIA (" + CANTIDAD + " objetos c/u) =====");
        System.out.printf("TipoSala : %s  (~%.2f bytes/obj)%n", formatear(deltaTipoSala), promTipoSala);
        System.out.printf("Producto : %s  (~%.2f bytes/obj)%n", formatear(deltaProducto), promProducto);
        System.out.println("Producto pesa más: tiene 10 atributos, uno de ellos un HashMap (stock por sede) que se crea en cada objeto;");
        System.out.println("TipoSala solo tiene 4 atributos.");
        System.out.println("Retenidos: " + tipos.size() + " / " + productos.size());

        assertTrue(promTipoSala < UMBRAL_COMPARACION, "TipoSala: " + promTipoSala + " bytes/obj");
        assertTrue(promProducto < UMBRAL_COMPARACION, "Producto: " + promProducto + " bytes/obj");
    }

    // -------------------------------------------------------

    private static void limpiarMemoria(Runtime rt) {
        rt.gc();
        Thread.yield();
        rt.gc();
    }

    private static long usada(Runtime rt) {
        return rt.totalMemory() - rt.freeMemory();
    }

    /** Imprime el reporte de una medición y devuelve el promedio en bytes por objeto. */
    private static double imprimir(String entidad, long antes, long despues, int retenidos) {
        long totalBytes = despues - antes;
        double promedio = (double) totalBytes / CANTIDAD;

        System.out.println("\n===== MEMORIA: " + entidad + " =====");
        System.out.println("Objetos creados     : " + CANTIDAD);
        System.out.println("Memoria antes       : " + formatear(antes));
        System.out.println("Memoria después     : " + formatear(despues));
        System.out.println("Delta total         : " + formatear(totalBytes));
        System.out.printf ("Promedio por objeto : %.2f bytes%n", promedio);
        System.out.println("Lista retenida      : " + retenidos + " objetos");
        return promedio;
    }

    /** Formatea bytes a KB o MB según el tamaño para facilitar la lectura. */
    private static String formatear(long bytes) {
        if (Math.abs(bytes) >= 1_048_576) {
            return String.format("%.2f MB (%d bytes)", bytes / 1_048_576.0, bytes);
        } else if (Math.abs(bytes) >= 1_024) {
            return String.format("%.2f KB (%d bytes)", bytes / 1_024.0, bytes);
        } else {
            return bytes + " bytes";
        }
    }
}
