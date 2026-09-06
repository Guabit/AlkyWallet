-- Inserción de Usuario ADMIN inicial para PostgreSQL
-- Password en texto plano: Password123
-- Hash BCrypt generado: $2a$10$T.ZjDLGpH0qT8Buy0oDTseGVHr6CnCUCYvunC6ZREY3tJFWjmnLKG
INSERT INTO usuarios (nombre, apellido, dni, email, password, rol, is_deleted, created_at)
VALUES (
    'Admin',
    'Sistema',
    '00000000',
    'admin@alkywallet.com',
    '$2a$10$T.ZjDLGpH0qT8Buy0oDTseGVHr6CnCUCYvunC6ZREY3tJFWjmnLKG',
    'ADMIN',
    false,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO NOTHING;

-- Cuenta inicial para el Admin
INSERT INTO cuentas (saldo, tipo_moneda, is_deleted, created_at, updated_at, usuario_id)
SELECT 0.00, 'ARS', false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, u.id
FROM usuarios u
WHERE u.email = 'admin@alkywallet.com'
  AND NOT EXISTS (
    SELECT 1 FROM cuentas c WHERE c.usuario_id = u.id AND c.tipo_moneda = 'ARS'
);