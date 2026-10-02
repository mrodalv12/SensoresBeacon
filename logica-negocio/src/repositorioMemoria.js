/**
 * Repositorio en memoria para pruebas
 * Simula acceso a BD sin modificar la BD real
 */
let mediciones = [];

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

function obtenerUltimas10() {
    return mediciones.slice(-10).map(m => ({
        fecha_y_hora: m.fecha_y_hora,
        tipo: m.tipo || m.tipo_de_medicion || '',
        valor: m.valor || 0
    }));
}

function limpiar() {
    mediciones = [];
}

module.exports = { guardar, obtenerUltimas10, limpiar, mediciones };
