CREATE ROLE cbas_user WITH LOGIN ENCRYPTED PASSWORD 'cbas_password';
CREATE DATABASE cbas_db OWNER cbas_user;
\c cbas_db;
