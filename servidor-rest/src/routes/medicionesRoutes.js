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
 *   beacon = (uuid: Text, minor: N, major: N, txPower: Z, nombre_emisora: Text?)
 *   medicion = (tipo: Text, valor: R)
 * - Valida entrada
 * - Llama a Lógica de Negocio: guardarMedicion(datos) 
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

            // Validación de campos requeridos según especificación
            const uuid = parsed.uuid;
            const minor = parsed.minor;
            const major = parsed.major;
            const txPower = parsed.txPower;
            const tipo = parsed.tipo;
            const valor = parsed.valor;
            const nombreEmisora = parsed.nombre_emisora;

            // Validación básica (tipos)
            if (uuid === undefined || minor === undefined || major === undefined || 
                txPower === undefined || tipo === undefined || valor === undefined) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ 
                    error: 'Campos requeridos: uuid(Text), minor(N), major(N), txPower(Z), tipo(Text), valor(R)' 
                }));
                return;
            }

            // Validaciones de tipo
            if (typeof uuid !== 'string') {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'uuid debe ser Text' }));
                return;
            }
            if (typeof tipo !== 'string') {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'tipo debe ser Text' }));
                return;
            }
            // Números (N, Z, R)
            if (typeof minor !== 'number' || !Number.isFinite(minor)) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'minor debe ser N (número)' }));
                return;
            }
            if (typeof major !== 'number' || !Number.isFinite(major)) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'major debe ser N (número)' }));
                return;
            }
            if (typeof txPower !== 'number' || !Number.isFinite(txPower)) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'txPower debe ser Z (número entero)' }));
                return;
            }
            if (typeof valor !== 'number' || !Number.isFinite(valor)) {
                res.writeHead(HTTP_BAD_REQUEST, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: 'valor debe ser R (número real)' }));
                return;
            }

            // Preparar datos para Lógica de Negocio
            const datosMedicion = {
                beacon: {
                    uuid: uuid,
                    minor: minor,
                    major: major,
                    txPower: txPower,
                    ...(nombreEmisora !== undefined ? { nombre_emisora: nombreEmisora } : {})
                },
                medicion: {
                    tipo: tipo,
                    valor: valor
                }
            };

            // Llamada a la Lógica de Negocio (única responsabilidad del servidor REST)
            LogicaNegocioMediciones.guardarMedicion(datosMedicion)
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
