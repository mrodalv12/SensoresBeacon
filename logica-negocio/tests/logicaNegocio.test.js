const { test, describe } = require('node:test');
const assert = require('node:assert');
const { guardarMedicion, obtenerMedicion } = require('../src/logicaNegocio');

describe('Lógica de Negocio - guardarMedicion', () => {
    test('guardarMedicion procesa datos correctamente', () => {
        const medicion = { tipo: 'distancia', valor: 2.5 };
        const resultado = guardarMedicion(medicion, 'F7826DA6', 812, 11, -59);
        
        assert.strictEqual(resultado.ok, true);
        assert.ok(resultado.procesados);
        assert.strictEqual(resultado.procesados.tipo, 'distancia');
        assert.strictEqual(resultado.procesados.valor, 2.5);
        assert.strictEqual(resultado.procesados.uuid, 'F7826DA6');
        assert.strictEqual(resultado.procesados.minor, 812);
        assert.strictEqual(resultado.procesados.major, 11);
        assert.strictEqual(resultado.procesados.txPower, -59);
    });

    test('guardarMedicion acepta objeto medicion con tipo y valor', () => {
        const medicion = { tipo: 'temperatura', valor: 23.5, nombreEmisora: 'GTI-3A-MJ' };
        const resultado = guardarMedicion(medicion, 'ABC123', 68, 13, -70);
        
        assert.strictEqual(resultado.ok, true);
        assert.strictEqual(resultado.procesados.tipo, 'temperatura');
        assert.strictEqual(resultado.procesados.valor, 23.5);
        assert.strictEqual(resultado.procesados.nombreEmisora, 'GTI-3A-MJ');
    });

    test('guardarMedicion maneja valores numéricos correctamente', () => {
        const medicion = { tipo: 'RSSI', valor: -72.5 };
        const resultado = guardarMedicion(medicion, 'uuid-test', 4, 2, 7);
        
        assert.strictEqual(resultado.procesados.valor, -72.5);
        assert.strictEqual(typeof resultado.procesados.valor, 'number');
    });
});

describe('Lógica de Negocio - obtenerMedicion', () => {
    test('obtenerMedicion devuelve array', () => {
        const resultado = obtenerMedicion();
        assert.ok(Array.isArray(resultado));
    });

    test('obtenerMedicion devuelve máximo 10 mediciones', () => {
        const resultado = obtenerMedicion();
        assert.ok(resultado.length <= 10);
    });

    test('obtenerMedicion retorna elementos con campos requeridos', () => {
        // Simulamos datos para verificar estructura
        const resultado = obtenerMedicion();
        if (resultado.length > 0) {
            const primer = resultado[0];
            assert.ok(Object.prototype.hasOwnProperty.call(primer, 'fecha_y_hora'));
            assert.ok(Object.prototype.hasOwnProperty.call(primer, 'tipo'));
            assert.ok(Object.prototype.hasOwnProperty.call(primer, 'valor'));
        }
    });
});
