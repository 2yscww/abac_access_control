-- ! 数据预备

-- ? 部门表
insert into departments(dept_name) VALUES('管理层');


-- ? 分公司表
INSERT INTO branches(branch_name) VALUES('久仰哈基米科技');

-- ? 员工表

INSERT INTO employees(employee_code,employee_name,dept_id,branch_id,level,password) VALUES(1001,'冬雪莲',1,1,9,'$2a$10$oRZKjeelAEwFX2mZQciPVe0Zwq9Jq/s1.ahjjQ6by.f2emPTH1qNy');