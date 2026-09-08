describe('Depósitos y Transferencias', () => {
    const timestamp = Date.now();
    const usuarioPrincipal = {
        dni: String(30000000 + Math.floor(Math.random() * 1000000)),
        email: `principal.${timestamp}@alkywallet.test`,
        password: 'ClaveSegura123'
    };
    const usuarioDestino = {
        dni: String(40000000 + Math.floor(Math.random() * 1000000)),
        email: `destino.${timestamp}@alkywallet.test`,
        password: 'ClaveSegura123'
    };

    before(() => {
        cy.registrarUsuarioApi(usuarioPrincipal);
        cy.registrarUsuarioApi(usuarioDestino);
    });

    beforeEach(() => {
        cy.login(usuarioPrincipal.email, usuarioPrincipal.password);
    });

    it('permite depositar dinero y muestra el mensaje de éxito', () => {
        cy.visit('/html/deposito.html');
        cy.get('#monto').type('5000');
        cy.get('#btn-depositar').click();
        cy.contains('procesado con éxito', { timeout: 10000 }).should('be.visible');
    });

    it('muestra un error si se intenta transferir sin destinatario', () => {
        cy.visit('/html/tranferencia.html');
        cy.get('#monto').type('100');
        cy.get('#btn-transferir').click();
        cy.get('#mensaje-notificacion').should('be.visible').and('contain.text', 'destinatario');
    });

    it('permite transferir dinero categorizado a otro usuario', () => {
        cy.visit('/html/tranferencia.html');
        cy.get('#cuenta-destino').type(usuarioDestino.email);
        cy.get('#monto').type('500');
        cy.get('#categoria').select('COMIDA');
        cy.get('#btn-transferir').click();
        cy.contains('realizada con éxito', { timeout: 10000 }).should('be.visible');
    });

    it('la transferencia aparece categorizada en el historial', () => {
        cy.visit('/html/historial.html');
        cy.get('#historial-tabla-wrapper', { timeout: 10000 }).should('be.visible');
        cy.get('#historial-tbody tr').first().should('contain.text', 'Comida');
    });
});
