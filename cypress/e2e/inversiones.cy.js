describe('Inversiones (simuladas)', () => {
    const timestamp = Date.now();
    const usuario = {
        dni: String(50000000 + Math.floor(Math.random() * 1000000)),
        email: `inversor.${timestamp}@alkywallet.test`,
        password: 'ClaveSegura123'
    };

    before(() => {
        cy.registrarUsuarioApi(usuario);
    });

    beforeEach(() => {
        cy.login(usuario.email, usuario.password);
    });

    it('deposita fondos y luego invierte una parte del saldo', () => {
        cy.visit('/html/deposito.html');
        cy.get('#monto').type('2000');
        cy.get('#btn-depositar').click();
        cy.contains('Depósito procesado con éxito', { timeout: 10000 });

        cy.visit('/html/inversiones.html');
        cy.get('#monto-invertir').type('1000');
        cy.get('#btn-invertir').click();
        cy.contains('¡Inversión realizada con éxito!', { timeout: 10000 }).should('be.visible');
        cy.get('#lista-inversiones').should('contain.text', 'Activa');
    });
});
