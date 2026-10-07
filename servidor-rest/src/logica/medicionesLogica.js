//==============================================================================================
//Nombre: medicionesLogica.js
//Descripcion: Capa de logica de negocio conectada a la base de datos: expone el guardado y la
//              lectura de las mediciones.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Capa de Lógica de Negocio - Conectada a Base de Datos
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Tipos de datos abstractos utilizados: N, Z, R, Text, [Tipo]
 */

const { LogicaNegocioSQLitePyAsync } = require('../../../logica-negocio/src/logicaNegocioSqlitePyAsync');
const repo = require('../../../logica-negocio/src/repositorioSqlitePythonAsync');

const logica = new LogicaNegocioSQLitePyAsync(repo);

class LogicaNegocioMediciones {
    /**
     * guardarMedicion(): separa los datos del beacon y los de la medicion, y
     * se los pasa a la logica de negocio para que los guarde. El nombre de
     * emisora del beacon (nombre_emisora) es el nombre del cliente y viaja con
     * la medicion para que se guarde en la columna nombre_cliente.
     */
    static async guardarMedicion(datos) {
        const beacon = datos && datos.beacon || {};
        const medicion = datos && datos.medicion || {};
        
        const medicionConNombre = beacon.nombre_emisora !== undefined
            ? { ...medicion, nombre_emisora: beacon.nombre_emisora }
            : medicion;

        return await logica.guardarMedicion(
            medicionConNombre,
            beacon.uuid,
            beacon.minor,
            beacon.major,
            beacon.txPower
        );
    }

    /**
     * obtenerMedicion(): pide a la logica de negocio las mediciones guardadas.
     */
    static async obtenerMedicion() {
        return await logica.obtenerMedicion();
    }
}

module.exports = { LogicaNegocioMediciones };
