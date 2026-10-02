const { test, describe } = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

describe('Página Web - Lógica', () => {
    test('Archivo logicaWeb.php existe', () => {
        const p = path.join(__dirname, '../php/logicaWeb.php');
        assert.ok(fs.existsSync(p));
    });
    
    test('Archivo index.php existe', () => {
        const p = path.join(__dirname, '../index.php');
        assert.ok(fs.existsSync(p));
    });
    
    test('Archivo css/style.css existe con colores correctos', () => {
        const p = path.join(__dirname, '../css/style.css');
        assert.ok(fs.existsSync(p));
        const content = fs.readFileSync(p);
        assert.ok(content.includes('#0F2A2E'));
        assert.ok(content.includes('#7FE3C0'));
    });
    
    test('Archivo js/app.js existe', () => {
        const p = path.join(__dirname, '../js/app.js');
        assert.ok(fs.existsSync(p));
    });
    
    test('API mediciones.php existe', () => {
        const p = path.join(__dirname, '../api/mediciones.php');
        assert.ok(fs.existsSync(p));
    });
});
