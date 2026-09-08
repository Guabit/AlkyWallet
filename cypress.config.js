const { defineConfig } = require('cypress');

module.exports = defineConfig({
  e2e: {
    // La app sirve frontend y backend desde el mismo origen (Spring Boot en :8080)
    baseUrl: 'http://localhost:8080',
    specPattern: 'cypress/e2e/**/*.cy.js',
    supportFile: 'cypress/support/e2e.js',
    video: false,
  },
});
