-- ============================================================================
-- Tabla : Mediciones
-- Motor : SQLite 3
-- Ref.  : Logical_Design_RE_Rules_v3.pdf
--         Seccion 1 (Fundamental Data Types)  -> N, Z, R, B, Text
--         Seccion 2 (Enumerations)            -> tipos con dominio finito
--         Seccion 3 (Variable Naming)         -> identificadores en minuscula,
--                                                tipos en PascalCase
-- ============================================================================

CREATE TABLE Mediciones (

    -- N (Natural Number) -> INTEGER PRIMARY KEY AUTOINCREMENT
    -- SQLite solo admite AUTOINCREMENT si la PK entera es alias de rowid,
    -- por eso el tipo es INTEGER y no INT / BIGINT.
    
    id               INTEGER PRIMARY KEY AUTOINCREMENT,

    -- Text -> TEXT NOT NULL
    -- SQLite no diferencia TEXT de VARCHAR: ambos tienen afinidad TEXT.
    -- NOT NULL representa el "obligatorio" de la especificacion.
    
    tipo_de_medicion TEXT    NOT NULL,

    -- R (Real Numbers) -> REAL NOT NULL
    -- REAL es el unico tipo con afinidad REAL (punto flotante IEEE 754).
    -- No usar NUMERIC: su afinidad convertiria enteros a real y ocultaria
    -- el error, en lugar de forzar al dato a ser numerico real.
    
    valor            REAL    NOT NULL,

    -- Text -> TEXT NOT NULL (obligatorio).
    
    nombre_emisora   TEXT    NOT NULL,

    -- La Seccion 1 del PDF no define un tipo fecha/hora y SQLite no tiene
    -- tipo temporal nativo. CURRENT_TIMESTAMP produce 'YYYY-MM-DD HH:MM:SS';
    -- declararlo DATETIME le da afinidad NUMERIC y, al no ser un numero
    -- bien formado, el valor queda almacenado como texto.
    
    fecha_y_hora     DATETIME DEFAULT CURRENT_TIMESTAMP,

    -- Text, opcional: la especificacion no lo declara obligatorio, asi que
    -- se permite NULL. Segun la Seccion 2.1 del PDF, si el dominio de valores
    -- fuera finito (p.ej. un UUID versionado) procederia un tipo enumerado
    -- explicito; al no especificarse los valores se mantiene TEXT.
    
    uuid             TEXT,

    -- Z (Integers) -> INTEGER (64 bits con signo).
    -- Sin NOT NULL: son atributos opcionales del dispositivo emisor.
    
    major            INTEGER,
    minor            INTEGER,

    -- Z (Integers) -> INTEGER.
    -- La Seccion 3 del PDF exige identificadores en minuscula (major, minor),
    -- por lo que "txPower" incumple la convencion. Se conserva tal cual
    -- porque la especificacion lo nombra de esa forma.
    
    txPower          INTEGER
);

-- Nota de diseño: si se requiere que un mismo registro se identifique de
-- forma univoca por su uuid, añadir UNIQUE sobre la columna uuid.

-- ----------------------------------------------------------------------------
-- Datos de ejemplo
-- ----------------------------------------------------------------------------
-- id y fecha_y_hora se omiten: los genera SQLite (AUTOINCREMENT y
-- CURRENT_TIMESTAMP). Reconstruir la base desde cero:
--     sqlite3 mediciones.db < mediciones.sql
INSERT INTO Mediciones
    (tipo_de_medicion, valor, nombre_emisora, uuid, major, minor, txPower)
VALUES
    ('RSSI', -72.5, 'emisor-A', 'abc-123', 4, 2, 7);
