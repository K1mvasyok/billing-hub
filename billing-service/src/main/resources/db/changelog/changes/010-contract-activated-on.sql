--liquibase formatted sql

--changeset billing:010-contract-activated-on
-- Дата активации договора. Черновик, расторгнутый до активации, остаётся без даты и счетов не получает.
-- Колонка есть и в contract_aud: Envers пишет поля сущности в аудит, без неё Hibernate validate упадёт на старте.
ALTER TABLE contract ADD COLUMN activated_on date;
ALTER TABLE contract_aud ADD COLUMN activated_on date;

-- Действующие договоры до этой миграции были активированы. Дату точно не восстановить, берём начало срока.
-- Расторгнутые договоры не трогаем: по статусу TERMINATED нельзя понять, были ли они активны.
UPDATE contract SET activated_on = valid_from WHERE status = 'ACTIVE';
