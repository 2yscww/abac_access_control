DROP TABLE IF EXISTS project_assets;
DROP TABLE IF EXISTS project_members;
DROP TABLE IF EXISTS policies;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS branches;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    dept_id BIGINT PRIMARY KEY,
    dept_name VARCHAR(255) NOT NULL,
    dept_type VARCHAR(64) NOT NULL,
    manager_id BIGINT
);

CREATE TABLE branches (
    branch_id BIGINT PRIMARY KEY,
    branch_name VARCHAR(255) NOT NULL
);

CREATE TABLE employees (
    employee_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_code VARCHAR(64),
    employee_name VARCHAR(255),
    dept_id BIGINT,
    branch_id BIGINT,
    level INT,
    current_projects VARCHAR(1000),
    is_contractor BOOLEAN,
    status VARCHAR(64),
    password VARCHAR(255),
    must_change_password BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE projects (
    project_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_name VARCHAR(255),
    project_phase INT,
    security_level INT,
    created_by_employee_id BIGINT,
    owner_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    status VARCHAR(64) NOT NULL,
    joined_phase INT,
    joined_at TIMESTAMP NULL,
    left_at TIMESTAMP NULL
);

CREATE TABLE project_assets (
    asset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    assets_type INT,
    assets_stage INT,
    security_level INT,
    created_by_employee_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    file_path VARCHAR(512),
    file_size BIGINT,
    description VARCHAR(1000)
);

CREATE TABLE policies (
    policy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_name VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    conditions CLOB,
    effect VARCHAR(64),
    priority INT,
    enabled BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NULL,
    resource_type VARCHAR(64),
    resource_id BIGINT NULL,
    project_id BIGINT NULL,
    action VARCHAR(64),
    decision VARCHAR(32),
    trigger_policy VARCHAR(128),
    deny_reason VARCHAR(255) NULL,
    project_phase INT NULL,
    assets_stage INT NULL,
    security_level INT NULL,
    request_ip VARCHAR(64) NULL,
    network_zone VARCHAR(32) NULL,
    request_uri VARCHAR(255) NULL,
    request_time TIMESTAMP NULL,
    detail_json CLOB NULL
);
