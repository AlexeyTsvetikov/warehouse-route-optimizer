-- Миграция V1: Создание таблиц для системы

-- 1. Категории товаров
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_categories_active ON categories(is_active);

-- 2. Товары
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    weight DOUBLE PRECISION,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    volume DOUBLE PRECISION GENERATED ALWAYS AS (width * height * depth) STORED,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    category_id BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_category ON products(category_id);

-- 3. Локации
CREATE TABLE IF NOT EXISTS locations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL
        CHECK (type IN ('RECEIVING', 'BULK', 'PICKING', 'DISPATCH')),
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    volume DOUBLE PRECISION GENERATED ALWAYS AS (width * height * depth) STORED,
    max_weight DOUBLE PRECISION,
    x_coord DOUBLE PRECISION NOT NULL,
    y_coord DOUBLE PRECISION NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_locations_code ON locations(code);
CREATE INDEX idx_locations_type ON locations(type);
CREATE INDEX idx_locations_coords ON locations(x_coord, y_coord);

-- 4. Заказы
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    customer_name VARCHAR(100),
    destination_region VARCHAR(100) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 2,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW'
        CHECK (status IN ('NEW', 'PROCESSING', 'COMPLETED', 'CANCELLED')),
    type VARCHAR(20) NOT NULL DEFAULT 'OUTBOUND'
        CHECK (type IN ('INBOUND', 'OUTBOUND')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    planned_departure TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_type ON orders(type);
CREATE INDEX idx_orders_departure ON orders(planned_departure);

-- 5. Позиции заказа
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    collected_quantity INTEGER NOT NULL DEFAULT 0 CHECK (collected_quantity >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_product ON order_items(product_id);

-- 6. Остатки товаров
CREATE TABLE IF NOT EXISTS stocks (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    inbound_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_reserved_not_exceed CHECK (reserved_quantity <= quantity),
    CONSTRAINT uc_stock_product_location UNIQUE (product_id, location_id)
);

CREATE INDEX idx_stock_product ON stocks(product_id);
CREATE INDEX idx_stock_location ON stocks(location_id);

-- 7. Пользователи
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'PICKER'
        CHECK (role IN ('ADMIN', 'MANAGER', 'OPERATOR', 'DRIVER', 'PICKER')),
    last_known_x DOUBLE PRECISION,
    last_known_y DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);

-- 8. Задания на складе
CREATE TABLE IF NOT EXISTS warehouse_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_number VARCHAR(50) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL
        CHECK (type IN ('PLACEMENT', 'PICKING', 'TRANSFER')),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    planned_quantity INTEGER NOT NULL CHECK (planned_quantity > 0),
    confirmed_quantity INTEGER CHECK (confirmed_quantity >= 0),
    source_location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    target_location_id BIGINT NOT NULL REFERENCES locations(id) ON DELETE RESTRICT,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT fk_task_locations CHECK (source_location_id != target_location_id)
);

CREATE INDEX idx_task_status ON warehouse_tasks(status);
CREATE INDEX idx_task_user ON warehouse_tasks(user_id);
CREATE INDEX idx_task_product ON warehouse_tasks(product_id);
CREATE INDEX idx_task_created_at ON warehouse_tasks(created_at DESC);

-- Комментарии к таблицам
COMMENT ON TABLE categories IS 'Категории товаров для классификации';
COMMENT ON TABLE products IS 'Товары/продукты с физическими характеристиками';
COMMENT ON TABLE locations IS 'Ячейки/локации склада с координатами';
COMMENT ON TABLE orders IS 'Заказы клиентов (OUTBOUND) или поставки (INBOUND)';
COMMENT ON TABLE order_items IS 'Позиции в заказах';
COMMENT ON TABLE stocks IS 'Остатки товаров в ячейках склада';
COMMENT ON TABLE users IS 'Пользователи системы (операторы, менеджеры)';
COMMENT ON TABLE warehouse_tasks IS 'Задания для операторов на складе';