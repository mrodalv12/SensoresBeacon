//==============================================================================================
//Nombre: logicaSqlitePhp.test.js
//Descripcion: Prueba de integracion de la logica de negocio contra la base de datos SQLite
//              via el puente PHP (PDO + sqlite3): guarda una fila real y la recupera.
//Copyright:
//Fecha de creacion: 2026-10-13
//Fecha de modificacion: 2026-10-13
//Autores: Jordi Bataller y Maria Jose Rodriguez
//Autor: Maria Jose Rodriguez
//Aportacion: prueba de integracion con la base de datos
//==============================================================================================

const { test, describe } = require('node:test');
const assert = require('node:assert');
const { LogicaNegocioSQLitePhp } = require('../src/logicaNegocioSqlitePhp');
const repo = require('../src/repositorioSqlitePhp');

describe('Lógica de Negocio - Integración con BD (PHP)', () => {
    test('guardarMedicion y obtenerMedicion funcionan con BD', async () => {
        const logica = new LogicaNegocioSQLitePhp(repo);
        const res = await logica.guardarMedicion(
            { tipo: 'distancia', valor: 2.5, nombre_emisora: 'TEST-PHP-123' }
        );
        assert.strictEqual(res.ok, true);
        assert.strictEqual(res.nombre_cliente, 'TEST-PHP-123');
        assert.ok(res.fecha_y_hora);

        const lista = await logica.obtenerMedicion();
        assert.ok(Array.isArray(lista));
        assert.ok(lista.length <= 10);
        
        const ultima = lista[lista.length - 1];
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'fecha_y_hora'));
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'tipo'));
        assert.ok(Object.prototype.hasOwnProperty.call(ultima, 'valor'));
    });
});
