-- ! 数据预备

-- ? 密码都修改为 ABAC123

-- ? 部门表
insert into departments(dept_name) VALUES('管理层');


-- ? 分公司表
INSERT INTO branches(branch_name) VALUES('久仰哈基米科技');

-- ? 员工表

-- * 管理层人员
INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1001,'冬雪莲',1,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');


-- * HR人员
INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1002,'东洋雪莲',2,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');

-- * 产品部人员
INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1003,'王小明',3,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');


-- * 研发部人员

INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1004,'山东小红',4,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');

-- * 测试部人员
INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1005,'丁源',5,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');


-- * 运维部人员

INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1006,'运维人员',5,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');


