package ucsur.pe.cineInterfaz;

import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.Cliente;
import ucsur.pe.cineobjects.DateTime;
import ucsur.pe.cineobjects.TipoEntrada;

/**
 * Por qué existe: demo de puntos. Cada tipo de entrada guarda sus puntos y el cliente aplica
 * el multiplicador de su nivel de socio.
 */
public class MainTipoEntrada {

    public static void main(String[] args) {

        System.out.println("=== TIPOS DE ENTRADA: PUNTOS POR ENTRADA ===");

        List<TipoEntrada> tipos = new ArrayList<>();
        tipos.add(new TipoEntrada(1, "GENERAL", "Entrada general", 20.0, 2, false, true));
        tipos.add(new TipoEntrada(2, "SOCIO", "Entrada para socios", 15.0, 3, true, true));
        tipos.add(new TipoEntrada(3, "NINO", "Entrada para ninos", 10.0, 1, false, true));
        tipos.add(new TipoEntrada(4, "ADULTO MAYOR", "Entrada adulto mayor", 10.0, 1, false, true));

        Cliente socio = new Cliente(1, "12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123");
        Cliente invitado = Cliente.ingresarComoInvitado("invitado@cine.com");

        System.out.println("Socio nivel " + socio.getNivelSocio()
                + " (multiplicador x" + socio.obtenerMultiplicadorPuntos() + ") e invitado:");
        for (TipoEntrada t : tipos) {
            System.out.printf("%s: base %d punto(s) | socio gana %d | invitado gana %d%n",
                    t, t.obtenerPuntos(), t.calcularPuntos(socio), t.calcularPuntos(invitado));
        }

        TipoEntrada inactiva = tipos.get(0);
        inactiva.desactivar();
        System.out.println("Tipo desactivado: " + inactiva.obtenerPuntos() + " punto(s)");
    }
}
