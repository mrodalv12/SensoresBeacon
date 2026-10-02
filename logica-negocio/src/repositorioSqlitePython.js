/**
 * Repositorio SQLite usando Python3 (evita dependencia externa node-sqlite3)
 * Separado de Lógica de Negocio
 */

const { spawnSync } = require('child_process');
const path = require('path');

const DB_PATH = path.join(__dirname, '../../bbdd/mediciones.db');

function guardar(datos) {
    const tipo = datos.tipo_de_medicion || datos.tipo || '';
    const valor = datos.valor || 0;
    const nombre = datos.nombre_emisora || datos.nombreEmisora || '';
    const uuid = datos.uuid || '';
    const major = datos.major === null || datos.major === undefined ? 'NULL' : datos.major;
    const minor = datos.minor === null || datos.minor === undefined ? 'NULL' : datos.minor;
    const txPower = datos.txPower === null || datos.txPower === undefined ? 'NULL' : datos.txPower;

    const sql = `INSERT INTO Mediciones (tipo_de_medicion, valor, nombre_emisora, uuid, major, minor, txPower) 
                 VALUES ('${tipo.replace(/'/g, "''")}', ${valor}, '${nombre.replace(/'/g, "''")}', 
                         ${uuid ? "'" + uuid.replace(/'/g, "''") + "'" : 'NULL'}, 
                         ${major === 'NULL' ? 'NULL' : major}, 
                         ${minor === 'NULL' ? 'NULL' : minor}, 
                         ${txPower === 'NULL' ? 'NULL' : txPower});`;
    
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
