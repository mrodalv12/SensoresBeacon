//==============================================================================================
//Nombre: logicaNegocio.js
//Descripcion: Logica de negocio sin base de datos: normaliza los datos de una medicion y
//              los deja preparados para el repositorio.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica de Negocio - Separada del Servidor REST y de la Base de Datos
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Ubicación: /Users/rodolfo/Desktop/promts/Logical_Design_RE_Rules_v3.pdf
 * 
 * Tipos de datos abstractos utilizados: N, Z, R, Text, [Tipo]
 */

/**
 * guardarMedicion(medicion: Medicion, uuid: Text, minor: N, major: N, txPower: Z) --> guardarMedicion()
 * 
 * Parámetros de entrada (-->):
 *   - medicion: Medicion  (contiene tipo: Text, valor: R)
 *   - uuid: Text
 *   - minor: N
 *   - major: N
 *   - txPower: Z
 * 
 * Salidas (<--):
 *   - resultado: { ok: B, ... } o estado de operación
 * 
 * Interacción (--x):
 *   - Envía datos procesados hacia la capa de Base de Datos (puerto de abstracción)
 */
// guardarMedicion(): normaliza la medicion y los datos del beacon y devuelve
// el resultado ya procesado, sin tocar la base de datos.
function guardarMedicion(medicion, uuid, minor, major, txPower) {
    // Validación básica de parámetros
    const tipo = medicion && medicion.tipo !== undefined ? medicion.tipo : (medicion && medicion.tipoDeMedicion);
    const valor = medicion && medicion.valor !== undefined ? medicion.valor : medicion;

    // Procesamiento de datos recibidos del Beacon y la medición
    const datosProcesados = {
        tipo: typeof tipo === 'string' ? tipo : String(tipo || ''),
        valor: typeof valor === 'number' ? valor : Number(valor || 0),
        uuid: typeof uuid === 'string' ? uuid : (uuid || ''),
        minor: Number(minor || 0),
        major: Number(major || 0),
        txPower: Number(txPower || 0),
        nombreEmisora: medicion && medicion.nombreEmisora ? medicion.nombreEmisora : ''
    };

    // En esta capa NO hay acceso directo a BD. Solo procesa y prepara para enviar.
    return {
        ok: true,
        procesados: datosProcesados
    };
}

/**
 * obtenerMedicion() --> [MedicionResumen]
 * 
 * Parámetros de entrada (-->):
 *   - (ninguno)
 * 
 * Salidas (<--):
 *   - [Tipo]: lista de elementos con fecha_y_hora: Text, tipo: Text, valor: R
 * 
 * Interacción (--x):
 *   - Obtiene datos desde la capa de Base de Datos
 */
// obtenerMedicion(): devuelve las ultimas 10 mediciones; sin repositorio
// devuelve una lista vacia.
function obtenerMedicion() {
    // Devuelve lista de las últimas 10 mediciones registradas
    // Cada elemento: fecha_y_hora (Text), tipo (Text), valor (R)
    const mediciones = [];

    // Placeholder: la implementación real obtendrá desde BD
    // Retorna array vacío por defecto (preparado para inyectar repositorio)
    return mediciones.slice(-10);
}

module.exports = {
    guardarMedicion,
    obtenerMedicion
};
