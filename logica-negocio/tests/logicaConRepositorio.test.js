const { test, describe, beforeEach } = require('node:test');
const assert = require('node:assert');
const { LogicaNegocio } = require('../src/logicaNegocioConRepositorio');
const repo = require('../src/repositorioMemoria');

describe('Lógica de Negocio con Repositorio', () => {
    beforeEach(() => {
        repo.limpiar();
    });

    test('guardarMedicion envía datos a repositorio y retorna ok', () => {
        const logica = new LogicaNegocio(repo);
        const res = logica.guardarMedicion({ tipo: 'distancia', valor: 2.5 }, 'uuid', 1, 2, -59);
        assert.strictEqual(res.ok, true);
    });

    test('obtenerMedicion devuelve últimas 10 con campos correctos', () => {
        const logica = new LogicaNegocio(repo);
        logica.guardarMedicion({ tipo: 'temperatura', valor: 23.5 }, 'u1', 1, 2, -70);
        logica.guardarMedicion({ tipo: 'RSSI', valor: -72.5 }, 'u2', 4, 2, 7);
        
        const lista = logica.obtenerMedicion();
        assert.strictEqual(lista.length, 2);
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'fecha_y_hora'));
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'tipo'));
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'valor'));
    });

    test('guardarMedicion procesa beacon y medición', () => {
        const logica = new LogicaNegocio(repo);
        logica.guardarMedicion({ tipo: 'distancia', valor: 5 }, 'F7826DA6', 812, 11, -59);
        const lista = logica.obtenerMedicion();
        assert.strictEqual(lista[0].tipo, 'distancia');
        assert.strictEqual(lista[0].valor, 5);
    });
});
