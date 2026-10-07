//==============================================================================================
//Nombre: medicionesRoutes.js
//Descripcion: Rutas del servidor REST para /mediciones: valida el POST y devuelve las
//              ultimas 10 mediciones en el GET.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

/**
 * Servidor REST - Módulo de rutas para mediciones.
 * 
 * Diseño según reglas del PDF "Logical_Design_RE_Rules_v3.pdf" 
 * Especificacion: "Logical_Design_RE_Rules_v3.pdf" (adjunta al enunciado)
 * 
 * Tipos de datos abstractos utilizados: N, Z, R, Text, [Tipo]
 */

const url = require('url');
const { LogicaNegocioMediciones } = require('../logica/medicionesLogica');

const HTTP_OK = 200;
const HTTP_CREATED = 201;
const HTTP_BAD_REQUEST = 400;
const HTTP_INTERNAL_SERVER_ERROR = 500;

/**
 * POST/mediciones
 * 
 * Diseño lógico (siguiendo reglas RE):
 * - Recibe en el cuerpo JSON:
 *   medicion = (tipo: Text, valor: R, nombre_emisora: Text?)
 * - Los datos del beacon (uuid, txPower, major, minor, rssi) no se envian en
 *   el POST: se quedan en la app Android (la uuid solo se usa ahi para buscar
 *   el iBeacon).
 * - Valida entrada
 * - Llama a Lógica de Negocio: guardarMedicion(medicion) 
 * - Responde con códigos HTTP 200/201/400/500
 */
function handlePostMediciones(req, res) {
    let body = '';

    req.on('data', chunk => {
        body += chunk.toString();
        // Evitar cuerpos excesivamente grandes (defensa básica)
        if (body.length > 1e6) {
            req.connection.destroy();
        }
    });

    req.on('end', () => {
        try {
            const parsed = body ? JSON.parse(body) : {};

            // Campos del contrato POST
            const tipo = parsed.tipo;
            const valor = parsed.valor;
            const nombreEmisora = parsed.nombre_emisora;

            // Validación básica (tipos)
            if (tipo === undefined || valor === undefined) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ 
                    error: 'Campos requeridos: tipo(Text), valor(R)' 
                }));
                return;
            }

            // Validaciones de tipo
            if (typeof tipo !== 'string') {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'tipo debe ser Text' }));
                return;
            }
            if (typeof valor !== 'number' || !Number.isFinite(valor)) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'valor debe ser R (número real)' }));
                return;
            }
            if (nombreEmisora !== undefined && typeof nombreEmisora !== 'string') {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'nombre_emisora debe ser Text' }));
                return;
            }

            // Preparar la medicion para la Lógica de Negocio (única
            // responsabilidad del servidor REST: validar y delegar)
            const medicion = { tipo: tipo, valor: valor };
            if (nombreEmisora !== undefined) {
                medicion.nombre_emisora = nombreEmisora;
            }

            // Llamada a la Lógica de Negocio (única responsabilidad del servidor REST)
            LogicaNegocioMediciones.guardarMedicion(medicion)
                .then(resultado => {
                    res.writeHead(HTTP_CREATED, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ status: 'ok', resultado }));
                })
                .catch(error => {
                    res.writeHead(HTTP_INTERNAL_SERVER_ERROR, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Error interno del servidor' }));
                });

        } catch (error) {
            res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ error: 'JSON inválido' }));
        }
    });
}

/**
 * GET/mediciones
 * 
 * Diseño lógico (siguiendo reglas RE):
 * - Invoca a Lógica de Negocio: obtenerMedicion()
 * - Responde con lista JSON de las últimas 10 mediciones
 * - Campos requeridos: fecha_y_hora, tipo, valor
 */
function handleGetMediciones(req, res) {
    try {
        LogicaNegocioMediciones.obtenerMedicion()
            .then(mediciones => {
                const ultimasDiez = Array.isArray(mediciones) ? mediciones.slice(-10) : [];
                const respuesta = ultimasDiez.map(m => ({
                    fecha_y_hora: m.fecha_y_hora || m.fechaYHora || m.fecha || new Date().toISOString(),
                    tipo: m.tipo || (m.medicion && m.medicion.tipo) || '',
                    valor: m.valor || (m.medicion && m.medicion.valor) || 0
                }));
                res.writeHead(HTTP_OK, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify(respuesta));
            })
            .catch(error => {
                res.writeHead(HTTP_INTERNAL_SERVER_ERROR, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'Error interno del servidor' }));
            });

    } catch (error) {
        res.writeHead(HTTP_INTERNAL_SERVER_ERROR, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Error interno del servidor' }));
    }
}

/**
 * Router para rutas de mediciones
 */
function routerMediciones(req, res) {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    if (req.method === 'POST' && (pathname === '/mediciones' || pathname === '/mediciones/')) {
        handlePostMediciones(req, res);
        return true;
    }

    if (req.method === 'GET' && (pathname === '/mediciones' || pathname === '/mediciones/')) {
        handleGetMediciones(req, res);
        return true;
    }

    return false;
}

module.exports = {
    routerMediciones,
    handlePostMediciones,
    handleGetMediciones,
    HTTP_OK,
    HTTP_CREATED,
    HTTP_BAD_REQUEST,
    HTTP_INTERNAL_SERVER_ERROR
};
