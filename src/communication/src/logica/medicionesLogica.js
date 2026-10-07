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

const { LogicaNegocioSQLitePyAsync } = require('../../../business_logic/src/logicaNegocioSqlitePyAsync');
const repo = require('../../../business_logic/src/repositorioSqlitePythonAsync');

const logica = new LogicaNegocioSQLitePyAsync(repo);

class LogicaNegocioMediciones {
    /**
     * guardarMedicion(): recibe la medicion del contrato POST (tipo, valor y
     * nombre_emisora) y se la pasa a la logica de negocio para que la guarde.
     * La logica de negocio es quien convierte nombre_emisora en la columna
     * nombre_cliente de la base de datos. Ningun dato del beacon viaja por aqui.
     */
    static async guardarMedicion(medicion) {
        return await logica.guardarMedicion(medicion || {});
    }

    /**
     * obtenerMedicion(): pide a la logica de negocio las mediciones guardadas.
     */
    static async obtenerMedicion() {
        return await logica.obtenerMedicion();
    }
}

module.exports = { LogicaNegocioMediciones };
