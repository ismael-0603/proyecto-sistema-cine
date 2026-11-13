package ucsur.pe.demo;

import ucsur.pe.model.Activable;
import ucsur.pe.model.Cupon;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Descuento;
import ucsur.pe.model.Promocion;
import ucsur.pe.tipo.CalculoDescuento;
import ucsur.pe.tipo.CalculoMontoFijo;
import ucsur.pe.tipo.CalculoPorcentaje;
import ucsur.pe.tipo.FabricaCalculoDescuento;
import java.util.ArrayList;
import java.util.List;

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
        CalculoDescuento desdeBD = FabricaCalculoDescuento.crear("PORCENTAJE", 15);
        System.out.printf("tipo_descuento=%s, valor=%.0f -> %s descuenta S/ %.2f de S/ 100.00%n",
                desdeBD.getTipo(), desdeBD.getValor(), desdeBD.describir(), desdeBD.calcular(100.0));

        System.out.println("\n--- DESACTIVAR ---");
        // Desactivar es tarea del administrador: se usa la interfaz Activable, no Descuento.
        List<Activable> administrables = List.of(cupon, promo);
        for (Activable a : administrables) {
            a.desactivar();
        }
        for (Descuento d : descuentos) {
            System.out.printf("%s: vigente=%b, descuento=S/ %.2f%n",
                    d, d.validarVigencia(hoy), d.calcularDescuento(100.0));
        }
    }
}
