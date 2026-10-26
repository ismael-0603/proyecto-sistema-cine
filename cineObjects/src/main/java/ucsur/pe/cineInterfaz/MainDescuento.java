package ucsur.pe.cineInterfaz;

import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cupon;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.DescuentoBase;
import ucsur.pe.cineobjects.Promocion;

/**
 * Por qué existe: demo de herencia y polimorfismo. Cupon y Promocion se usan como Descuento.
 */
public class MainDescuento {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ DESCUENTO ===");

        DateTime hoy = DateTime.ahora();
        DateTime inicio = new DateTime(hoy.getDia(), hoy.getMes(), hoy.getAnio());
        DateTime fin = inicio.sumarDias(30);

        Cupon cupon = new Cupon(1, "CINE10", DescuentoBase.PORCENTAJE, 10, inicio, fin, 5, true);
        Promocion promo = new Promocion(1, "Promo 5 soles", "S/ 5 de descuento", DescuentoBase.MONTO_FIJO, 5,
                inicio, fin, false, null, true);

        List<Descuento> descuentos = new ArrayList<>();
        descuentos.add(cupon);
        descuentos.add(promo);

        System.out.println("\n--- DESCUENTO SOBRE S/ 100.00 ---");
        for (Descuento d : descuentos) {
            System.out.printf("%s: vigente=%b, descuento=S/ %.2f%n",
                    d, d.validarVigencia(hoy), d.calcularDescuento(100.0));
        }

        System.out.println("\n--- DESACTIVAR ---");
        for (Descuento d : descuentos) {
            d.desactivar();
            System.out.printf("%s: vigente=%b, descuento=S/ %.2f%n",
                    d, d.validarVigencia(hoy), d.calcularDescuento(100.0));
        }
    }
}
