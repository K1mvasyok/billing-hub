--liquibase formatted sql

--changeset billing:011-billing-run-running-unique
-- Не больше одного идущего запуска на период. Запуски, оставшиеся RUNNING после падения сервиса,
-- помечаем FAILED: так делает failStaleRuns при старте, но миграция выполняется раньше старта,
-- и без этого шага индекс упал бы на старых дубликатах.
UPDATE billing_run SET status = 'FAILED', finished_at = now(), updated_at = now() WHERE status = 'RUNNING';

CREATE UNIQUE INDEX ux_billing_run_running_period ON billing_run (period) WHERE status = 'RUNNING';
