-- ! 部门表
CREATE TABLE departments (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- departments：部门表
-- dept_id：部门唯一标识
-- dept_name：部门名称（如 R&D、QA、Legal）
-- created_at：记录创建时间
-- updated_at：记录更新时间


-- ! 分公司表
CREATE TABLE branches (
    branch_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    branch_name VARCHAR(64) NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- branches：分公司表
-- branch_id：分公司唯一标识
-- branch_name：分公司名称
-- created_at：记录创建时间
-- updated_at：记录更新时间

-- ! 员工表
CREATE TABLE employees (
    employee_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_name VARCHAR(64) NOT NULL,
    dept_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    level INT NOT NULL,
    current_projects JSON,
    is_contractor BOOLEAN DEFAULT FALSE,
    status VARCHAR(16) DEFAULT 'ACTIVE', 
    password VARCHAR(255) NOT NULL ,              
    must_change_password BOOLEAN DEFAULT TRUE,    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_dept
        FOREIGN KEY (dept_id)
        REFERENCES departments(dept_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_employee_branch
        FOREIGN KEY (branch_id)
        REFERENCES branches(branch_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- * employees：员工表，对应 ABAC 中的 Subject（主体）
-- employee_id：员工唯一标识
-- username：员工姓名 / 登录名
-- dept：所属部门（如 R&D、QA、Legal）
-- branch：所属分公司
-- ? level：员工职级或安全级别（如 P5、P8、VP）
-- current_projects：当前参与的项目列表（JSON 数组）
-- is_contractor：是否为外包或临时员工
-- status 员工状态: ACTIVE(在职), INACTIVE(离职)
-- password 登录密码（加密存储）
-- must_change_password 是否首次登录需要修改密码
-- created_at：记录创建时间
-- updated_at：记录更新时间





-- ! 项目表
CREATE TABLE projects (
    project_id BIGINT PRIMARY KEY ,
    project_name VARCHAR(128) NOT NULL,
    project_phase INT NOT NULL,
    created_by_employee_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- * projects：项目表，用于项目维度的权限控制与资源归属

-- project_id：项目唯一标识
-- project_name：项目名称（如 AI手机项目）
-- * project_phase：项目阶段
-- created_by_employee_id：项目创建人（员工ID）
-- created_at：项目创建时间
-- updated_at：项目更新时间

-- * 立项 → 需求设计 → 研发实现 → 测试验证 → 上线交付 → 归档
-- ? 数据库层面不使用枚举类型规定项目阶段，代码层面用枚举写死


-- ! 项目资产表
CREATE TABLE project_assets (
    asset_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    assets_type VARCHAR(32) NOT NULL,
    assets_stage VARCHAR(32) NOT NULL,
    security_level VARCHAR(16) NOT NULL,
    created_by_employee_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_assets_project
    FOREIGN KEY (project_id)
    REFERENCES projects(project_id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
);

-- * project_assets：项目资源表，统一管理项目全生命周期产生的资源对象

-- asset_id：资源唯一标识
-- project_id：所属项目ID
-- asset_name：资源名称
-- ? asset_type：资源类型(需求文档、设计文档、源代码、测试报告、部署脚本、运维文档)
-- ? asset_stage：资源产生阶段 (立项、需求设计、研发实现、测试验证、上线交付、归档)
-- ? security_level：资源密级(公开、内部、机密、高度机密)
-- created_by_employee_id：资源创建人
-- created_at：创建时间



-- ! 策略表
CREATE TABLE policies (
    policy_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_name VARCHAR(128) NOT NULL,
    description TEXT,
    conditions JSON NOT NULL,
    effect VARCHAR(16) NOT NULL,
    priority INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) 

-- policies：ABAC策略表，用于存储访问控制策略

-- policy_id：策略唯一标识
-- policy_name：策略名称
-- description：策略描述
-- conditions：策略条件（ABAC规则JSON，包括主体、资源、环境属性及逻辑）
-- effect：策略效果（ALLOW / DENY）
-- priority：策略优先级，数值越大优先执行
-- enabled：策略是否启用，可动态开关
-- created_at：策略创建时间
-- updated_at：策略更新时间



-- ! 安全审计日志表
CREATE TABLE audit_logs (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    asset_id VARCHAR(64),
    action VARCHAR(32) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    matched_policies JSON,
    deny_reason VARCHAR(255),
    network_zone VARCHAR(32),
    device_safety FLOAT,
    access_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_audit_asset
        FOREIGN KEY (asset_id)
        REFERENCES project_assets(asset_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) 


-- audit_logs：安全审计日志表，用于记录系统操作行为与权限决策情况

-- log_id：审计日志唯一标识
-- user_id：执行操作的用户ID
-- resource_id：被访问的资源ID
-- action：操作类型（如 READ / WRITE / DELETE）
-- decision：权限决策结果（ALLOW / DENY）
-- matched_policies：命中的策略列表（JSON格式）
-- deny_reason：拒绝访问原因
-- network_zone：访问网络环境（如内网、VPN、公网）
-- device_safety：设备安全评分（如杀毒/补丁状态）
-- access_time：操作发生时间
