/**
 * Capa de Lógica de Negocio - Stubs.
 * 
 * NOTA: No contiene acceso a datos ni lógica de negocio completa.
 * Únicamente define las firmas necesarias para que el Servidor REST las invoque,
 * conforme a lo solicitado.
 */
class LogicaNegocioMediciones {
    /**
     * guardarMedicion() - Stub
     * @param {Object} datos - Datos de la medición y beacon
     */
    static guardarMedicion(datos) {
        // Stub: En implementación posterior se añadirá lógica de negocio
        return { ok: true, datos };
    }

    /**
     * obtenerMedicion() - Stub
     * @returns {Array<Object>} Lista de mediciones
     */
    static obtenerMedicion() {
        // Stub: Devuelve array vacío; en implementación posterior con BD
        return [];
    }
}

module.exports = { LogicaNegocioMediciones };
