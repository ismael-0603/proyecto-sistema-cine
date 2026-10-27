package ucsur.pe.demo;

import java.util.ArrayList;
import java.util.List;
import ucsur.pe.cineInterfaz.CalculoDescuento;
import ucsur.pe.cineInterfaz.Descuento;

import ucsur.pe.cineobjects.CalculoMontoFijo;
import ucsur.pe.cineobjects.CalculoPorcentaje;
import ucsur.pe.cineobjects.Cupon;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.DescuentoBase;
import ucsur.pe.cineobjects.Promocion;

/**
 * Por qué existe: demo de herencia y polimorfismo. Cupon y Promocion se usan como Descuento,
 * y cada forma de cálculo (porcentaje o monto fijo) descuenta a su manera.
 */
public class MainDescuento {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ DESCUENTO ===");

        DateTime hoy = DateTime.ahora();
        DateTime inicio = new DateTime(hoy.getDia(), hoy.getMes(), hoy.getAnio());
        DateTime fin = inicio.sumarDias(30);

        Cupon cupon = new Cupon(1, "CINE10", new CalculoPorcentaje(10), inicio, fin, 5, true);
        Promocion promo = new Promocion(1, "Promo 5 soles", "S/ 5 de descuento", new CalculoMontoFijo(5),
                inicio, fin, false, null, true);

        List<Descuento> descuentos = new ArrayList<>();
        descuentos.add(cupon);
        descuentos.add(promo);

        System.out.println("\n--- DESCUENTO SOBRE S/ 100.00 ---");
        for (Descuento d : descuentos) {
            System.out.printf("%s: vigente=%b, descuento=S/ %.2f%n",
                    d, d.validarVigencia(hoy), d.calcularDescuento(100.0));
        }

        System.out.println("\n--- FORMA DE CALCULO LEIDA DE LA BD ---");
        CalculoDescuento desdeBD = DescuentoBase.crearCalculo("PORCENTAJE", 15);
        System.out.printf("tipo_descuento=%s, valor=%.0f -> %s descuenta S/ %.2f de S/ 100.00%n",
                desdeBD.getTipo(), desdeBD.getValor(), desdeBD.describir(), desdeBD.calcular(100.0));

        System.out.println("\n--- DESACTIVAR ---");
        for (Descuento d : descuentos) {
            d.desactivar();
            System.out.printf("%s: vigente=%b, descuento=S/ %.2f%n",
                    d, d.validarVigencia(hoy), d.calcularDescuento(100.0));
        }
    }
}
