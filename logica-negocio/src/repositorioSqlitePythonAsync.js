/**
 * Repositorio SQLite usando Python3 (async)
 */

const { spawn } = require('child_process');
const path = require('path');

const DB_PATH = path.join(__dirname, '../../bbdd/mediciones.db');

function guardar(datos) {
    return new Promise((resolve, reject) => {
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
        const p = spawn('python3', ['-c', script]);
        let out = '', err = '';
        p.stdout.on('data', d => out += d);
        p.stderr.on('data', d => err += d);
        p.on('close', code => {
            if (code !== 0) return reject(new Error(err));
            resolve({ id: parseInt(out.trim()) || 0 });
        });
    });
}

function obtenerUltimas10() {
    return new Promise((resolve, reject) => {
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
        const p = spawn('python3', ['-c', script]);
        let out = '', err = '';
        p.stdout.on('data', d => out += d);
        p.stderr.on('data', d => err += d);
        p.on('close', code => {
            if (code !== 0) return reject(new Error(err));
            try { resolve(JSON.parse(out).slice(-10)); } catch(e) { resolve([]); }
        });
    });
}

module.exports = { guardar, obtenerUltimas10, DB_PATH };
