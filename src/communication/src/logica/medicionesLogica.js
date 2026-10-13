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

const { LogicaNegocioSQLitePhpAsync } = require('../../../business_logic/src/logicaNegocioSqlitePhpAsync');
const repo = require('../../../business_logic/src/repositorioSqlitePhpAsync');

const logica = new LogicaNegocioSQLitePhpAsync(repo);

class LogicaNegocioMediciones {
    //--------------------
    // medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, id: N )
    //   Recibe la medicion del contrato POST (tipo, valor y nombre_emisora) y
    //   la pasa a la logica de negocio. Ningun dato del beacon viaja por aqui.
    //--------------------
    static async guardarMedicion(medicion) {
        return await logica.guardarMedicion(medicion || {});
    }

    //--------------------
    // obtenerMedicion() --> [ Medicion ]
    //   Pide a la logica de negocio las mediciones guardadas.
    //--------------------
    static async obtenerMedicion() {
        return await logica.obtenerMedicion();
    }
}

module.exports = { LogicaNegocioMediciones };
