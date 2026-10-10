//==============================================================================================
//Nombre: repositorioMemoria.js
//Descripcion: Repositorio en memoria para pruebas: guarda las mediciones en un array, sin
//              tocar la base de datos real.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Repositorio en memoria para pruebas
 * Simula acceso a BD sin modificar la BD real
 */
let mediciones = [];

//--------------------
// medicion: MedicionNueva --> guardar() --> Medicion
//   Anade la medicion al array en memoria, con su fecha actual.
//--------------------
function guardar(medicion) {
    const nueva = {
        fecha_y_hora: new Date().toISOString(),
        tipo: medicion.tipo || medicion.tipo_de_medicion || '',
        valor: medicion.valor || 0,
        ...medicion
    };
    mediciones.push(nueva);
    return nueva;
}

//--------------------
// obtenerUltimas10() --> [ Medicion ]
//   Devuelve las ultimas 10 mediciones con solo fecha, tipo y valor.
//--------------------
function obtenerUltimas10() {
    return mediciones.slice(-10).map(m => ({
        fecha_y_hora: m.fecha_y_hora,
        tipo: m.tipo || m.tipo_de_medicion || '',
        valor: m.valor || 0
    }));
}

//--------------------
// limpiar() -->
//   Vacia el array de mediciones, para empezar de cero.
//--------------------
function limpiar() {
    mediciones = [];
}

module.exports = { guardar, obtenerUltimas10, limpiar, mediciones };
