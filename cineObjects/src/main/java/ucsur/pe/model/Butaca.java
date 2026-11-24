package ucsur.pe.model;

/**
 * Por qué existe: es un asiento físico de una sala (fila + número, por ejemplo A5).
 * Sabe su tipo y en qué bloque de la sala está: las salas tienen dos pasillos que separan
 * un bloque IZQUIERDA, uno CENTRO (el más ancho) y uno DERECHA.
 */
public class Butaca {
    // Tipos de butaca (constantes: se usan como Butaca.NOMBRE y su valor no cambia).
    public static final String ESTANDAR = "ESTANDAR"; // asiento normal
    public static final String PREFERENCIAL = "PREFERENCIAL"; // asiento de mejor ubicación o comodidad
    public static final String DISCAPACIDAD = "DISCAPACIDAD"; // asiento accesible; se recomienda solo si se pide

    // Bloques de la sala, separados por los pasillos.
    public static final String IZQUIERDA = "IZQUIERDA"; // bloque lateral izquierdo
    public static final String CENTRO = "CENTRO"; // bloque central, entre los dos pasillos
    public static final String DERECHA = "DERECHA"; // bloque lateral derecho

    private int idButaca;
    private String fila; // de 1 a 3 letras, se guarda en mayúsculas
    private int numero;
    private String tipo;
    private String bloque; // IZQUIERDA, CENTRO o DERECHA; dos butacas de bloques distintos tienen un pasillo en medio
    private Sala sala;

    public Butaca(int idButaca, String fila, int numero, String tipo, String bloque, Sala sala) {
        if (fila == null || fila.trim().isEmpty() || fila.trim().length() > 3) {
            throw new IllegalArgumentException("La fila es obligatoria (máx. 3 caracteres)");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("El número de butaca debe ser mayor a 0");
        }
        if (!ESTANDAR.equals(tipo) && !PREFERENCIAL.equals(tipo) && !DISCAPACIDAD.equals(tipo)) {
            throw new IllegalArgumentException("Tipo de butaca inválido: " + tipo);
        }
        if (!IZQUIERDA.equals(bloque) && !CENTRO.equals(bloque) && !DERECHA.equals(bloque)) {
            throw new IllegalArgumentException("Bloque de butaca inválido: " + bloque);
        }
        if (sala == null) {
            throw new IllegalArgumentException("La sala es obligatoria");
        }
        this.idButaca = idButaca;
        this.bloque = bloque;
        this.fila = fila.trim().toUpperCase();
        this.numero = numero;
        this.tipo = tipo;
        this.sala = sala;
    }

    /**
     * Asigna el id real generado por la base tras el INSERT. Las butacas que crea Sala.generarButacas
     * nacen con id 0, y las entradas de la BD necesitan ese id (entrada.id_butaca).
     */
    public void asignarId(int idButaca) {
        this.idButaca = Ids.asignar(this.idButaca, idButaca, "la butaca");
    }

    /** Código visible del asiento: fila y número, como A5. */
    public String getCodigoAsiento() {
        return fila + numero;
    }

    /** Indica si es una butaca para personas con discapacidad. */
    public boolean esAccesible() {
        return DISCAPACIDAD.equals(tipo);
    }

    public int getIdButaca() { return idButaca; }
    public String getFila() { return fila; }
    public int getNumero() { return numero; }
    public String getTipo() { return tipo; }
    public String getBloque() { return bloque; }
    public boolean estaEnBloqueCentral() { return CENTRO.equals(bloque); }
    public Sala getSala() { return sala; }

    @Override
    public String toString() {
        return getCodigoAsiento() + " (" + tipo + ")";
    }
}
