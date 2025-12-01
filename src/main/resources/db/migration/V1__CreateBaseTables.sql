-- Миграция V1: Создание таблиц для системы

CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    tracking_number VARCHAR(50) UNIQUE NOT NULL,
    destination_region VARCHAR(100) NOT NULL,
    width DOUBLE PRECISION,
    height DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    volume DOUBLE PRECISION GENERATED ALWAYS AS (width * height * depth) STORED,
    weight DOUBLE PRECISION,
    priority VARCHAR(10) CHECK (priority IN ('URGENT', 'NORMAL', 'LOW')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE storage_racks (
    id BIGSERIAL PRIMARY KEY,
    zone VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('EXPRESS', 'STANDARD', 'BUFFER')),
    description TEXT
);

CREATE TABLE rack_cells (
    id BIGSERIAL PRIMARY KEY,
    storage_rack_id BIGINT REFERENCES storage_racks(id) ON DELETE CASCADE,
    cell_code VARCHAR(20) UNIQUE NOT NULL,
    cell_type VARCHAR(20) NOT NULL CHECK (cell_type IN ('STORAGE', 'RECEIVING', 'SHIPPING', 'BUFFER')),
    coord_x DOUBLE PRECISION NOT NULL,
    coord_y DOUBLE PRECISION NOT NULL,
    max_volume DOUBLE PRECISION NOT NULL,
    current_volume DOUBLE PRECISION DEFAULT 0,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    destination_region VARCHAR(100) NOT NULL,
    status VARCHAR(20) CHECK (status IN ('FORMING', 'READY', 'SHIPPED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    planned_departure TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE order_details (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER DEFAULT 1 CHECK (quantity > 0),
    UNIQUE(order_id, product_id)
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) CHECK (role IN ('PICKER', 'PLACER', 'MANAGER', 'ADMIN')),
    last_known_x DOUBLE PRECISION,
    last_known_y DOUBLE PRECISION,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tasks (
    id BIGSERIAL PRIMARY KEY,
    task_number VARCHAR(50) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('PLACEMENT', 'PICKING', 'TRANSFER')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    product_id BIGINT REFERENCES products(id) ON DELETE CASCADE NOT NULL,
    source_cell_id BIGINT REFERENCES rack_cells(id) ON DELETE CASCADE NOT NULL,
    target_cell_id BIGINT REFERENCES rack_cells(id) ON DELETE CASCADE NOT NULL,
    order_id BIGINT REFERENCES orders(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    assigned_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT different_cells CHECK (source_cell_id != target_cell_id)
);

CREATE INDEX idx_products_tracking ON products(tracking_number);
CREATE INDEX idx_products_priority ON products(priority, created_at);
CREATE INDEX idx_rack_cells_coords ON rack_cells(coord_x, coord_y);
CREATE INDEX idx_rack_cells_type ON rack_cells(cell_type);
CREATE INDEX idx_orders_status ON orders(status, planned_departure);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_user ON tasks(user_id, status);
CREATE INDEX idx_tasks_type_status ON tasks(type, status);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_order_details_composite ON order_details(order_id, product_id);
CREATE INDEX idx_tasks_cells ON tasks(source_cell_id, target_cell_id);
