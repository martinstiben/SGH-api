-- Script para corregir la longitud de la columna account_status
-- Ejecutar este script en tu base de datos MySQL antes de usar el sistema

USE sgh_db; -- Cambia por el nombre de tu base de datos

-- Alterar la columna account_status para que tenga suficiente longitud
ALTER TABLE users MODIFY COLUMN account_status VARCHAR(15) NOT NULL;

-- Verificar que el cambio se aplicó correctamente
DESCRIBE users;

-- Opcional: Ver los valores actuales en la tabla
SELECT user_id, account_status FROM users;