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
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                1L,
                "R&D",
                "RD",
                88L
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
}
