package ucsur.pe.demo;

import ucsur.pe.util.ConversorDinero;
import ucsur.pe.model.Cliente;
import ucsur.pe.model.DateTime;
import ucsur.pe.model.Entrada;
import ucsur.pe.model.Reloj;
import ucsur.pe.model.RelojSistema;
import ucsur.pe.model.TipoEntrada;
import java.util.ArrayList;
import java.util.List;

/**
 * Por qué existe: demo de puntos. Cada tipo de entrada guarda sus puntos y el cliente aplica
 * el multiplicador de su nivel de socio.
 */
public class MainTipoEntrada {

    public static void main(String[] args) {

        System.out.println("=== TIPOS DE ENTRADA: PUNTOS POR ENTRADA ===");

        List<TipoEntrada> tipos = new ArrayList<>();
        tipos.add(new TipoEntrada(1, "GENERAL", "Entrada general", ConversorDinero.aCentavos("20.00"), 2, false, true));
        tipos.add(new TipoEntrada(2, "SOCIO", "Entrada para socios", ConversorDinero.aCentavos("15.00"), 3, true, true));
        tipos.add(new TipoEntrada(3, "NINO", "Entrada para ninos", ConversorDinero.aCentavos("10.00"), 1, false, true));
        tipos.add(new TipoEntrada(4, "ADULTO MAYOR", "Entrada adulto mayor", ConversorDinero.aCentavos("10.00"), 1, false, true));

        Reloj reloj = new RelojSistema(); // en la demo no hay App: se crea el reloj aquí
        Cliente socio = new Cliente(1, "12345678", "Ismael", "Chipana",
                new DateTime(1, 1, 2000), "ismael@mail.com", null, "clave123", reloj);
        Cliente invitado = Cliente.ingresarComoInvitado("invitado@cine.com", reloj);

        System.out.println("Socio nivel " + socio.getNivelSocio()
                + " (multiplicador x" + socio.obtenerMultiplicadorPuntos() + ") e invitado:");
        for (TipoEntrada t : tipos) {
            System.out.printf("%s: base %d punto(s) | socio gana %d | invitado gana %d%n",
                    t, t.obtenerPuntos(), socio.aplicarMultiplicador(t.obtenerPuntos()),
                    invitado.aplicarMultiplicador(t.obtenerPuntos()));
        }

        TipoEntrada inactiva = tipos.get(0);
        inactiva.desactivar();
        System.out.println("Tipo desactivado: " + inactiva.obtenerPuntos() + " punto(s)");
    }
}
