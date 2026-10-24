package ucsur.pe.cineobjects;

/**
 * Representa una butaca física, identificada por su fila y número dentro de una sala.
 * El constructor valida la fila, el número, el tipo permitido y que se indique una sala.
 */
public class Butaca {
    public static final String ESTANDAR = "ESTANDAR";
    public static final String PREFERENCIAL = "PREFERENCIAL";
    public static final String DISCAPACIDAD = "DISCAPACIDAD";

    private int idButaca;
    private String fila;
    private int numero;
    private String tipo;
    private Sala sala;

    public Butaca(int idButaca, String fila, int numero, String tipo, Sala sala) {
        if (fila == null || fila.trim().isEmpty() || fila.trim().length() > 3) {
            throw new IllegalArgumentException("La fila es obligatoria (máx. 3 caracteres)");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de butaca debe ser mayor a 0");
        }
        if (!ESTANDAR.equals(tipo) && !PREFERENCIAL.equals(tipo) && !DISCAPACIDAD.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de butaca inválido: " + tipo);
        }
        if (sala == null) {
            throw new IllegalArgumentException("La sala es obligatoria");
        }
        this.idButaca = idButaca;
        this.fila = fila.trim().toUpperCase();
        this.numero = numero;
        this.tipo = tipo;
        this.sala = sala;
    }

    public String getCodigoAsiento() {
        return fila + numero;
    }

    public boolean esAccesible() {
        return DISCAPACIDAD.equals(tipo);
    }

    /** Consulta la disponibilidad de esta butaca para una función; si no hay función, devuelve false. */
    public boolean estaDisponible(Funcion funcion) {
        if (funcion == null) return false;
        return funcion.esButacaDisponible(this);
    }

    public int getIdButaca() { return idButaca; }
    public String getFila() { return fila; }
    public int getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public Sala getSala() { return sala; }

    @Override
    public String toString() {
        return getCodigoAsiento() + " (" + tipo + ")";
    }
}
