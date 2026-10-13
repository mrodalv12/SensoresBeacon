//==============================================================================================
//Nombre: repositorioSqlitePhpAsync.js
//Descripcion: Repositorio SQLite asincrono: delega el INSERT y el SELECT en el PHP del XAMPP
//              (extension PDO + sqlite3) mediante procesos hijo.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Repositorio SQLite usando el PHP del XAMPP (extension PDO + sqlite3) de forma
 * asincrona. El puente al motor SQLite es un proceso `php` con un script PDO
 * embebido.
 */

const { spawn } = require('child_process');
const fs = require('fs');
const path = require('path');

const DB_PATH = path.join(__dirname, '../../database/mediciones.db');

// Ruta por defecto del interprete PHP del XAMPP en macOS.
const PHP_POR_DEFECTO = '/Applications/XAMPP/xamppfiles/bin/php';

//--------------------
// binarioPhp() --> Text
//   Devuelve la ruta del interprete PHP a usar: la de PHP_BIN si existe, si no
//   la del XAMPP por defecto y, como ultimo recurso, "php" (del PATH).
//--------------------
function binarioPhp() {
    if (process.env.PHP_BIN && fs.existsSync(process.env.PHP_BIN)) {
        return process.env.PHP_BIN;
    }
    if (fs.existsSync(PHP_POR_DEFECTO)) {
        return PHP_POR_DEFECTO;
    }
    return 'php';
}

// Script PHP que actua de puente con SQLite. Recibe por argv[1] un JSON con la
// accion ("guardar" con datos, u "obtenerUltimas10") y escribe el resultado en
// JSON. La base se abre con PDO y sentencias preparadas: no hay que escapar
// comillas a mano ni construir SQL por concatenacion.
const PHP_SQLITE = `
ini_set("serialize_precision", "-1");
$cmd = json_decode($argv[1] ?? "{}", true);
$db = new PDO("sqlite:" . getenv("DB_PATH"));
$db->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
if (($cmd["accion"] ?? "") === "guardar") {
    $d = $cmd["datos"] ?? [];
    $st = $db->prepare("INSERT INTO Mediciones (tipo_de_medicion, nombre_cliente, valor, fecha_y_hora) VALUES (:tipo, :nombre, :valor, strftime('%Y-%m-%d %H:%M:%f','now','localtime'))");
    $st->execute([":tipo" => $d["tipo_de_medicion"] ?? "", ":nombre" => $d["nombre_cliente"] ?? "", ":valor" => $d["valor"] ?? 0]);
    $fecha = $db->query("SELECT fecha_y_hora FROM Mediciones WHERE rowid = last_insert_rowid()")->fetchColumn();
    echo json_encode(["nombre_cliente" => $d["nombre_cliente"] ?? "", "fecha_y_hora" => $fecha]);
} else {
    $rows = $db->query("SELECT fecha_y_hora, tipo_de_medicion, valor FROM Mediciones ORDER BY fecha_y_hora ASC")->fetchAll(PDO::FETCH_ASSOC);
    $out = [];
    foreach ($rows as $r) {
        $out[] = ["fecha_y_hora" => $r["fecha_y_hora"], "tipo" => $r["tipo_de_medicion"], "valor" => (float)$r["valor"]];
    }
    echo json_encode($out);
}
`;

//--------------------
// entrada: Text --> ejecutarPhp() --> Promise<Text>
//   Lanza el puente PHP con un proceso hijo y devuelve una promesa con su
//   salida JSON.
//--------------------
function ejecutarPhp(entrada) {
    return new Promise((resolve, reject) => {
        const p = spawn(binarioPhp(), ['-r', PHP_SQLITE, entrada], {
            env: Object.assign({}, process.env, { DB_PATH })
        });
        let out = '', err = '';
        p.stdout.on('data', d => out += d);
        p.stderr.on('data', d => err += d);
        p.on('close', code => {
            if (code !== 0) return reject(new Error(err));
            resolve(out);
        });
    });
}

//--------------------
// datos: MedicionNueva --> guardar() --> Promise<{ nombre_cliente: Text, fecha_y_hora: Text }>
//   Inserta la medicion en SQLite con un proceso PHP hijo y devuelve una
//   promesa con la clave compuesta (emisora + fecha/hora con milisegundos).
//   Solo guarda tipo, nombre de cliente (nombre_emisora = nombre_cliente),
//   valor y fecha_y_hora.
//--------------------
function guardar(datos) {
    const tipo = datos.tipo_de_medicion || datos.tipo || '';
    const valor = datos.valor || 0;
    const nombre = datos.nombre_cliente || datos.nombre_emisora || datos.nombreEmisora || '';

    // fecha_y_hora se genera explicitamente en hora LOCAL del sistema
    // (datetime('now','localtime')), igual que el DEFAULT de la tabla. No se
    // usa CURRENT_TIMESTAMP porque devuelve UTC y desfasaria la hora.
    const entrada = JSON.stringify({
        accion: 'guardar',
        datos: { tipo_de_medicion: tipo, nombre_cliente: nombre, valor: valor }
    });

    return ejecutarPhp(entrada).then(salida => {
        try {
            return JSON.parse(salida);
        } catch (e) {
            throw new Error('Respuesta invalida del puente PHP');
        }
    });
}

//--------------------
// obtenerUltimas10() --> Promise<[ Medicion ]>
//   Lee las mediciones con un proceso PHP hijo y devuelve una promesa con las
//   ultimas 10.
//--------------------
function obtenerUltimas10() {
    return ejecutarPhp(JSON.stringify({ accion: 'obtenerUltimas10' })).then(salida => {
        try {
            return JSON.parse(salida).slice(-10);
        } catch (e) {
            return [];
        }
    });
}

module.exports = { guardar, obtenerUltimas10, DB_PATH };
