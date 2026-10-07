const { test, describe } = require('node:test');
const assert = require('node:assert');
const { LogicaNegocioSQLitePy } = require('../src/logicaNegocioSqlitePy');
const repo = require('../src/repositorioSqlitePython');

describe('Lógica de Negocio - Integración con BD (Python)', () => {
    test('guardarMedicion y obtenerMedicion funcionan con BD', async () => {
        const logica = new LogicaNegocioSQLitePy(repo);
        const res = await logica.guardarMedicion(
            { tipo: 'distancia', valor: 2.5 },
            'TEST-PY-123',
            812,
            11,
            -59
        );
        assert.strictEqual(res.ok, true);
        assert.ok(res.id);

        const lista = await logica.obtenerMedicion();
        assert.ok(Array.isArray(lista));
        assert.ok(lista.length <= 10);
        
        const ultima = lista[lista.length - 1];
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'fecha_y_hora'));
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'tipo'));
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'valor'));
    });
});
