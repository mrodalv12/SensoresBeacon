-- ============================================================================
-- Tabla : Mediciones
-- Motor : SQLite 3
-- Ref.  : Logical_Design_RE_Rules_v3.pdf
--         Seccion 1 (Fundamental Data Types)  -> N, Z, R, B, Text
--         Seccion 2 (Enumerations)            -> tipos con dominio finito
--         Seccion 3 (Variable Naming)         -> identificadores en minuscula,
--                                                tipos en PascalCase
-- Nota  : La tabla guarda unicamente tipo de medicion, nombre de cliente,
--         valor y fecha/hora. El "nombre de emisora" que envia la app en el
--         campo nombre_emisora es el nombre del cliente: aqui se almacena en
--         la columna nombre_cliente.
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

    -- Text -> TEXT NOT NULL (obligatorio).
    -- Es el nombre de emisora que envia la app (nombre_emisora) = nombre del
    -- cliente. Se almacena como nombre_cliente.
    
    nombre_cliente   TEXT    NOT NULL,

    -- R (Real Numbers) -> REAL NOT NULL
    -- REAL es el unico tipo con afinidad REAL (punto flotante IEEE 754).
    -- No usar NUMERIC: su afinidad convertiria enteros a real y ocultaria
    -- el error, en lugar de forzar al dato a ser numerico real.
    
    valor            REAL    NOT NULL,

    -- La Seccion 1 del PDF no define un tipo fecha/hora y SQLite no tiene
    -- tipo temporal nativo. datetime('now','localtime') produce
    -- 'YYYY-MM-DD HH:MM:SS' en hora LOCAL del sistema; declararlo DATETIME le
    -- da afinidad NUMERIC y, al no ser un numero bien formado, el valor queda
    -- almacenado como texto. No usar CURRENT_TIMESTAMP: devuelve UTC y
    -- desfasa la hora (p. ej. 2 h en verano en Espana).
    
    fecha_y_hora     DATETIME DEFAULT (datetime('now','localtime'))
);

-- ----------------------------------------------------------------------------
-- Datos de ejemplo
-- ----------------------------------------------------------------------------
-- id y fecha_y_hora se omiten: los genera SQLite (AUTOINCREMENT y
-- datetime('now','localtime')). Reconstruir la base desde cero:
--     sqlite3 mediciones.db < mediciones.sql
INSERT INTO Mediciones
    (tipo_de_medicion, nombre_cliente, valor)
VALUES
    ('co2', 'GTI-3A-MJ', 812);
