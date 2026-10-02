/**
 * Lógica de Negocio con acceso a repositorio (BD)
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf"
 * Tipos de datos abstractos: N, Z, R, Text, [Tipo]
 */

class LogicaNegocio {
    constructor(repositorio) {
        this.repositorio = repositorio;
    }

    /**
     * guardarMedicion(medicion: Medicion, uuid: Text, minor: N, major: N, txPower: Z) -->
     * 
     * Parámetros de entrada (-->):
     *   - medicion: Medicion (tipo: Text, valor: R, nombreEmisora?: Text)
     *   - uuid: Text
     *   - minor: N
     *   - major: N
     *   - txPower: Z
     * 
     * Salidas (<--):
     *   - { ok: Boolean, id?: N }
     * 
     * Interacción (--x): Envía a repositorio/BD
     */
    guardarMedicion(medicion, uuid, minor, major, txPower) {
        const tipo = medicion && (medicion.tipo !== undefined ? medicion.tipo : medicion.tipoDeMedicion);
        const valor = medicion && (medicion.valor !== undefined ? medicion.valor : medicion.valor);

        const datos = {
            tipo_de_medicion: typeof tipo === 'string' ? tipo : String(tipo || 'desconocido'),
            valor: Number(valor || 0),
            nombre_emisora: medicion && medicion.nombreEmisora ? medicion.nombreEmisora : (medicion && medicion.nombre_emisora) || '',
            uuid: typeof uuid === 'string' ? uuid : String(uuid || ''),
            major: Number(major || 0),
            minor: Number(minor || 0),
            txPower: Number(txPower || 0)
        };

        const resultado = this.repositorio.guardar(datos);
        return { ok: true, id: resultado && resultado.id };
    }

    /**
     * obtenerMedicion() --> [Tipo]
     * 
     * Parámetros de entrada (-->): ninguno
     * Salidas (<--): Array<{ fecha_y_hora: Text, tipo: Text, valor: R }>
     * Interacción (--x): Obtiene desde repositorio/BD
     */
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
