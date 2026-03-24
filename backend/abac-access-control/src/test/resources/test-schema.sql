DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(255) NOT NULL,
    dept_type VARCHAR(64) NOT NULL,
    manager_id BIGINT
);
