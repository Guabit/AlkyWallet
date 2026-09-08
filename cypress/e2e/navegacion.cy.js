describe('Navegación general', () => {
    it('muestra la landing en la raíz del sitio', () => {
        cy.visit('/');
        cy.contains('ALKYWALLET').should('be.visible');
        cy.contains('Crear cuenta').should('be.visible');
    });

    it('deniega el acceso (403) ante una ruta protegida inexistente', () => {
        cy.request({ url: '/esto-no-existe-1234', failOnStatusCode: false }).then((resp) => {
            expect(resp.status).to.eq(403);
            expect(resp.status).to.eq(403);
        });
    });
});
