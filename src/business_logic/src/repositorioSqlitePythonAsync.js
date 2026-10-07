//==============================================================================================
//Nombre: repositorioSqlitePythonAsync.js
//Descripcion: Repositorio SQLite asincrono: delega el INSERT y el SELECT en python3 mediante
//              procesos hijo.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Repositorio SQLite usando Python3 (async)
 */

const { spawn } = require('child_process');
const path = require('path');

const DB_PATH = path.join(__dirname, '../../database/mediciones.db');

// guardar(): inserta la medicion en SQLite con un proceso python3 hijo y
// devuelve una promesa con el id. Solo guarda tipo, nombre de cliente
// (nombre_emisora = nombre_cliente), valor y fecha_y_hora.
function guardar(datos) {
    return new Promise((resolve, reject) => {
        const tipo = datos.tipo_de_medicion || datos.tipo || '';
        const valor = datos.valor || 0;
        const nombre = datos.nombre_cliente || datos.nombre_emisora || datos.nombreEmisora || '';

        const sql = `INSERT INTO Mediciones (tipo_de_medicion, nombre_cliente, valor) 
                     VALUES ('${tipo.replace(/'/g, "''")}', '${nombre.replace(/'/g, "''")}', ${valor});`;
        
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

// obtenerUltimas10(): lee las mediciones con un proceso python3 hijo y
// devuelve una promesa con las ultimas 10.
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
