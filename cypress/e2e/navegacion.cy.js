describe('Navegación general', () => {
    it('muestra la landing en la raíz del sitio', () => {
        cy.visit('/');
        cy.contains('ALKYWALLET').should('be.visible');
        cy.contains('Crear cuenta').should('be.visible');
    });

    it('responde 404 con la página propia ante una ruta inexistente', () => {
        cy.request({ url: '/esto-no-existe-1234', failOnStatusCode: false }).then((resp) => {
            expect(resp.status).to.eq(404);
            expect(resp.body).to.include('404');
        });
    });
});
