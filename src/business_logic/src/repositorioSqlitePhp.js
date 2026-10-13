//==============================================================================================
//Nombre: repositorioSqlitePhp.js
//Descripcion: Repositorio SQLite sincrono: delega el INSERT y el SELECT en el PHP del XAMPP
//              (extension PDO + sqlite3), sin dependencias externas de Node.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Repositorio SQLite usando el PHP del XAMPP (extension PDO + sqlite3): evita
 * la dependencia externa node-sqlite3. Separado de la Lógica de Negocio.
 *
 * El puente al motor SQLite es un proceso externo `php` con un script PDO
 * embebido. El intérprete se toma de la variable de entorno PHP_BIN o, si no,
 * del XAMPP instalado en macOS.
 */

const { spawnSync } = require('child_process');
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
    $st = $db->prepare("INSERT INTO Mediciones (tipo_de_medicion, nombre_cliente, valor, fecha_y_hora) VALUES (:tipo, :nombre, :valor, datetime('now','localtime'))");
    $st->execute([":tipo" => $d["tipo_de_medicion"] ?? "", ":nombre" => $d["nombre_cliente"] ?? "", ":valor" => $d["valor"] ?? 0]);
    echo json_encode(["id" => (int)$db->lastInsertId()]);
} else {
    $rows = $db->query("SELECT fecha_y_hora, tipo_de_medicion, valor FROM Mediciones ORDER BY id ASC")->fetchAll(PDO::FETCH_ASSOC);
    $out = [];
    foreach ($rows as $r) {
        $out[] = ["fecha_y_hora" => $r["fecha_y_hora"], "tipo" => $r["tipo_de_medicion"], "valor" => (float)$r["valor"]];
    }
    echo json_encode($out);
}
`;

//--------------------
// entrada: Text --> ejecutarPhp() --> Text
//   Lanza el puente PHP con la entrada JSON recibida y devuelve su salida JSON.
//--------------------
function ejecutarPhp(entrada) {
    const res = spawnSync(binarioPhp(), ['-r', PHP_SQLITE, entrada], {
        encoding: 'utf8',
        env: Object.assign({}, process.env, { DB_PATH })
    });
    if (res.status !== 0) {
        throw new Error(res.stderr || 'Error del puente PHP con la BD');
    }
    return res.stdout;
}

//--------------------
// datos: MedicionNueva --> guardar() --> { id: N }
//   Inserta la medicion en SQLite a traves del puente PHP y devuelve el id.
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

    try {
        return JSON.parse(ejecutarPhp(entrada));
    } catch (e) {
        throw new Error('Respuesta invalida del puente PHP');
    }
}

//--------------------
// obtenerUltimas10() --> [ Medicion ]
//   Lee las mediciones de SQLite a traves del puente PHP y devuelve las ultimas 10.
//--------------------
function obtenerUltimas10() {
    try {
        const rows = JSON.parse(ejecutarPhp(JSON.stringify({ accion: 'obtenerUltimas10' })));
        return rows.slice(-10);
    } catch (e) {
        return [];
    }
}

module.exports = { guardar, obtenerUltimas10, DB_PATH };
