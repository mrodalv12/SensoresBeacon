//==============================================================================================
//Nombre: mediciones.js
//Descripcion: Logica de negocio clienta (proxy): encapsula la comunicacion HTTP con el
//              backend para que la GUI nunca haga transporte directamente.
//Copyright:
//Fecha de creacion: 2026-10-07
//Autores: Maria Jose Rodriguez
//==============================================================================================

/**
 * Logica de negocio de cliente (proxy) de las mediciones.
 *
 * La GUI (src/gui/) solo conoce este modulo: llama a mediciones.obtenerMedicion()
 * y aqui dentro se decide como viajan los datos (fetch al API que a su vez
 * consulta el servidor REST). Asi la GUI queda desacoplada del transporte.
 *
 * La firma logica es identica a la del backend (business_logic):
 *     obtenerMedicion() --> [ (fecha_y_hora: Text, tipo: Text, valor: R) ]
 * (regla de paridad de interface del frontend con el backend).
 */

const mediciones = (function () {

    // Ruta del endpoint que sirve las mediciones (frontend_business_logic/api).
    const RUTA_API = '/frontend_business_logic/api/mediciones.php';

    // Ultimo error de comunicacion, para que la GUI lo muestre si quiere.
    let mensajeError = '';

    // obtenerMedicion(): pide las mediciones al API y devuelve una lista; si la
    // peticion falla, anota el error y devuelve una lista vacia.
    async function obtenerMedicion() {
        try {
            const response = await fetch(RUTA_API);
            if (!response.ok) {
                throw new Error('Error en la petición HTTP');
            }
            const data = await response.json();
            mensajeError = '';
            return Array.isArray(data) ? data : [];
        } catch (error) {
            console.error('Error al obtener mediciones:', error);
            mensajeError = 'No se ha podido conectar con el servidor REST. Verifica la conexión.';
            return [];
        }
    }

    // errorActual(): devuelve el ultimo error de comunicacion ('' si no lo hay).
    function errorActual() {
        return mensajeError;
    }

    return { obtenerMedicion, errorActual };
})();