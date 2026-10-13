//==============================================================================================
//Nombre: logicaNegocioSqlitePhp.js
//Descripcion: Logica de negocio sincrona preparada para trabajar con un repositorio SQLite
//              (PHP).
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica de Negocio con acceso a Base de Datos (vía PHP)
 * Mantiene separación de capas
 */

class LogicaNegocioSQLitePhp {
    //--------------------
    // repositorio: Repositorio --> constructor() --> nueva instancia
    //   Guarda el repositorio en el que se apoyara la logica.
    //--------------------
    constructor(repositorio) {
        this.repositorio = repositorio;
    }

    //--------------------
    // medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, identificador_placa: Text, fecha_y_hora: Text )
    //   Normaliza la medicion y la guarda en el repositorio; devuelve la clave
    //   compuesta. Solo recibe el dominio (tipo, valor e identificador de placa):
    //   ningun dato del beacon.
    //--------------------
    async guardarMedicion(medicion) {
        const tipo = medicion && (medicion.tipo !== undefined ? medicion.tipo : medicion.tipoDeMedicion);
        const valor = medicion && (medicion.valor !== undefined ? medicion.valor : medicion.valor);

        const datos = {
            tipo_de_medicion: typeof tipo === 'string' ? tipo : String(tipo || 'desconocido'),
            identificador_placa: (medicion && (medicion.identificador_placa || medicion.identificadorPlaca)) || '',
            valor: Number(valor || 0)
        };

        const resultado = await this.repositorio.guardar(datos);
        return {
            ok: true,
            identificador_placa: resultado && resultado.identificador_placa,
            fecha_y_hora: resultado && resultado.fecha_y_hora
        };
    }

    //--------------------
    // obtenerMedicion() --> [ Medicion ]
    //   Pide al repositorio las ultimas 10 mediciones y las devuelve con
    //   fecha_y_hora, tipo y valor.
    //--------------------
    async obtenerMedicion() {
        const mediciones = await this.repositorio.obtenerUltimas10();
        return (mediciones || []).map(m => ({
            fecha_y_hora: m.fecha_y_hora,
            tipo: m.tipo || m.tipo_de_medicion || '',
            valor: Number(m.valor || 0)
        }));
    }
}

module.exports = { LogicaNegocioSQLitePhp };
