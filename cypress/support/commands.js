// Inicia sesión a través de la UI real (formulario de ingresar.html) y
// espera la redirección al dashboard.
Cypress.Commands.add('login', (email, password) => {
    cy.visit('/html/ingresar.html');
    cy.get('#Iemail').type(email);
    cy.get('#Ipassword').type(password);
    cy.get('#ingreso button[type="submit"]').click();
    cy.location('pathname', { timeout: 10000 }).should('include', 'tableroDeControl.html');
});

// Registra un usuario directamente contra la API (más rápido que llenar el
// formulario de registro), útil para preparar datos de prueba.
Cypress.Commands.add('registrarUsuarioApi', (usuario) => {
    return cy.request('POST', '/api/usuarios/registrar', usuario);
});
