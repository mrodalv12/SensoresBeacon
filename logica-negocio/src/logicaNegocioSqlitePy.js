//==============================================================================================
//Nombre: logicaNegocioSqlitePy.js
//Descripcion: Logica de negocio sincrona preparada para trabajar con un repositorio SQLite
//              (Python).
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica de Negocio con acceso a Base de Datos (vía Python)
 * Mantiene separación de capas
 */

class LogicaNegocioSQLitePy {
    // constructor( repositorio ): guarda el repositorio en el que se apoyara
    // la logica.
    constructor(repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * guardarMedicion(medicion: Medicion, uuid: Text, minor: N, major: N, txPower: Z) -->
     */
    /**
     * guardarMedicion(): normaliza la medicion y la guarda en el repositorio;
     * devuelve el id con el que queda registrada. Los datos del beacon
     * (uuid, minor, major, txPower) se reciben pero no se persisten.
     */
    async guardarMedicion(medicion, uuid, minor, major, txPower) {
        const tipo = medicion && (medicion.tipo !== undefined ? medicion.tipo : medicion.tipoDeMedicion);
        const valor = medicion && (medicion.valor !== undefined ? medicion.valor : medicion.valor);

        const datos = {
            tipo_de_medicion: typeof tipo === 'string' ? tipo : String(tipo || 'desconocido'),
            nombre_cliente: (medicion && (medicion.nombre_cliente || medicion.nombreEmisora || medicion.nombre_emisora)) || '',
            valor: Number(valor || 0)
        };

        const resultado = await this.repositorio.guardar(datos);
        return { ok: true, id: resultado && resultado.id };
    }

    /**
     * obtenerMedicion() --> [Tipo]
     */
    /**
     * obtenerMedicion(): pide al repositorio las ultimas 10 mediciones y las
     * devuelve con fecha_y_hora, tipo y valor.
     */
    async obtenerMedicion() {
        const mediciones = await this.repositorio.obtenerUltimas10();
        return (mediciones || []).map(m => ({
            fecha_y_hora: m.fecha_y_hora,
            tipo: m.tipo || m.tipo_de_medicion || '',
            valor: Number(m.valor || 0)
        }));
    }
}

module.exports = { LogicaNegocioSQLitePy };
