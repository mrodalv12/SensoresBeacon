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

    -- Text -> TEXT NOT NULL
    -- SQLite no diferencia TEXT de VARCHAR: ambos tienen afinidad TEXT.
    -- NOT NULL representa el "obligatorio" de la especificacion.

    tipo_de_medicion TEXT     NOT NULL,

    -- Text -> TEXT NOT NULL (obligatorio).
    -- Es el nombre de emisora que envia la app (nombre_emisora) = nombre del
    -- cliente. Se almacena como nombre_cliente.

    nombre_cliente   TEXT     NOT NULL,

    -- R (Real Numbers) -> REAL NOT NULL
    -- REAL es el unico tipo con afinidad REAL (punto flotante IEEE 754).
    -- No usar NUMERIC: su afinidad convertiria enteros a real y ocultaria
    -- el error, en lugar de forzar al dato a ser numerico real.

    valor            REAL     NOT NULL,

    -- La Seccion 1 del PDF no define un tipo fecha/hora y SQLite no tiene
    -- tipo temporal nativo. strftime('%Y-%m-%d %H:%M:%f','now','localtime')
    -- produce 'YYYY-MM-DD HH:MM:SS.SSS' (con milisegundos) en hora LOCAL del
    -- sistema. La precision de milisegundos evita que dos mediciones de la
    -- misma emisora hechas en el mismo segundo colisionen en la clave primaria.
    -- No usar CURRENT_TIMESTAMP: devuelve UTC y desfasa la hora.

    fecha_y_hora     DATETIME NOT NULL DEFAULT (strftime('%Y-%m-%d %H:%M:%f','now','localtime')),

    -- Clave primaria compuesta: la identidad natural de la fila es la emisora
    -- mas la marca de tiempo. Se prescinde de una columna id autonumerica.

    PRIMARY KEY (nombre_cliente, fecha_y_hora)
);

-- ----------------------------------------------------------------------------
-- Datos de ejemplo
-- ----------------------------------------------------------------------------
-- fecha_y_hora se omite: la genera SQLite (strftime) en hora local.
-- Reconstruir la base desde cero:
--     sqlite3 mediciones.db < mediciones.sql
INSERT INTO Mediciones
    (tipo_de_medicion, nombre_cliente, valor)
VALUES
    ('ozono', 'GTI-3A-MJ', 812);
