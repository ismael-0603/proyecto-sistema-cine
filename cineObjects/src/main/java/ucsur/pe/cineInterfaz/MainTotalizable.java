package ucsur.pe.cineInterfaz;

import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.Pedido;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.DetallePedido;
import ucsur.pe.cineobjects.Sede;

/**
 * Por qué existe: demo de polimorfismo. Pedido y Compra calculan su total como Totalizable.
 */
public class MainTotalizable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ TOTALIZABLE ===");

        // 1. Creamos un cliente invitado
        Cliente cliente1 = Cliente.ingresarComoInvitado("invitado@cine.com");

        // 2. Creamos un pedido de dulceria
        Pedido pedido1 = new Pedido(1);

        // 3. Creamos la sede y los productos con sus precios y stock en esa sede
        Sede sede = new Sede(1, "Cine Prueba", "Av. Lima 123", "Miraflores", null, null, Sede.ACTIVA);

        Producto producto1 = new Producto(
                1, "Canchita mediana(Salado)", "Canchita Mediana",
                "Canchitas", 17.00, false, true
        );
        producto1.reponerStock(sede, 20);

        Producto producto2 = new Producto(
                2, "CCBebida Mediana", "CCBebida Mediana(21oz)",
                "Bebidas", 13.00, false, true
        );
        producto2.reponerStock(sede, 30);

        // 4. Creamos los detalles indicando cantidades
        DetallePedido detalle1 = new DetallePedido(
                1, producto1, 2, null
        );

        DetallePedido detalle2 = new DetallePedido(
                2, producto2, 1, null
        );

        // 5. Agregamos los detalles al pedido
        pedido1.agregarDetalle(detalle1);
        pedido1.agregarDetalle(detalle2);

        // 6. Mostramos el contenido del pedido
        System.out.println("\n--- DETALLE DEL PEDIDO ---");
        System.out.println(detalle1);
        System.out.println(detalle2);

        System.out.printf(
                "Total del pedido: S/ %.2f%n",
                pedido1.calcularTotal()
        );

        // 7. Creamos la compra del cliente en la sede
        Compra compra1 = cliente1.realizarCompra(sede);

        // 8. Asociamos el pedido a la compra
        compra1.asignarPedido(pedido1);

        // 9. Creamos una lista de objetos Totalizable
        List<Totalizable> listaTotales = new ArrayList<>();

        listaTotales.add(pedido1);
        listaTotales.add(compra1);

        // 10. Aplicamos el polimorfismo
        System.out.println("\n--- TOTALIZABLE ---");

        for (Totalizable t : listaTotales) {

            System.out.printf(
                    "Total de %s: S/ %.2f%n",
                    t.getClass().getSimpleName(),
                    t.calcularTotal()
            );
        }
    }
}
