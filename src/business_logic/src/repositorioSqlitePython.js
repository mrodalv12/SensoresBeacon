//==============================================================================================
//Nombre: repositorioSqlitePython.js
//Descripcion: Repositorio SQLite sincrono: delega el INSERT y el SELECT en python3, sin
//              dependencias externas de Node.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Repositorio SQLite usando Python3 (evita dependencia externa node-sqlite3)
 * Separado de Lógica de Negocio
 */

const { spawnSync } = require('child_process');
const path = require('path');

const DB_PATH = path.join(__dirname, '../../database/mediciones.db');

//--------------------
// datos: MedicionNueva --> guardar() --> { id: N }
//   Inserta la medicion en SQLite mediante python3 y devuelve el id. Solo
//   guarda tipo, nombre de cliente (nombre_emisora = nombre_cliente), valor y
//   fecha_y_hora.
//--------------------
function guardar(datos) {
    const tipo = datos.tipo_de_medicion || datos.tipo || '';
    const valor = datos.valor || 0;
    const nombre = datos.nombre_cliente || datos.nombre_emisora || datos.nombreEmisora || '';

    // fecha_y_hora se genera explicitamente en hora LOCAL del sistema
    // (datetime('now','localtime')), igual que el DEFAULT de la tabla. No se
    // usa CURRENT_TIMESTAMP porque devuelve UTC y desfasaria la hora.
    const sql = `INSERT INTO Mediciones (tipo_de_medicion, nombre_cliente, valor, fecha_y_hora) 
                 VALUES ('${tipo.replace(/'/g, "''")}', '${nombre.replace(/'/g, "''")}', ${valor}, datetime('now','localtime'));`;
    
    const script = `
import sqlite3
conn = sqlite3.connect('${DB_PATH}')
cur = conn.cursor()
cur.execute("""${sql.replace(/\\/g, '\\\\')}""")
lastid = cur.lastrowid
conn.commit()
conn.close()
print(lastid)
`;

    const res = spawnSync('python3', ['-c', script], { encoding: 'utf8' });
    if (res.status !== 0) {
        throw new Error(res.stderr || 'Error al guardar en BD');
    }
    const lastId = parseInt(res.stdout.trim()) || 0;
    return { id: lastId };
}

//--------------------
// obtenerUltimas10() --> [ Medicion ]
//   Lee las mediciones de SQLite mediante python3 y devuelve las ultimas 10.
//--------------------
function obtenerUltimas10() {
    const script = `
import sqlite3
import json
conn = sqlite3.connect('${DB_PATH}')
cur = conn.cursor()
cur.execute("SELECT fecha_y_hora, tipo_de_medicion, valor FROM Mediciones ORDER BY id ASC")
rows = cur.fetchall()
conn.close()
result = []
for r in rows:
    result.append({'fecha_y_hora': r[0], 'tipo': r[1], 'valor': r[2]})
print(json.dumps(result))
`;

    const res = spawnSync('python3', ['-c', script], { encoding: 'utf8' });
    if (res.status !== 0) {
        throw new Error(res.stderr || 'Error al leer BD');
    }
    try {
        const rows = JSON.parse(res.stdout);
        return rows.slice(-10);
    } catch (e) {
        return [];
    }
}

module.exports = { guardar, obtenerUltimas10, DB_PATH };
