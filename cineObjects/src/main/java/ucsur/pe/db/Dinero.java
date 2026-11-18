package ucsur.pe.db;

import ucsur.pe.util.ConversorDinero;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Por qué existe: el modelo guarda el dinero en céntimos (long) y la BD en DECIMAL(x,2).
 * Al leer se toma el texto ("41.90") y se convierte con ConversorDinero, nunca con getDouble,
 * para no perder céntimos por redondeo. Es el único lugar de db que hace esta conversión.
 */
final class Dinero {

    private Dinero() { }

    /** Céntimos -> DECIMAL para un PreparedStatement: 4190 -> 41.90. */
    static BigDecimal aDecimal(long centavos) {
        return BigDecimal.valueOf(centavos, 2);
    }

    /** Columna DECIMAL -> céntimos: "41.90" -> 4190. */
    static long leer(ResultSet rs, String columna) throws SQLException {
        return ConversorDinero.aCentavos(rs.getString(columna));
    }
}
