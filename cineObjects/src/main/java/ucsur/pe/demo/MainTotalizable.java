package ucsur.pe.demo;

import ucsur.pe.util.ConversorDinero;
import ucsur.pe.model.CalculadoraImporte;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.Compra;
import ucsur.pe.model.DetallePedido;
import ucsur.pe.model.GestorPuntos;
import ucsur.pe.model.Pedido;
import ucsur.pe.model.Producto;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.model.Sede;
import ucsur.pe.model.SeleccionAsientos;
import ucsur.pe.model.Totalizable;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: demo de polimorfismo. Pedido y Compra calculan su total como Totalizable.
 */
public class MainTotalizable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ TOTALIZABLE ===");

        // 1. Creamos un cliente invitado
        Reloj reloj = new RelojSistema(); // en la demo no hay App: se crea el reloj aquí
        Cliente cliente1 = Cliente.ingresarComoInvitado("invitado@cine.com", reloj);

        // 2. Creamos la sede y los productos con sus precios y stock en esa sede
        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);

        Producto producto1 = new Producto(
                1, "Canchita mediana(Salado)", "Canchita Mediana",
                "Canchitas", ConversorDinero.aCentavos("17.00"), false, true
        );
        producto1.reponerStock(sede, 20);

        Producto producto2 = new Producto(
                2, "CCBebida Mediana", "CCBebida Mediana(21oz)",
                "Bebidas", ConversorDinero.aCentavos("13.00"), false, true
        );
        producto2.reponerStock(sede, 30);

        // 3. Creamos la compra del cliente en la sede (en el sistema la crea CompraService)
        Compra compra1 = new Compra(1, null, cliente1, sede,
                new SeleccionAsientos(), new CalculadoraImporte(), new GestorPuntos(), reloj);

        // 4. Creamos los detalles indicando cantidades
        DetallePedido detalle1 = new DetallePedido(
                1, producto1, 2, null
        );

        DetallePedido detalle2 = new DetallePedido(
                2, producto2, 1, null
        );

        // 5. Agregamos los detalles: la compra crea su pedido de dulcería con el primer producto
        compra1.agregarProducto(detalle1);
        compra1.agregarProducto(detalle2);
        Pedido pedido1 = compra1.getPedido();

        // 6. Mostramos el contenido del pedido
        System.out.println("\n--- DETALLE DEL PEDIDO ---");
        System.out.println(detalle1);
        System.out.println(detalle2);

        System.out.printf(
                "Total del pedido: %s%n",
                ConversorDinero.formatear(pedido1.calcularTotalCentavos())
        );

        // 7. Creamos una lista de objetos Totalizable
        List<Totalizable> listaTotales = new ArrayList<>();

        listaTotales.add(pedido1);
        listaTotales.add(compra1);

        // 8. Aplicamos el polimorfismo
        System.out.println("\n--- TOTALIZABLE ---");

        for (Totalizable t : listaTotales) {

            System.out.printf(
                    "Total de %s: %s%n",
                    t.getClass().getSimpleName(),
                    ConversorDinero.formatear(t.calcularTotalCentavos())
            );
        }
    }
}
