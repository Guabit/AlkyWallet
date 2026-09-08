-- Archivo de datos iniciales en desarrollo (se ejecuta si spring.sql.init.mode=always)

-- ¡NOTA IMPORTANTE!
-- El usuario ADMIN inicial ahora es gestionado por DatabaseSeeder.java 
-- para evitar hardcodear credenciales y asegurar que el hash se genere con el PasswordEncoder activo.
-- Configura las credenciales con ADMIN_EMAIL y ADMIN_PASSWORD en las variables de entorno.

-- Puedes colocar aquí otros datos de prueba si lo deseas (Mock de categorías, parámetros globales, etc.)

-- Dummy statement para que Spring Boot no falle al ejecutar un script vacío.
SELECT 1;
