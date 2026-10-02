const { test, describe, before, after } = require('node:test');
const assert = require('node:assert');
const http = require('http');
const { server } = require('../src/app');

let port;

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
        const body = JSON.stringify({
            uuid: 'F7826DA64FA24E988024BC5B71E0893E',
            minor: 812,
            major: 11,
            txPower: -59,
            tipo: 'distancia',
            valor: 2.5
        });
        const res = await makeRequest('POST', '/mediciones', body);
        assert.strictEqual(res.statusCode, 201);
    });

    test('POST /mediciones - valida y pasa a lógica de negocio (con todos los campos)', async () => {
        const body = JSON.stringify({
            uuid: 'F7826DA64FA24E988024BC5B71E0893E',
            minor: 68,
            major: 13,
            txPower: -70,
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
            uuid: 'ABC123',
            minor: 1
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
            assert.ok(Object.prototype.hasOwnProperty.call(parsed[0], 'fecha_y_hora'));
            assert.ok(Object.prototype.hasOwnProperty.call(parsed[0], 'tipo'));
            assert.ok(Object.prototype.hasOwnProperty.call(parsed[0], 'valor'));
        }
    });
});
