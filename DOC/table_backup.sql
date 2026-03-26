-- ! 部门表
CREATE TABLE departments (
    dept_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dept_name VARCHAR(64) NOT NULL UNIQUE,
    dept_type VARCHAR(64) NOT NULL,
    manager_id BIGINT, 
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- * departments：部门表，对应企业组织结构（ABAC中的组织属性来源）

-- dept_id：部门唯一标识
-- dept_name：部门名称（如 产品部、研发部、测试部、运维部、管理层）
-- dept_type：部门类别（如 技术类 / 业务类 / 管理类，可用于策略判断）

-- manager_id：部门负责人（员工ID）
--   - 表示该部门的直接管理者（如研发部负责人、测试负责人等）
--   - 在权限控制中可用于：
--       1. 审批类操作（如阶段推进、资源审批）
--       2. 高权限访问控制（如跨阶段访问）
--       3. 审计责任归属

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
    employee_code VARCHAR(32) NOT NULL UNIQUE,
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
-- emplpyee _code: 员工工号
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

-- ! 项目成员表
CREATE TABLE project_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    joined_phase INT NOT NULL,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    left_at DATETIME,

    CONSTRAINT fk_pm_project
        FOREIGN KEY (project_id) REFERENCES projects(project_id),

    CONSTRAINT fk_pm_employee
        FOREIGN KEY (employee_id) REFERENCES employees(employee_id)
);

-- * project_members：项目成员表，用于记录员工参与项目的关系及其生命周期（支持审计与ABAC）

-- id：主键
-- project_id：所属项目ID
-- employee_id：参与该项目的员工ID

-- * status：成员状态
--   ACTIVE：当前参与项目
--   INACTIVE：已退出项目（不物理删除，用于保留历史）

-- * joined_phase：加入项目时的项目阶段
--   用于记录成员是在项目哪个阶段进入的（如研发阶段加入、测试阶段加入）
--   可用于ABAC中的环境属性判断

-- left_at：退出项目时间（为空表示仍在项目中）
--   当成员退出项目时填写，用于历史审计与追溯

-- joined_at：加入项目时间

-- ? 本表不做物理删除（DELETE），而是通过 status + left_at 记录成员生命周期
-- ? 支持项目成员的历史追溯（谁在什么阶段参与过项目）

-- ? 可用于ABAC决策，例如：
--   - 判断用户是否为当前阶段的有效成员（status = ACTIVE）
--   - 判断用户是否在某阶段参与过项目（joined_phase <= 当前阶段）



-- ! 项目表
CREATE TABLE projects (
    project_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(128) NOT NULL,
    project_phase INT NOT NULL,
    security_level INT NOT NULL,
    created_by_employee_id BIGINT,
    owner_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- * projects：项目表，用于项目维度的权限控制与资源归属

-- project_id：项目唯一标识
-- project_name：项目名称（如 AI手机项目）
-- * project_phase：项目阶段
-- * security_level 项目保密等级 (公开、内部、机密、高度机密)
-- created_by_employee_id：项目创建人（员工ID）
-- owner_id : 实际负责人(员工ID)
-- created_at：项目创建时间
-- updated_at：项目更新时间

-- * 立项 → 需求设计 → 研发实现 → 测试验证 → 上线交付 → 归档
-- ? 数据库层面不使用枚举类型规定项目阶段，代码层面用枚举写死


-- ! 项目资产表
CREATE TABLE project_assets (
    asset_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    asset_name VARCHAR(255) NOT NULL,
    assets_type INT NOT NULL,
    assets_stage INT NOT NULL,
    security_level INT NOT NULL,
    created_by_employee_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    file_path VARCHAR(512),
    file_size BIGINT,
    description TEXT,
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
-- ? asset_stage：资源产生阶段 (立项、需求设计、研发实现、测试验证、上线交付、归档) （历史快照，不随项目阶段变化）
-- ? security_level：资源密级(公开、内部、机密、高度机密)
-- created_by_employee_id：资源创建人
-- created_at：创建时间
-- file_path : 文件路径
-- file_size : 文件大小 字节
-- description : 资产描述



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
);

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
    resource_type VARCHAR(32) NOT NULL,
    resource_id BIGINT,
    project_id BIGINT,
    action VARCHAR(32) NOT NULL,
    decision VARCHAR(16) NOT NULL,
    trigger_policy VARCHAR(128),
    deny_reason VARCHAR(255),
    project_phase INT,
    assets_stage INT,
    security_level INT,
    request_ip VARCHAR(64),
    request_uri VARCHAR(255),
    request_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(employee_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_audit_project
        FOREIGN KEY (project_id)
        REFERENCES projects(project_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX idx_audit_employee_time
    ON audit_logs(employee_id, request_time);

CREATE INDEX idx_audit_action_time
    ON audit_logs(action, request_time);

CREATE INDEX idx_audit_resource
    ON audit_logs(resource_type, resource_id);

CREATE INDEX idx_audit_project
    ON audit_logs(project_id, request_time);


-- audit_logs：安全审计日志表，用于记录系统操作行为与权限决策情况

-- 字段说明
-- resource_type: PROJECT / ASSET
-- resource_id:
--   - PROJECT 日志时，对应 project_id
--   - ASSET 日志时，对应 asset_id
-- project_id:
--   - 项目日志时，等于 project_id
--   - 资产日志时，表示资产所属项目
-- trigger_policy: 触发本次决策的规则名，例如 SecurityLevelPolicy
-- project_phase / assets_stage / security_level:
--   用于保存授权发生当下的资源快照，避免历史审计受后续资源变更影响


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



ALTER TABLE departments
ADD CONSTRAINT fk_dept_manager
FOREIGN KEY (manager_id)
REFERENCES employees(employee_id)
ON DELETE SET NULL
ON UPDATE CASCADE;