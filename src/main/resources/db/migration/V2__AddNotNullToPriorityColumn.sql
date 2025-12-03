--- Миграция V2: Добавляем ограничение NOT NULL к колонке priority для согласованности с JPA Entity

ALTER TABLE products
ALTER COLUMN priority SET NOT NULL;

-- Добавляем значение по умолчанию для согласованности с default значением в Entity
ALTER TABLE products
ALTER COLUMN priority SET DEFAULT 'NORMAL';