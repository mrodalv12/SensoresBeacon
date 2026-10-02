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
     * guardarMedicion() 
     */
    static async guardarMedicion(datos) {
        const beacon = datos && datos.beacon || {};
        const medicion = datos && datos.medicion || {};
        
        return await logica.guardarMedicion(
            medicion,
            beacon.uuid,
            beacon.minor,
            beacon.major,
            beacon.txPower
        );
    }

    /**
     * obtenerMedicion() 
     */
    static async obtenerMedicion() {
        return await logica.obtenerMedicion();
    }
}

module.exports = { LogicaNegocioMediciones };
