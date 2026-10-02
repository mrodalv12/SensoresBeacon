/**
 * Lógica de Negocio con acceso a Base de Datos (vía Python)
 * Mantiene separación de capas
 */

class LogicaNegocioSQLitePy {
    constructor(repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * guardarMedicion(medicion: Medicion, uuid: Text, minor: N, major: N, txPower: Z) -->
     */
    async guardarMedicion(medicion, uuid, minor, major, txPower) {
        const tipo = medicion && (medicion.tipo !== undefined ? medicion.tipo : medicion.tipoDeMedicion);
        const valor = medicion && (medicion.valor !== undefined ? medicion.valor : medicion.valor);

        const datos = {
            tipo_de_medicion: typeof tipo === 'string' ? tipo : String(tipo || 'desconocido'),
            valor: Number(valor || 0),
            nombre_emisora: (medicion && (medicion.nombreEmisora || medicion.nombre_emisora)) || '',
            uuid: typeof uuid === 'string' ? uuid : String(uuid || ''),
            major: Number(major || 0),
            minor: Number(minor || 0),
            txPower: Number(txPower || 0)
        };

        const resultado = await this.repositorio.guardar(datos);
        return { ok: true, id: resultado && resultado.id };
    }

    /**
     * obtenerMedicion() --> [Tipo]
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
