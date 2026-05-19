package com.xie.platform.mapper;

import com.xie.platform.model.Department;
import com.xie.platform.model.enumValue.DeptType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@ActiveProfiles("test")
class DepartmentMapperIntegrationTest {

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM departments");
        jdbcTemplate.update("DELETE FROM employees");

        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                1L,
                "R&D",
                "RD",
                88L
        );
        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                2L,
                "QA",
                "QA",
                99L
        );
        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                3L,
                "Management",
                "MANAGEMENT",
                100L
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, status) VALUES (?, ?, ?, ?, ?)",
                88L,
                "E088",
                "RD Manager",
                1L,
                "INACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, status) VALUES (?, ?, ?, ?, ?)",
                99L,
                "E099",
                "QA Manager",
                2L,
                "ACTIVE"
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, status) VALUES (?, ?, ?, ?, ?)",
                100L,
                "E100",
                "Management Manager",
                3L,
                "ACTIVE"
        );
    }

    @Test
    void selectById_shouldMapDeptTypeAndManagerIdFromDatabase() {
        Department department = departmentMapper.selectById(1L);

        assertNotNull(department);
        assertEquals(1L, department.getDeptId());
        assertEquals("R&D", department.getDeptName());
        assertEquals(DeptType.RD, department.getDeptType());
        assertEquals(88L, department.getManagerId());
    }

    @Test
    void selectAll_shouldReturnDepartmentsOrderedByDeptId() {
        assertIterableEquals(
                java.util.List.of(1L, 2L, 3L),
                departmentMapper.selectAll().stream().map(Department::getDeptId).toList()
        );
    }

    @Test
    void updateManagerId_shouldPersistNewManagerId() {
        departmentMapper.updateManagerId(2L, 108L);

        Department department = departmentMapper.selectById(2L);
        assertNotNull(department);
        assertEquals(108L, department.getManagerId());
    }

    @Test
    void selectByManagerId_shouldReturnMatchingDepartments() {
        assertEquals(1, departmentMapper.selectByManagerId(88L).size());
        assertEquals(1L, departmentMapper.selectByManagerId(88L).get(0).getDeptId());
    }

    @Test
    void selectByDeptType_shouldReturnDepartmentByEnumName() {
        Department department = departmentMapper.selectByDeptType(DeptType.QA);

        assertNotNull(department);
        assertEquals(2L, department.getDeptId());
        assertEquals("QA", department.getDeptName());
    }

    @Test
    void selectWithInactiveManager_shouldReturnDepartmentsNeedingHandover() {
        var todos = departmentMapper.selectWithInactiveManager();

        assertEquals(1, todos.size());
        assertEquals(1L, todos.get(0).getDeptId());
        assertEquals("R&D", todos.get(0).getDeptName());
        assertEquals(88L, todos.get(0).getManagerId());
        assertEquals("E088", todos.get(0).getManagerCode());
        assertEquals("RD Manager", todos.get(0).getManagerName());
    }

    @Test
    void selectById_shouldReturnNullWhenDepartmentMissing() {
        assertNull(departmentMapper.selectById(999L));
    }
}
