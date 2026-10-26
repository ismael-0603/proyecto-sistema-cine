package ucsur.pe.cineInterfaz;

import java.util.ArrayList;
import java.util.List;

import ucsur.pe.cineobjects.TipoEntrada;
import ucsur.pe.cineobjects.TipoEntradaGeneral;
import ucsur.pe.cineobjects.TipoEntradaReducida;
import ucsur.pe.cineobjects.TipoEntradaSocio;

/** Demuestra polimorfismo: cada categoría de entrada responde distinto a obtenerPuntos(). */
public class MainTipoEntrada {

    public static void main(String[] args) {

        System.out.println("=== POLIMORFISMO: TIPOS DE ENTRADA ===");

        List<TipoEntrada> tipos = new ArrayList<>();
        tipos.add(new TipoEntradaGeneral(1, "GENERAL", "Entrada general", 20.0, false, true));
        tipos.add(new TipoEntradaSocio(2, "SOCIO", "Entrada para socios", 15.0, true, true));
        tipos.add(new TipoEntradaReducida(3, "NINO", "Entrada para ninos", 10.0, false, true));
        tipos.add(TipoEntrada.desdeNombre(4, "ADULTO MAYOR", "Entrada adulto mayor", 10.0, false, true, 0));

        for (TipoEntrada t : tipos) {
            System.out.printf("%s [%s]: %d punto(s)%n", t, t.getClass().getSimpleName(), t.obtenerPuntos());
        }

        TipoEntrada inactiva = tipos.get(0);
        inactiva.desactivar();
        System.out.println("Tipo desactivado: " + inactiva.obtenerPuntos() + " punto(s)");
    }
}
