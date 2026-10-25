/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ucsur.pe.cineobjects.cineInterfaz;

/**
 *
 * @author Usuario
 */
import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.Compra;
import ucsur.pe.cineobjects.Pedido;
import ucsur.pe.cineobjects.Producto;
import ucsur.pe.cineobjects.DetallePedido;

public class MainTotalizable {

    public static void main(String[] args) {

        System.out.println("=== INTERFAZ TOTALIZABLE ===");

        // 1. Creamos un cliente invitado
        Cliente cliente1 = Cliente.ingresarComoInvitado("invitado@cine.com");

        // 2. Creamos un pedido de dulceria
        Pedido pedido1 = new Pedido(1);

        // 3. Creamos los productos con sus precios y stock
        Producto producto1 = new Producto(
                1, "Canchita mediana(Salado)", "Canchita Mediana",
                "Canchitas", 17.00, false, 20, true
        );

        Producto producto2 = new Producto(
                2, "CCBebida Mediana", "CCBebida Mediana(21oz)",
                "Bebidas", 13.00, false, 30, true
        );

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

        // 7. Creamos la compra del cliente
        Compra compra1 = cliente1.realizarCompra();

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



/*

---------------- Código sin datos(el total saldrá cero por la falta de estos)---------------

public class MainTotalizable {

    public static void main(String[] args) {

        System.out.println("--- PROBANDO TOTALIZABLE ---");

        // 1. Creamos un cliente invitado
        Cliente cliente1 = Cliente.ingresarComoInvitado("invitado@cine.com");

        // 2. Creamos una compra para ese cliente
        Compra compra1 = cliente1.realizarCompra();

        // 3. Creamos un pedido de dulceria
        Pedido pedido1 = new Pedido(1);

        // 4. Asociamos el pedido a la compra
        compra1.asignarPedido(pedido1);

        // 5. Creamos una lista de objetos Totalizable
        List<Totalizable> listaTotales = new ArrayList<>();

        listaTotales.add(pedido1);
        listaTotales.add(compra1);

        // 6. Calculamos los totales mediante polimorfismo
        System.out.println("\n--- TOTALES ---");

        for (Totalizable t : listaTotales) {
            System.out.printf(
                "%s: S/ %.2f%n",
                t.getClass().getSimpleName(),
                t.calcularTotal()
            );
        }
    }
}
*/