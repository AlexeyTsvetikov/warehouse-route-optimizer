ALTER TABLE warehouse_tasks
    ALTER COLUMN source_location_id DROP NOT NULL;

ALTER TABLE warehouse_tasks
    ALTER COLUMN target_location_id DROP NOT NULL;

ALTER TABLE warehouse_tasks
    DROP CONSTRAINT IF EXISTS fk_task_locations;