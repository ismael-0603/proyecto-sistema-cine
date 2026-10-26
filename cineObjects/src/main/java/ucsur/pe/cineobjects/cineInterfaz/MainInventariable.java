package ucsur.pe.cineobjects.cineInterfaz;

import ucsur.pe.cineobjects.Producto;

/** Demuestra abstracción: el stock del producto se maneja solo a través de Inventariable. */
public class MainInventariable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ INVENTARIABLE ===");

        Inventariable canchita = new Producto(1, "Canchita mediana", "Canchita para compartir",
                "Canchitas", 17.00, false, 5, true);

        System.out.println("Hay 3 unidades: " + canchita.verificarStock(3));
        canchita.descontarStock(3);
        System.out.println("Hay 3 unidades despues de descontar 3: " + canchita.verificarStock(3));

        try {
            canchita.descontarStock(3);
        } catch (IllegalStateException ex) {
            System.out.println("Motivo del rechazo: " + ex.getMessage());
        }

        canchita.reponerStock(10);
        System.out.println("Hay 3 unidades tras reponer 10: " + canchita.verificarStock(3));
    }
}
