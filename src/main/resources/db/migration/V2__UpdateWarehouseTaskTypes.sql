-- Миграция V2: Изменение проверки складских операций

ALTER TABLE warehouse_tasks
    DROP CONSTRAINT warehouse_tasks_type_check;


ALTER TABLE warehouse_tasks
    ADD CONSTRAINT warehouse_tasks_type_check
    CHECK (type IN ('RECEIVING', 'MOVEMENT', 'PICKING'));