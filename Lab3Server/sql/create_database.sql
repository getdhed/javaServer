-- Выполнить под администратором PostgreSQL (например, postgres), отдельным запросом.
CREATE USER lab3_user WITH PASSWORD 'CHANGE_ME_STRONG_PASSWORD';
CREATE DATABASE lab3_dictionary OWNER lab3_user ENCODING 'UTF8';
-- Затем подключиться к lab3_dictionary под lab3_user и запустить init.sql.
