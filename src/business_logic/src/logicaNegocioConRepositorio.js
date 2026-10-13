//==============================================================================================
//Nombre: logicaNegocioConRepositorio.js
//Descripcion: Logica de negocio sincrona que recibe un repositorio por constructor y delega
//              en el el guardado y la lectura de las mediciones.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Lógica de Negocio con acceso a repositorio (BD)
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Tipos de datos abstractos: N, Z, R, Text, [Tipo]
 */

class LogicaNegocio {
    //--------------------
    // repositorio: Repositorio --> constructor() --> nueva instancia
    //   Guarda el repositorio en el que se apoyara la logica.
    //--------------------
    constructor(repositorio) {
        this.repositorio = repositorio;
    }

    //--------------------
    // medicion: MedicionNueva --> guardarMedicion() --> ( ok: B, id: N )
    //   Normaliza tipo, valor e identificador de placa y delega el alta en el
    //   repositorio. No recibe datos del beacon.
    //--------------------
    guardarMedicion(medicion) {
        const tipo = medicion && (medicion.tipo !== undefined ? medicion.tipo : medicion.tipoDeMedicion);
        const valor = medicion && (medicion.valor !== undefined ? medicion.valor : medicion.valor);

        const datos = {
            tipo_de_medicion: typeof tipo === 'string' ? tipo : String(tipo || 'desconocido'),
            valor: Number(valor || 0),
            identificador_placa: medicion && (medicion.identificadorPlaca || medicion.identificador_placa) || ''
        };

        const resultado = this.repositorio.guardar(datos);
        return { ok: true, id: resultado && resultado.id };
    }

    //--------------------
    // obtenerMedicion() --> [ Medicion ]
    //   Pide al repositorio las ultimas 10 mediciones y las devuelve con
    //   fecha_y_hora, tipo y valor.
    //--------------------
    obtenerMedicion() {
        const mediciones = this.repositorio.obtenerUltimas10() || [];
        return mediciones.map(m => ({
            fecha_y_hora: m.fecha_y_hora || m.fechaYHora || new Date().toISOString(),
            tipo: m.tipo || m.tipo_de_medicion || '',
            valor: Number(m.valor || 0)
        }));
    }
}

module.exports = { LogicaNegocio };
