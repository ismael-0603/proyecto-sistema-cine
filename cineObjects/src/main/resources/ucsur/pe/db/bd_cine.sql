-- Borra la base si ya existe para poder volver a ejecutar el script desde cero.
-- ATENCION: elimina todos los datos de DB_Cine.
DROP DATABASE IF EXISTS DB_Cine;
CREATE DATABASE DB_Cine;
USE DB_Cine;

-- ============================================================
-- CATALOGOS Y CONFIGURACION
-- ============================================================

CREATE TABLE rol (
    id_rol       INT          NOT NULL AUTO_INCREMENT,
    nombre       VARCHAR(80)  NOT NULL,
    descripcion  VARCHAR(150),
    nivel_acceso INT          NOT NULL,

    CONSTRAINT pk_rol        PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    -- 1 = dulceria, 2 = administrador (igual que Rol en Java)
    CONSTRAINT ck_rol_nivel  CHECK (nivel_acceso IN (1, 2))
);

CREATE TABLE sede (
    id_sede          INT          NOT NULL AUTO_INCREMENT,
    nombre           VARCHAR(60)  NOT NULL,
    direccion        VARCHAR(150) NOT NULL,
    distrito         VARCHAR(60)  NOT NULL,
    telefono         VARCHAR(20),
    horario_atencion VARCHAR(100),
    estado           VARCHAR(20)  NOT NULL DEFAULT 'ACTIVA',

    CONSTRAINT pk_sede        PRIMARY KEY (id_sede),
    CONSTRAINT uq_sede_nombre UNIQUE (nombre),
    CONSTRAINT ck_sede_estado CHECK (estado IN ('ACTIVA', 'INACTIVA', 'EN_MANTENIMIENTO'))
);

CREATE TABLE genero (
    id_genero INT         NOT NULL AUTO_INCREMENT,
    nombre    VARCHAR(20) NOT NULL,

    CONSTRAINT pk_genero        PRIMARY KEY (id_genero),
    CONSTRAINT uq_genero_nombre UNIQUE (nombre)
);

CREATE TABLE tipo_sala (
    id_tipo_sala     INT          NOT NULL AUTO_INCREMENT,
    formato          VARCHAR(30)  NOT NULL,
    descripcion      VARCHAR(150),
    tarifa_adicional DECIMAL(6,2) NOT NULL DEFAULT 0,

    CONSTRAINT pk_tipo_sala         PRIMARY KEY (id_tipo_sala),
    CONSTRAINT uq_tipo_sala_formato UNIQUE (formato),
    CONSTRAINT ck_tipo_sala_tarifa  CHECK (tarifa_adicional >= 0)
);

CREATE TABLE tipo_entrada (
    id_tipo_entrada    INT          NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(40)  NOT NULL,
    descripcion        VARCHAR(200),
    tarifa             DECIMAL(6,2) NOT NULL,
    puntos_por_entrada INT          NOT NULL DEFAULT 0, -- puntos base que gana el cliente por entrada (sin multiplicador de nivel)
    requiere_socio     BOOLEAN      NOT NULL DEFAULT FALSE,
    puntos_canje       INT          NOT NULL DEFAULT 0,
    solo_cumpleanos    BOOLEAN      NOT NULL DEFAULT FALSE, -- solo se vende el día del cumpleaños del cliente
    estado             BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_tipo_entrada             PRIMARY KEY (id_tipo_entrada),
    CONSTRAINT uq_tipo_entrada_nombre      UNIQUE (nombre),
    CONSTRAINT ck_tipo_entrada_tarifa      CHECK (tarifa >= 0),
    CONSTRAINT ck_tipo_entrada_puntos      CHECK (puntos_por_entrada >= 0),
    CONSTRAINT ck_tipo_entrada_canje       CHECK (puntos_canje >= 0),
    CONSTRAINT ck_tipo_entrada_canje_socio CHECK (puntos_canje = 0 OR requiere_socio = TRUE),
    CONSTRAINT ck_tipo_entrada_cumple      CHECK (solo_cumpleanos = FALSE OR requiere_socio = TRUE)
);

CREATE TABLE promocion (
    id_promocion       INT          NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(40)  NOT NULL,
    descripcion        VARCHAR(200),
    tipo_descuento     VARCHAR(15)  NOT NULL,
    valor              DECIMAL(6,2) NOT NULL,
    fecha_inicio       DATE         NOT NULL,
    fecha_fin          DATE         NOT NULL,
    nivel_socio_minimo TINYINT      NOT NULL DEFAULT 0, -- 0 = todos; 1 = cualquier socio; 2 a 4 = ese nivel o más
    estado             BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_promocion        PRIMARY KEY (id_promocion),
    CONSTRAINT ck_promocion_nivel  CHECK (nivel_socio_minimo BETWEEN 0 AND 4),
    CONSTRAINT ck_promocion_tipo   CHECK (tipo_descuento IN ('PORCENTAJE', 'MONTO_FIJO')),
    CONSTRAINT ck_promocion_valor  CHECK (valor >= 0),
    CONSTRAINT ck_promocion_fechas CHECK (fecha_fin >= fecha_inicio)
);

CREATE TABLE cupon (
    id_cupon           INT          NOT NULL AUTO_INCREMENT,
    codigo             VARCHAR(30)  NOT NULL,
    tipo_descuento     VARCHAR(15)  NOT NULL,
    descuento          DECIMAL(6,2) NOT NULL,
    fecha_inicio       DATE         NOT NULL,
    fecha_fin          DATE         NOT NULL,
    limite_uso         INT          NOT NULL,
    usos_realizados    INT          NOT NULL DEFAULT 0,
    nivel_socio_minimo TINYINT      NOT NULL DEFAULT 0, -- 0 = todos; 1 = cualquier socio; 2 a 4 = ese nivel o más
    estado             BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_cupon        PRIMARY KEY (id_cupon),
    CONSTRAINT uq_cupon_codigo UNIQUE (codigo),
    CONSTRAINT ck_cupon_tipo   CHECK (tipo_descuento IN ('PORCENTAJE', 'MONTO_FIJO')),
    CONSTRAINT ck_cupon_desc   CHECK (descuento >= 0),
    CONSTRAINT ck_cupon_fechas CHECK (fecha_fin >= fecha_inicio),
    CONSTRAINT ck_cupon_limite CHECK (limite_uso >= 0),
    CONSTRAINT ck_cupon_usos   CHECK (usos_realizados >= 0 AND usos_realizados <= limite_uso),
    CONSTRAINT ck_cupon_nivel  CHECK (nivel_socio_minimo BETWEEN 0 AND 4)
);

-- niveles del programa de socios; el cine les pone nombre y define cuando se sube
-- (Java usa las mismas reglas: Cliente.VISITAS_POR_NIVEL y MULTIPLICADOR_POR_NIVEL)
CREATE TABLE nivel_socio (
    id_nivel             INT          NOT NULL, -- 0 = invitado (sin programa), 1 a 4 = socio
    nombre               VARCHAR(30)  NOT NULL,
    visitas_minimas      INT          NOT NULL, -- visitas confirmadas en el año para llegar al nivel
    multiplicador_puntos DECIMAL(4,2) NOT NULL, -- los puntos base se multiplican por este valor

    CONSTRAINT pk_nivel_socio         PRIMARY KEY (id_nivel),
    CONSTRAINT uq_nivel_socio_nombre  UNIQUE (nombre),
    CONSTRAINT ck_nivel_socio_id      CHECK (id_nivel BETWEEN 0 AND 4),
    CONSTRAINT ck_nivel_socio_visitas CHECK (visitas_minimas >= 0),
    CONSTRAINT ck_nivel_socio_mult    CHECK (multiplicador_puntos >= 0)
);

CREATE TABLE producto (
    id_producto        INT          NOT NULL AUTO_INCREMENT,
    nombre             VARCHAR(100) NOT NULL,
    descripcion        VARCHAR(255),
    categoria          VARCHAR(40)  NOT NULL,
    precio             DECIMAL(8,2) NOT NULL,
    es_combo           BOOLEAN      NOT NULL DEFAULT FALSE,
    nivel_socio_minimo INT          NOT NULL DEFAULT 0,
    estado             BOOLEAN      NOT NULL DEFAULT TRUE,
    imagen_url         VARCHAR(255), -- ruta de la foto dentro del proyecto: 'productos/canchita-salada.jpg'

    CONSTRAINT pk_producto        PRIMARY KEY (id_producto),
    CONSTRAINT uq_producto_nombre UNIQUE (nombre),
    CONSTRAINT ck_producto_precio CHECK (precio >= 0),
    -- 0 = cualquier cliente; 1 a 4 = combo exclusivo desde ese nivel de socio
    CONSTRAINT fk_producto_nivel  FOREIGN KEY (nivel_socio_minimo) REFERENCES nivel_socio (id_nivel)
);

CREATE TABLE pelicula (
    id_pelicula        INT          NOT NULL AUTO_INCREMENT,
    titulo             VARCHAR(150) NOT NULL,
    sinopsis           TEXT,
    duracion_min       INT          NOT NULL,
    clasificacion_edad VARCHAR(10)  NOT NULL,
    trailer_url        VARCHAR(255),
    poster_url         VARCHAR(255),
    fecha_estreno      DATE,
    estado             VARCHAR(20)  NOT NULL DEFAULT 'EN_CARTELERA',

    CONSTRAINT pk_pelicula          PRIMARY KEY (id_pelicula),
    CONSTRAINT ck_pelicula_duracion CHECK (duracion_min > 0),
    -- PROXIMAMENTE: anunciada, sin funciones | PREESTRENO: vende funciones antes de fecha_estreno
    -- EN_CARTELERA: estrenada | REESTRENO: pelicula antigua de vuelta en cines (fecha_estreno es la original)
    -- FUERA_DE_CARTELERA: ya no se proyecta (equivale a desactivarla)
    CONSTRAINT ck_pelicula_estado   CHECK (estado IN
        ('PROXIMAMENTE', 'PREESTRENO', 'EN_CARTELERA', 'REESTRENO', 'FUERA_DE_CARTELERA'))
);

-- ============================================================
-- PERSONAS
-- ============================================================

CREATE TABLE cliente (
    id_cliente     INT          NOT NULL AUTO_INCREMENT,
    dni            CHAR(8),
    nombres        VARCHAR(80),
    apellidos      VARCHAR(80),
    fecha_nac      DATE,
    correo         VARCHAR(100) NOT NULL,
    telefono       VARCHAR(20),
    contra         VARCHAR(255),
    estado         BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    puntos         INT          NOT NULL DEFAULT 0,
    nivel_socio    INT          NOT NULL DEFAULT 0, -- 0 = invitado, 1 a 4 = nivel de socio (Cliente.NIVEL_MAXIMO)
    es_invitado    BOOLEAN      NOT NULL DEFAULT FALSE,
    -- columna calculada: solo tiene valor si el cliente NO es invitado
    correo_unico   VARCHAR(100) GENERATED ALWAYS AS
                       (CASE WHEN es_invitado = 0 THEN correo ELSE NULL END) STORED,

    CONSTRAINT pk_cliente           PRIMARY KEY (id_cliente),
    CONSTRAINT uq_cliente_dni       UNIQUE (dni),
    CONSTRAINT uk_cliente_correo    UNIQUE (correo_unico),
    CONSTRAINT ck_cliente_puntos    CHECK (puntos >= 0),
    CONSTRAINT fk_cliente_nivel     FOREIGN KEY (nivel_socio) REFERENCES nivel_socio (id_nivel),
    CONSTRAINT ck_client_registrado CHECK (
        es_invitado = 1
        OR (dni IS NOT NULL AND nombres IS NOT NULL
            AND apellidos IS NOT NULL AND contra IS NOT NULL)
    )
);

CREATE TABLE empleado (
    id_empleado INT          NOT NULL AUTO_INCREMENT,
    dni         CHAR(8)      NOT NULL,
    nombres     VARCHAR(80)  NOT NULL,
    apellidos   VARCHAR(80)  NOT NULL,
    fecha_nac   DATE,
    correo      VARCHAR(100) NOT NULL,
    telefono    VARCHAR(20),
    contra      VARCHAR(255) NOT NULL,
    estado      BOOLEAN      NOT NULL DEFAULT TRUE,
    turno       VARCHAR(20),
    id_rol      INT          NOT NULL,
    id_sede     INT          NOT NULL,
    pin         CHAR(4)      NOT NULL, -- se pide al volver al panel desde la cuenta personal
    id_cliente  INT          NULL,     -- cuenta personal (cliente) vinculada, con el mismo DNI

    CONSTRAINT pk_empleado         PRIMARY KEY (id_empleado),
    CONSTRAINT uq_empleado_dni     UNIQUE (dni),
    CONSTRAINT uq_empleado_correo  UNIQUE (correo),
    CONSTRAINT uq_empleado_cliente UNIQUE (id_cliente),
    CONSTRAINT ck_empleado_pin     CHECK (pin REGEXP '^[0-9]{4}$'),
    CONSTRAINT fk_empleado_rol     FOREIGN KEY (id_rol)     REFERENCES rol (id_rol),
    CONSTRAINT fk_empleado_sede    FOREIGN KEY (id_sede)    REFERENCES sede (id_sede),
    CONSTRAINT fk_empleado_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente)
);

-- ============================================================
-- SALAS Y FUNCIONES
-- ============================================================

CREATE TABLE sala (
    id_sala      INT         NOT NULL AUTO_INCREMENT,
    numero       INT         NOT NULL,
    capacidad    INT         NOT NULL,
    estado       VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    id_sede      INT         NOT NULL,
    id_tipo_sala INT         NOT NULL,

    CONSTRAINT pk_sala           PRIMARY KEY (id_sala),
    CONSTRAINT uq_sala_sede_num  UNIQUE (id_sede, numero),
    CONSTRAINT ck_sala_capacidad CHECK (capacidad > 0),
    CONSTRAINT ck_sala_estado    CHECK (estado IN ('ACTIVA', 'INACTIVA', 'EN_MANTENIMIENTO')),
    CONSTRAINT fk_sala_sede      FOREIGN KEY (id_sede)      REFERENCES sede (id_sede),
    CONSTRAINT fk_sala_tipo      FOREIGN KEY (id_tipo_sala) REFERENCES tipo_sala (id_tipo_sala)
);

CREATE TABLE butaca (
    id_butaca INT         NOT NULL AUTO_INCREMENT,
    fila      VARCHAR(3)  NOT NULL,
    numero    INT         NOT NULL,
    tipo      VARCHAR(15) NOT NULL,
    -- bloque de la sala: los dos pasillos separan IZQUIERDA | CENTRO | DERECHA
    bloque    VARCHAR(10) NOT NULL DEFAULT 'CENTRO',
    id_sala   INT         NOT NULL,

    CONSTRAINT pk_butaca          PRIMARY KEY (id_butaca),
    CONSTRAINT uq_butaca_posicion UNIQUE (id_sala, fila, numero),
    CONSTRAINT ck_butaca_tipo     CHECK (tipo IN ('ESTANDAR', 'PREFERENCIAL', 'DISCAPACIDAD')),
    CONSTRAINT ck_butaca_bloque   CHECK (bloque IN ('IZQUIERDA', 'CENTRO', 'DERECHA')),
    CONSTRAINT fk_butaca_sala     FOREIGN KEY (id_sala) REFERENCES sala (id_sala)
);

CREATE TABLE funcion (
    id_funcion  INT         NOT NULL AUTO_INCREMENT,
    fecha       DATE        NOT NULL,
    hora_inicio TIME        NOT NULL,
    idioma      VARCHAR(30) NOT NULL,
    estado      VARCHAR(15) NOT NULL DEFAULT 'DISPONIBLE',
    id_pelicula     INT         NOT NULL,
    id_sala         INT         NOT NULL,
    -- columna calculada: vale 1 solo si la funcion NO esta CANCELADA (permite reprogramar horarios cancelados)
    horario_activo  TINYINT GENERATED ALWAYS AS
                        (CASE WHEN estado <> 'CANCELADA' THEN 1 ELSE NULL END) STORED,

    CONSTRAINT pk_funcion          PRIMARY KEY (id_funcion),
    CONSTRAINT uq_funcion_horario  UNIQUE (id_sala, fecha, hora_inicio, horario_activo),
    CONSTRAINT ck_funcion_estado   CHECK (estado IN ('DISPONIBLE', 'AGOTADA', 'EN_CURSO', 'FINALIZADA', 'CANCELADA')),
    CONSTRAINT fk_funcion_pelicula FOREIGN KEY (id_pelicula) REFERENCES pelicula (id_pelicula),
    CONSTRAINT fk_funcion_sala     FOREIGN KEY (id_sala)     REFERENCES sala (id_sala)
);

-- ============================================================
-- COMPRAS Y PAGOS
-- ============================================================

CREATE TABLE compra (
    id_compra         INT           NOT NULL AUTO_INCREMENT,
    fecha_hora        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_compra     DATETIME      NOT NULL, -- fecha_hora + 4 min: plazo de todo el proceso (butacas, dulceria y pago)
    serie_numero      VARCHAR(20),
    subtotal          DECIMAL(10,2) NOT NULL DEFAULT 0,
    descuento         DECIMAL(10,2) NOT NULL DEFAULT 0,
    igv               DECIMAL(10,2) NOT NULL DEFAULT 0,
    total             DECIMAL(10,2) NOT NULL DEFAULT 0,
    puntos_canjeados  INT           NOT NULL DEFAULT 0,
    puntos_otorgados  INT           NOT NULL DEFAULT 0, -- puntos ganados al confirmar; se revierten si se reembolsa
    monto_reembolsado DECIMAL(10,2) NOT NULL DEFAULT 0, -- lo devuelto al reembolsar: el total, o solo las entradas si la dulceria ya se entrego
    estado            VARCHAR(15)   NOT NULL DEFAULT 'EN_PROCESO',
    id_cliente        INT           NOT NULL,
    id_sede           INT           NOT NULL,
    id_cupon          INT           NULL,
    id_promocion      INT           NULL,

    CONSTRAINT pk_compra           PRIMARY KEY (id_compra),
    CONSTRAINT uq_compra_serie     UNIQUE (serie_numero),
    CONSTRAINT ck_compra_montos    CHECK (subtotal >= 0 AND descuento >= 0 AND igv >= 0 AND total >= 0),
    CONSTRAINT ck_compra_puntos    CHECK (puntos_canjeados >= 0 AND puntos_otorgados >= 0),
    CONSTRAINT ck_compra_reembolso CHECK (monto_reembolsado >= 0 AND monto_reembolsado <= total),
    CONSTRAINT ck_compra_plazo     CHECK (expira_compra >= fecha_hora),
    CONSTRAINT ck_compra_estado    CHECK (estado IN ('EN_PROCESO', 'CONFIRMADA', 'CANCELADA', 'EXPIRADA', 'REEMBOLSADA')),
    CONSTRAINT fk_compra_cliente   FOREIGN KEY (id_cliente)   REFERENCES cliente (id_cliente),
    CONSTRAINT fk_compra_sede      FOREIGN KEY (id_sede)      REFERENCES sede (id_sede),
    CONSTRAINT fk_compra_cupon     FOREIGN KEY (id_cupon)     REFERENCES cupon (id_cupon),
    CONSTRAINT fk_compra_promocion FOREIGN KEY (id_promocion) REFERENCES promocion (id_promocion)
);

-- las entradas se insertan al confirmar los asientos (Compra.confirmarSeleccionAsientos), ya con su tipo
CREATE TABLE entrada (
    id_entrada      INT          NOT NULL AUTO_INCREMENT,
    codigo_qr       VARCHAR(100),
    precio          DECIMAL(8,2) NOT NULL,
    fecha_emision   DATETIME,
    expira_bloqueo  DATETIME,
    estado          VARCHAR(15)  NOT NULL DEFAULT 'BLOQUEADA',
    id_compra       INT          NOT NULL,
    id_funcion      INT          NOT NULL,
    id_butaca       INT          NOT NULL,
    id_tipo_entrada INT          NOT NULL,
    -- columna calculada: vale 1 solo si la entrada esta BLOQUEADA o VENDIDA
    butaca_ocupada  TINYINT GENERATED ALWAYS AS
                        (CASE WHEN estado IN ('BLOQUEADA', 'VENDIDA') THEN 1 ELSE NULL END) STORED,

    CONSTRAINT pk_entrada                PRIMARY KEY (id_entrada),
    CONSTRAINT uq_entrada_qr             UNIQUE (codigo_qr),
    CONSTRAINT uq_entrada_butaca_funcion UNIQUE (id_funcion, id_butaca, butaca_ocupada),
    CONSTRAINT ck_entrada_precio         CHECK (precio >= 0),
    -- PRESELECCIONADA existe en Java mientras el cliente elige; normalmente se inserta ya BLOQUEADA
    CONSTRAINT ck_entrada_estado         CHECK (estado IN ('PRESELECCIONADA', 'BLOQUEADA', 'VENDIDA', 'EXPIRADA', 'ANULADA')),
    CONSTRAINT fk_entrada_compra         FOREIGN KEY (id_compra)       REFERENCES compra (id_compra),
    CONSTRAINT fk_entrada_funcion        FOREIGN KEY (id_funcion)      REFERENCES funcion (id_funcion),
    CONSTRAINT fk_entrada_butaca         FOREIGN KEY (id_butaca)       REFERENCES butaca (id_butaca),
    CONSTRAINT fk_entrada_tipo           FOREIGN KEY (id_tipo_entrada) REFERENCES tipo_entrada (id_tipo_entrada)
);

CREATE TABLE transaccion_pago (
    id_transaccion      INT           NOT NULL AUTO_INCREMENT,
    monto               DECIMAL(10,2) NOT NULL,
    medio_pago          VARCHAR(20)   NOT NULL,
    estado_pago         VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
    fecha_hora          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referencia_pasarela VARCHAR(80),
    motivo_rechazo      VARCHAR(150),
    id_compra           INT           NOT NULL,

    CONSTRAINT pk_transaccion        PRIMARY KEY (id_transaccion),
    CONSTRAINT uq_transaccion_ref    UNIQUE (referencia_pasarela),
    CONSTRAINT ck_transaccion_monto  CHECK (monto >= 0),
    CONSTRAINT ck_transaccion_medio  CHECK (medio_pago IN ('TARJETA_CREDITO', 'TARJETA_DEBITO', 'YAPE', 'PLIN', 'PAGO_QR')),
    CONSTRAINT ck_transaccion_estado CHECK (estado_pago IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    CONSTRAINT fk_transaccion_compra FOREIGN KEY (id_compra) REFERENCES compra (id_compra)
);

CREATE TABLE pedido (
    id_pedido     INT          NOT NULL AUTO_INCREMENT,
    codigo_qr     VARCHAR(100), -- se genera despues del INSERT, cuando ya existe id_pedido
    fecha_hora    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_entrega DATETIME,
    estado        VARCHAR(15)  NOT NULL DEFAULT 'PENDIENTE',
    id_compra     INT          NOT NULL,
    id_empleado   INT          NULL,

    CONSTRAINT pk_pedido          PRIMARY KEY (id_pedido),
    CONSTRAINT uq_pedido_qr       UNIQUE (codigo_qr),
    CONSTRAINT uq_pedido_compra   UNIQUE (id_compra),
    CONSTRAINT ck_pedido_estado   CHECK (estado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'ENTREGADO', 'ANULADO')),
    CONSTRAINT fk_pedido_compra   FOREIGN KEY (id_compra)   REFERENCES compra (id_compra),
    CONSTRAINT fk_pedido_empleado FOREIGN KEY (id_empleado) REFERENCES empleado (id_empleado)
);

-- ============================================================
-- POSTVENTA
-- ============================================================

CREATE TABLE reclamo (
    id_reclamo  INT         NOT NULL AUTO_INCREMENT,
    tipo        VARCHAR(20) NOT NULL,
    descripcion TEXT        NOT NULL,
    fecha       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado      VARCHAR(15) NOT NULL DEFAULT 'REGISTRADO',
    respuesta   TEXT,
    id_compra   INT         NOT NULL,

    CONSTRAINT pk_reclamo        PRIMARY KEY (id_reclamo),
    -- no hay reclamo de reembolso: si el cine cancela la funcion, el dinero se devuelve automaticamente
    CONSTRAINT ck_reclamo_tipo   CHECK (tipo IN ('ATENCION', 'PRODUCTO_DULCERIA', 'SALA_PROYECCION')),
    CONSTRAINT ck_reclamo_estado CHECK (estado IN
        ('REGISTRADO', 'EN_REVISION', 'RESUELTO', 'RECHAZADO')),
    CONSTRAINT fk_reclamo_compra FOREIGN KEY (id_compra) REFERENCES compra (id_compra)
);

CREATE TABLE calificacion (
    id_calificacion INT          NOT NULL AUTO_INCREMENT,
    puntuacion      TINYINT      NOT NULL,
    comentario      VARCHAR(300),
    fecha           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_compra       INT          NOT NULL,

    CONSTRAINT pk_calificacion        PRIMARY KEY (id_calificacion),
    CONSTRAINT uq_calificacion_compra UNIQUE (id_compra),
    CONSTRAINT ck_calificacion_punt   CHECK (puntuacion BETWEEN 1 AND 5),
    CONSTRAINT fk_calificacion_compra FOREIGN KEY (id_compra) REFERENCES compra (id_compra)
);

-- ============================================================
-- SERIES DE COMPROBANTES
-- ============================================================

-- guarda el ultimo numero usado por cada serie; lo incrementa el trigger trg_compra_serie
CREATE TABLE correlativo (
    serie         CHAR(4) NOT NULL,
    ultimo_numero INT     NOT NULL DEFAULT 0,

    CONSTRAINT pk_correlativo        PRIMARY KEY (serie),
    CONSTRAINT ck_correlativo_numero CHECK (ultimo_numero >= 0)
);

INSERT INTO correlativo (serie) VALUES ('B001');

-- ============================================================
-- TABLAS INTERMEDIAS
-- ============================================================

CREATE TABLE pelicula_genero (
    id_pelicula INT NOT NULL,
    id_genero   INT NOT NULL,

    CONSTRAINT pk_pelicula_genero PRIMARY KEY (id_pelicula, id_genero),
    CONSTRAINT fk_pg_pelicula     FOREIGN KEY (id_pelicula) REFERENCES pelicula (id_pelicula) ON DELETE CASCADE,
    CONSTRAINT fk_pg_genero       FOREIGN KEY (id_genero)   REFERENCES genero (id_genero) ON DELETE CASCADE
);

CREATE TABLE promocion_dia (
    id_promocion INT     NOT NULL,
    dia_semana   TINYINT NOT NULL, -- 1 = lunes ...

    CONSTRAINT pk_promocion_dia PRIMARY KEY (id_promocion, dia_semana),
    CONSTRAINT ck_promocion_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT fk_pd_promocion  FOREIGN KEY (id_promocion) REFERENCES promocion (id_promocion) ON DELETE CASCADE
);

CREATE TABLE inventario_sede (
    id_sede     INT NOT NULL,
    id_producto INT NOT NULL,
    stock       INT NOT NULL DEFAULT 0,

    CONSTRAINT pk_inventario_sede  PRIMARY KEY (id_sede, id_producto),
    CONSTRAINT ck_inventario_stock CHECK (stock >= 0),
    CONSTRAINT fk_inv_sede         FOREIGN KEY (id_sede)     REFERENCES sede (id_sede),
    CONSTRAINT fk_inv_producto     FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
);

CREATE TABLE detalle_pedido (
    id_detalle      INT          NOT NULL AUTO_INCREMENT,
    cantidad        INT          NOT NULL,
    precio_unitario DECIMAL(8,2) NOT NULL,
    personalizacion VARCHAR(255),
    id_pedido       INT          NOT NULL,
    id_producto     INT          NOT NULL,

    CONSTRAINT pk_detalle_pedido   PRIMARY KEY (id_detalle),
    CONSTRAINT ck_detalle_cant     CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_precio   CHECK (precio_unitario >= 0),
    CONSTRAINT fk_detalle_pedido   FOREIGN KEY (id_pedido)   REFERENCES pedido (id_pedido) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto)
);

-- ============================================================
-- TRIGGERS
-- ============================================================

-- la butaca debe ser de la sala de la funcion, y la funcion de la sede de la compra (RF-36)
DELIMITER $$

CREATE TRIGGER trg_entrada_coherencia_sala
BEFORE INSERT ON entrada
FOR EACH ROW
BEGIN
    DECLARE sala_funcion INT;
    DECLARE sala_butaca  INT;
    DECLARE sede_funcion INT;
    DECLARE sede_compra  INT;

    SELECT f.id_sala, s.id_sede INTO sala_funcion, sede_funcion
      FROM funcion f JOIN sala s ON s.id_sala = f.id_sala
     WHERE f.id_funcion = NEW.id_funcion;
    SELECT id_sala INTO sala_butaca FROM butaca WHERE id_butaca = NEW.id_butaca;
    SELECT id_sede INTO sede_compra FROM compra WHERE id_compra = NEW.id_compra;

    IF sala_funcion <> sala_butaca THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La butaca no pertenece a la sala de la funcion';
    END IF;
    IF sede_funcion <> sede_compra THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La funcion no pertenece a la sede de la compra';
    END IF;
END$$

DELIMITER ;

-- misma validacion al modificar una entrada existente
DELIMITER $$

CREATE TRIGGER trg_entrada_coherencia_sala_upd
BEFORE UPDATE ON entrada
FOR EACH ROW
BEGIN
    DECLARE sala_funcion INT;
    DECLARE sala_butaca  INT;
    DECLARE sede_funcion INT;
    DECLARE sede_compra  INT;

    SELECT f.id_sala, s.id_sede INTO sala_funcion, sede_funcion
      FROM funcion f JOIN sala s ON s.id_sala = f.id_sala
     WHERE f.id_funcion = NEW.id_funcion;
    SELECT id_sala INTO sala_butaca FROM butaca WHERE id_butaca = NEW.id_butaca;
    SELECT id_sede INTO sede_compra FROM compra WHERE id_compra = NEW.id_compra;

    IF sala_funcion <> sala_butaca THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La butaca no pertenece a la sala de la funcion';
    END IF;
    IF sede_funcion <> sede_compra THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La funcion no pertenece a la sede de la compra';
    END IF;
END$$

DELIMITER ;

-- una sala no puede tener mas butacas que su capacidad (misma regla que Sala.agregarButaca en Java)
DELIMITER $$

CREATE TRIGGER trg_butaca_capacidad
BEFORE INSERT ON butaca
FOR EACH ROW
BEGIN
    DECLARE cap        INT;
    DECLARE existentes INT;

    SELECT capacidad INTO cap FROM sala WHERE id_sala = NEW.id_sala;
    SELECT COUNT(*) INTO existentes FROM butaca WHERE id_sala = NEW.id_sala;

    IF existentes >= cap THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La sala ya alcanzo su capacidad de butacas';
    END IF;
END$$

DELIMITER ;

-- la base asigna la serie al confirmar la compra
-- (consecutiva y solo para compras confirmadas; si la transaccion se revierte, el numero tambien)
DELIMITER $$

CREATE TRIGGER trg_compra_serie
BEFORE UPDATE ON compra
FOR EACH ROW
BEGIN
    DECLARE nuevo_numero INT;

    IF NEW.estado = 'CONFIRMADA' AND OLD.estado <> 'CONFIRMADA' AND NEW.serie_numero IS NULL THEN
        UPDATE correlativo SET ultimo_numero = ultimo_numero + 1 WHERE serie = 'B001';
        SELECT ultimo_numero INTO nuevo_numero FROM correlativo WHERE serie = 'B001';
        SET NEW.serie_numero = CONCAT('B001-', LPAD(nuevo_numero, 6, '0'));
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- EVENTOS PROGRAMADOS
-- ============================================================

-- El programador de eventos debe estar activo (una vez; requiere permisos de admin)
SET GLOBAL event_scheduler = ON;

-- Java calcula expira_compra con la hora de Peru (UTC-5) y el evento la compara con NOW().
-- Si el servidor usa otra zona horaria, las compras expirarian antes o despues de los 4 minutos.
SET GLOBAL time_zone = '-05:00'; -- para las conexiones nuevas y el evento (requiere permisos de admin)
SET time_zone = '-05:00';        -- para esta sesion

DELIMITER $$

CREATE EVENT ev_expirar_compras
ON SCHEDULE EVERY 1 MINUTE
DO
BEGIN
    -- 1. Toda compra en proceso (con entradas, con dulcería o ambas) cuyo plazo venció pasa a EXPIRADA
    UPDATE compra
       SET estado = 'EXPIRADA'
     WHERE estado = 'EN_PROCESO'
       AND expira_compra <= NOW(); -- vence al llegar al limite, igual que Compra.verificarExpiracion

    -- 2. Sus entradas bloqueadas, y cualquier bloqueo vencido, pasan a EXPIRADA
    --    (butaca_ocupada pasa a NULL y el asiento queda libre)
    UPDATE entrada e
      JOIN compra c ON c.id_compra = e.id_compra
       SET e.estado = 'EXPIRADA'
     WHERE e.estado = 'BLOQUEADA'
       AND (c.estado = 'EXPIRADA' OR e.expira_bloqueo <= NOW());

    -- 3. El pedido de dulceria de una compra expirada se anula (ya no se prepara).
    --    No se devuelve stock: el stock solo se descuenta al confirmar la compra (Pedido.registrar).
    UPDATE pedido p
      JOIN compra c ON c.id_compra = p.id_compra
       SET p.estado = 'ANULADO'
     WHERE c.estado = 'EXPIRADA'
       AND p.estado = 'PENDIENTE';
END$$

DELIMITER ;

-- Al llegar su fecha de estreno, las peliculas en PREESTRENO pasan solas a EN_CARTELERA.
-- Corre una vez al dia, a las 00:05 (hora de Peru).
CREATE EVENT ev_estrenar_peliculas
ON SCHEDULE EVERY 1 DAY
STARTS TIMESTAMP(CURDATE() + INTERVAL 1 DAY, '00:05:00')
DO
    UPDATE pelicula
       SET estado = 'EN_CARTELERA'
     WHERE estado = 'PREESTRENO'
       AND fecha_estreno <= CURDATE();

-- ============================================================
-- CONSULTAS DE VERIFICACION (comentadas: quitar el "-- " para revisar una tabla)
-- ============================================================

-- Catálogos y configuración
-- SELECT * FROM rol;
-- SELECT * FROM sede;
-- SELECT * FROM genero;
-- SELECT * FROM tipo_sala;
-- SELECT * FROM tipo_entrada;
-- SELECT * FROM promocion;
-- SELECT * FROM cupon;
-- SELECT * FROM producto;
-- SELECT * FROM pelicula;

-- Usuarios y estructura física
-- SELECT * FROM cliente;
-- SELECT * FROM empleado;
-- SELECT * FROM sala;
-- SELECT * FROM butaca;

-- Operaciones principales
-- SELECT * FROM funcion;
-- SELECT * FROM compra;
-- SELECT * FROM entrada;
-- SELECT * FROM transaccion_pago;
-- SELECT * FROM pedido;

-- Servicio postventa
-- SELECT * FROM reclamo;
-- SELECT * FROM calificacion;

-- Tablas intermedias
-- SELECT * FROM pelicula_genero;
-- SELECT * FROM promocion_dia;
-- SELECT * FROM inventario_sede;
-- SELECT * FROM detalle_pedido;
