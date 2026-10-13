//==============================================================================================
//Nombre: logicaConRepositorio.test.js
//Descripcion: Pruebas de la logica de negocio con un repositorio en memoria inyectado:
//              guarda, lee las ultimas y verifica que no procesa datos del beacon.
//Copyright:
//Fecha de creacion: 2026-10-07
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: pruebas de la logica con repositorio
//==============================================================================================

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
        const res = logica.guardarMedicion({ tipo: 'distancia', valor: 2.5 });
        assert.strictEqual(res.ok, true);
    });

    test('obtenerMedicion devuelve últimas 10 con campos correctos', () => {
        const logica = new LogicaNegocio(repo);
        logica.guardarMedicion({ tipo: 'temperatura', valor: 23.5 });
        logica.guardarMedicion({ tipo: 'RSSI', valor: -72.5 });
        
        const lista = logica.obtenerMedicion();
        assert.strictEqual(lista.length, 2);
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'fecha_y_hora'));
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'tipo'));
        assert.ok(Object.prototype.hasOwnProperty.call(lista[0], 'valor'));
    });

    test('guardarMedicion procesa la medicion sin datos del beacon', () => {
        const logica = new LogicaNegocio(repo);
        logica.guardarMedicion({ tipo: 'distancia', valor: 5 });
        const lista = logica.obtenerMedicion();
        assert.strictEqual(lista[0].tipo, 'distancia');
        assert.strictEqual(lista[0].valor, 5);
        assert.strictEqual(lista[0].uuid, undefined);
    });
});
