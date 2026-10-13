//==============================================================================================
//Nombre: mediciones.test.js
//Descripcion: Pruebas del servidor REST: levanta el servidor y comprueba el POST y el GET
//              de /mediciones.
//Copyright:
//Fecha de creacion: 2026-10-02
//Fecha de modificacion: 2026-10-04
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: limpieza y mas comentarios
//==============================================================================================

const { test, describe, before, after } = require('node:test');
const assert = require('node:assert');
const http = require('http');
const { server } = require('../src/app');

let port;

// makeRequest(): envia una peticion HTTP al servidor de pruebas y devuelve
// codigo, cabeceras y cuerpo.
function makeRequest(method, path, body) {
    return new Promise((resolve, reject) => {
        const req = http.request({
            method,
            hostname: '127.0.0.1',
            port: port,
            path: path
        }, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                resolve({
                    statusCode: res.statusCode,
                    headers: res.headers,
                    body: data
                });
            });
        });
        req.on('error', reject);
        if (body) {
            req.write(body);
        }
        req.end();
    });
}

describe('Servidor REST - /mediciones', () => {
    before(() => {
        return new Promise((resolve) => {
            server.listen(0, () => {
                port = server.address().port;
                resolve();
            });
        });
    });

    after(() => {
        return new Promise((resolve) => {
            server.close(() => resolve());
        });
    });

    test('POST /mediciones - válida', async () => {
        // solo tipo y valor: no hace falta ningun dato del beacon
        const body = JSON.stringify({
            tipo: 'distancia',
            valor: 2.5
        });
        const res = await makeRequest('POST', '/mediciones', body);
        assert.strictEqual(res.statusCode, 201);
    });

    test('POST /mediciones - valida y pasa a lógica de negocio (con todos los campos)', async () => {
        // los tres campos del contrato: tipo, valor y nombre_emisora
        const body = JSON.stringify({
            tipo: 'temperatura',
            valor: 23.5,
            nombre_emisora: 'GTI-3A-MJ'
        });
        const res = await makeRequest('POST', '/mediciones', body);
        assert.strictEqual(res.statusCode, 201);
        const parsed = JSON.parse(res.body);
        assert.ok(parsed.status === 'ok');
    });

    test('POST /mediciones - campos faltantes retorna 400', async () => {
        const body = JSON.stringify({
            tipo: 'ozono'
        });
        const res = await makeRequest('POST', '/mediciones', body);
        assert.strictEqual(res.statusCode, 400);
    });

    test('POST /mediciones - JSON inválido retorna 400', async () => {
        const res = await makeRequest('POST', '/mediciones', '{invalid json');
        assert.strictEqual(res.statusCode, 400);
    });

    test('GET /mediciones - retorna lista JSON con últimas 10 mediciones', async () => {
        const res = await makeRequest('GET', '/mediciones');
        assert.strictEqual(res.statusCode, 200);
        const parsed = JSON.parse(res.body);
        assert.ok(Array.isArray(parsed));
    });

    test('GET /mediciones - campos deben ser fecha_y_hora, tipo, valor', async () => {
        const res = await makeRequest('GET', '/mediciones');
        assert.strictEqual(res.statusCode, 200);
        const parsed = JSON.parse(res.body);
        if (parsed.length > 0) {
            // exactamente esos tres campos: nada de datos del beacon
            assert.deepStrictEqual(
                Object.keys(parsed[0]).sort(),
                ['fecha_y_hora', 'tipo', 'valor']
            );
        }
    });
});
