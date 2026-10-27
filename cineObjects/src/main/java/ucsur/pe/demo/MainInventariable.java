package ucsur.pe.demo;

import ucsur.pe.cineInterfaz.Inventariable;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.Sede;

/**
 * Por qué existe: demo de abstracción. El stock por sede se maneja solo con Inventariable.
 */
public class MainInventariable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ INVENTARIABLE ===");

        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);
        Sede otraSede = new Sede(2, "Cine Centro", "Jr. Union 456", "Cercado", null, null, Sede.ACTIVA);

        Inventariable canchita = new Producto(1, "Canchita mediana", "Canchita para compartir",
                "Canchitas", 17.00, false, true);
        canchita.reponerStock(sede, 5);

        System.out.println("Hay 3 unidades: " + canchita.verificarStock(sede, 3));
        canchita.descontarStock(sede, 3);
        System.out.println("Hay 3 unidades despues de descontar 3: " + canchita.verificarStock(sede, 3));
        System.out.println("Otra sede sin stock: " + canchita.verificarStock(otraSede, 1));

        try {
            canchita.descontarStock(sede, 3);
        } catch (IllegalStateException ex) {
            System.out.println("Motivo del rechazo: " + ex.getMessage());
        }

        canchita.reponerStock(sede, 10);
        System.out.println("Hay 3 unidades tras reponer 10: " + canchita.verificarStock(sede, 3));
    }
}
