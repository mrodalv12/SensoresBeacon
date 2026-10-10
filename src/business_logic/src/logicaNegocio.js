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
 * Especificacion: "Logical_Design_RE_Rules_v3.pdf" (adjunta al enunciado)
 * 
 * Tipos de datos abstractos utilizados: N, Z, R, Text, [Tipo]
 */

//--------------------
// medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, procesados: MedicionNueva )
//   Normaliza la medicion y devuelve el resultado ya procesado, sin tocar la
//   base de datos. No recibe ningun dato del beacon (uuid, txPower, major,
//   minor, rssi): esos se quedan en la app Android.
//--------------------
function guardarMedicion(medicion) {
    // Validación básica de parámetros
    const tipo = medicion && medicion.tipo !== undefined ? medicion.tipo : (medicion && medicion.tipoDeMedicion);
    const valor = medicion && medicion.valor !== undefined ? medicion.valor : medicion;

    // Procesamiento de la medicion
    const datosProcesados = {
        tipo: typeof tipo === 'string' ? tipo : String(tipo || ''),
        valor: typeof valor === 'number' ? valor : Number(valor || 0),
        nombreEmisora: (medicion && (medicion.nombreEmisora || medicion.nombre_emisora)) || ''
    };

    // En esta capa NO hay acceso directo a BD. Solo procesa y prepara para enviar.
    return {
        ok: true,
        procesados: datosProcesados
    };
}

//--------------------
// obtenerMedicion() --> [ Medicion ]
//   Devuelve las ultimas 10 mediciones (fecha_y_hora, tipo, valor); sin
//   repositorio devuelve una lista vacia.
//--------------------
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
