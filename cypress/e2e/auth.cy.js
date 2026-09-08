// Los tres tests de este archivo dependen del orden de ejecución dentro
// del describe: el primero crea el usuario que usan los siguientes dos.
describe('Autenticación', () => {
    const timestamp = Date.now();
    const nuevoUsuario = {
        dni: String(20000000 + Math.floor(Math.random() * 1000000)),
        email: `qa.${timestamp}@alkywallet.test`,
        password: 'ClaveSegura123'
    };

    it('permite registrar un usuario nuevo y redirige al login', () => {
        cy.visit('/html/registro.html');
        cy.get('#Rdni').type(nuevoUsuario.dni);
        cy.get('#Remail').type(nuevoUsuario.email);
        cy.get('#Rpassword').type(nuevoUsuario.password);
        cy.get('#r-password').type(nuevoUsuario.password);
        cy.get('#registro button[type="submit"]').click();

        cy.contains('exitoso', { timeout: 10000 }).should('be.visible');
        cy.location('pathname', { timeout: 10000 }).should('include', 'ingresar.html');
    });

    it('permite iniciar sesión con el usuario recién creado', () => {
        cy.login(nuevoUsuario.email, nuevoUsuario.password);
        cy.get('.user-email').first().should('contain.text', nuevoUsuario.email);
    });

    it('muestra un error con credenciales inválidas', () => {
        cy.visit('/html/ingresar.html');
        cy.get('#Iemail').type(nuevoUsuario.email);
        cy.get('#Ipassword').type('claveIncorrecta123');
        cy.get('#ingreso button[type="submit"]').click();
        cy.get('#mensajeError', { timeout: 10000 }).should('be.visible').and('contain.text', 'Credenciales inválidas');
    });
});
