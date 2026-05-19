package com.xie.platform.mapper;

import com.xie.platform.dto.AssetQueryDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.dto.ProjectMemberDTO;
import com.xie.platform.dto.ProjectQueryDTO;
import com.xie.platform.model.Branch;
import com.xie.platform.model.PolicyConfig;
import com.xie.platform.model.ProjectAssets;
import com.xie.platform.model.ProjectMember;
import com.xie.platform.model.Projects;
import com.xie.platform.model.enumValue.AssetType;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeStatus;
import com.xie.platform.model.enumValue.ProjectMemberStatus;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class StorageMapperIntegrationTest {

    @Autowired
    private EmployeesMapper employeesMapper;

    @Autowired
    private BranchMapper branchMapper;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    @Autowired
    private ProjectAssetsMapper projectAssetsMapper;

    @Autowired
    private PolicyConfigMapper policyConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM project_assets");
        jdbcTemplate.update("DELETE FROM project_members");
        jdbcTemplate.update("DELETE FROM policies");
        jdbcTemplate.update("DELETE FROM projects");
        jdbcTemplate.update("DELETE FROM employees");
        jdbcTemplate.update("DELETE FROM branches");
        jdbcTemplate.update("DELETE FROM departments");

        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                1L, "Product", "PRODUCT", 101L
        );
        jdbcTemplate.update(
                "INSERT INTO departments (dept_id, dept_name, dept_type, manager_id) VALUES (?, ?, ?, ?)",
                2L, "R&D", "RD", 102L
        );
        jdbcTemplate.update(
                "INSERT INTO branches (branch_id, branch_name) VALUES (?, ?)",
                1L, "Shanghai"
        );
        jdbcTemplate.update(
                "INSERT INTO branches (branch_id, branch_name) VALUES (?, ?)",
                2L, "Shenzhen"
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, branch_id, level, current_projects, is_contractor, status, password, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                101L, "E101", "Product Manager", 1L, 1L, 7, "[]", false, "ACTIVE", "pwd", false
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, branch_id, level, current_projects, is_contractor, status, password, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                102L, "E102", "RD Manager", 2L, 1L, 8, "[]", false, "ACTIVE", "pwd", false
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, branch_id, level, current_projects, is_contractor, status, password, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                103L, "E103", "Developer", 2L, 2L, 6, "[]", false, "ACTIVE", "pwd", false
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, branch_id, level, current_projects, is_contractor, status, password, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                104L, "E104", "Contractor", 2L, 2L, 5, "[]", true, "ACTIVE", "pwd", false
        );
        jdbcTemplate.update(
                "INSERT INTO employees (employee_id, employee_code, employee_name, dept_id, branch_id, level, current_projects, is_contractor, status, password, must_change_password) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                105L, "E105", "Inactive Employee", 2L, 1L, 5, "[]", false, "INACTIVE", "pwd", false
        );

        jdbcTemplate.update(
                "INSERT INTO projects (project_id, project_name, project_phase, security_level, created_by_employee_id, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                201L, "Apollo", 3, 2, 101L, 102L,
                LocalDateTime.of(2026, 4, 1, 10, 0, 0),
                LocalDateTime.of(2026, 4, 1, 10, 0, 0)
        );
        jdbcTemplate.update(
                "INSERT INTO projects (project_id, project_name, project_phase, security_level, created_by_employee_id, owner_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                202L, "Beacon", 4, 3, 102L, 103L,
                LocalDateTime.of(2026, 4, 2, 10, 0, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0, 0)
        );

        jdbcTemplate.update(
                "INSERT INTO project_members (id, project_id, employee_id, status, joined_phase, joined_at, left_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                301L, 201L, 102L, "ACTIVE", 3,
                LocalDateTime.of(2026, 4, 1, 10, 0, 0), null
        );
        jdbcTemplate.update(
                "INSERT INTO project_members (id, project_id, employee_id, status, joined_phase, joined_at, left_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                302L, 201L, 103L, "ACTIVE", 3,
                LocalDateTime.of(2026, 4, 1, 11, 0, 0), null
        );
        jdbcTemplate.update(
                "INSERT INTO project_members (id, project_id, employee_id, status, joined_phase, joined_at, left_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                303L, 201L, 104L, "INACTIVE", 2,
                LocalDateTime.of(2026, 3, 20, 11, 0, 0),
                LocalDateTime.of(2026, 4, 1, 12, 0, 0)
        );

        jdbcTemplate.update(
                "INSERT INTO project_assets (asset_id, project_id, asset_name, assets_type, assets_stage, security_level, created_by_employee_id, created_at, file_path, file_size, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                401L, 201L, "Design Spec", 2, 2, 2, 102L,
                LocalDateTime.of(2026, 4, 2, 9, 0, 0),
                "minio://asset/design-spec.pdf", 2048L, "Requirement design"
        );
        jdbcTemplate.update(
                "INSERT INTO project_assets (asset_id, project_id, asset_name, assets_type, assets_stage, security_level, created_by_employee_id, created_at, file_path, file_size, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                402L, 202L, "Test Report", 4, 4, 3, 103L,
                LocalDateTime.of(2026, 4, 3, 9, 0, 0),
                "https://example.com/report.pdf", 4096L, "Release report"
        );

        jdbcTemplate.update(
                "INSERT INTO policies (policy_id, policy_name, description, conditions, effect, priority, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                501L, "SecurityLevelPolicy", "Security policy", "{\"publicMinRank\":1}", "DENY", 10, true
        );
        jdbcTemplate.update(
                "INSERT INTO policies (policy_id, policy_name, description, conditions, effect, priority, enabled) VALUES (?, ?, ?, ?, ?, ?, ?)",
                502L, "EnvironmentAccessPolicy", "Environment policy", "{\"workStart\":\"08:00\"}", "DENY", 20, true
        );
    }

    @Test
    void employeesMapper_shouldSupportLookupByCodeAndId() {
        assertEquals("Developer", employeesMapper.selectByEmployeeCode("E103").getEmployeeName());
        assertEquals(2L, employeesMapper.selectByEmployeeId(102L).getDeptId());
    }

    @Test
    void employeesMapper_shouldReturnOnlyActiveOptionsForKeywordAndDepartment() {
        List<EmployeeOptionDTO> keywordMatches = employeesMapper.selectActiveOptions("E10");
        List<EmployeeOptionDTO> departmentMatches = employeesMapper.selectActiveOptionsByDeptId(2L);

        assertEquals(4, keywordMatches.size());
        assertEquals(List.of(102L, 103L, 104L), departmentMatches.stream().map(EmployeeOptionDTO::getEmployeeId).toList());
        assertTrue(departmentMatches.stream().allMatch(item -> item.getStatus() == EmployeeStatus.ACTIVE));
    }

    @Test
    void branchMapper_shouldReturnSingleBranchAndOrderedBranchList() {
        Branch branch = branchMapper.selectById(2L);

        assertNotNull(branch);
        assertEquals("Shenzhen", branch.getBranchName());
        assertEquals(List.of(1L, 2L), branchMapper.selectAll().stream().map(Branch::getBranchId).toList());
    }

    @Test
    void projectMapper_shouldFilterAndCountProjectsByCondition() {
        ProjectQueryDTO query = new ProjectQueryDTO();
        query.setProjectName("Apo");
        query.setProjectPhase(3);
        query.setSecurityLevel(2);
        query.setPageNum(null);
        query.setPageSize(null);

        List<Projects> projects = projectMapper.selectByCondition(query);

        assertEquals(1, projects.size());
        assertEquals("Apollo", projects.get(0).getProjectName());
        assertEquals(1, projectMapper.countByCondition(query));
    }

    @Test
    void projectMapper_shouldSupportPhaseLookupAndOwnerUpdate() {
        assertEquals(List.of(201L), projectMapper.selectByPhaseCodes(List.of(3)).stream().map(Projects::getProjectId).toList());

        projectMapper.updateOwnerByPhaseCodes(List.of(3, 4), 101L);

        assertEquals(101L, projectMapper.selectById(201L).getOwnerId());
        assertEquals(101L, projectMapper.selectById(202L).getOwnerId());
    }

    @Test
    void projectMemberMapper_shouldQueryAndUpdateMemberships() {
        ProjectMember existing = projectMemberMapper.selectByProjectIdAndEmployeeId(201L, 103L);
        assertNotNull(existing);
        assertEquals(ProjectMemberStatus.ACTIVE, existing.getStatus());
        assertEquals(1, projectMemberMapper.countActiveMember(201L, 103L));

        projectMemberMapper.deactivate(201L, 103L);
        assertEquals(0, projectMemberMapper.countActiveMember(201L, 103L));

        projectMemberMapper.reactivate(201L, 103L, ProjectPhase.TEST);
        assertEquals(1, projectMemberMapper.countActiveMember(201L, 103L));
        assertEquals(ProjectPhase.TEST, projectMemberMapper.selectByProjectIdAndEmployeeId(201L, 103L).getJoinedPhase());
    }

    @Test
    void projectMemberMapper_shouldReturnJoinedMemberViewAndDeactivateByEmployeeId() {
        List<ProjectMemberDTO> members = projectMemberMapper.selectByProjectId(201L);
        List<ProjectMemberDTO> activeMembers = projectMemberMapper.selectActiveByProjectId(201L);

        assertEquals(3, members.size());
        assertEquals(2, activeMembers.size());
        assertEquals(DeptType.RD, activeMembers.get(0).getDeptType());

        projectMemberMapper.deactivateByEmployeeId(102L);
        assertEquals(0, projectMemberMapper.countActiveMember(201L, 102L));
    }

    @Test
    void projectAssetsMapper_shouldFilterCountAndDeleteAssets() {
        AssetQueryDTO query = new AssetQueryDTO();
        query.setProjectId(201L);
        query.setAssetsType(2);
        query.setSecurityLevel(2);
        query.setPageNum(null);
        query.setPageSize(null);

        List<ProjectAssets> assets = projectAssetsMapper.selectByCondition(query);

        assertEquals(1, assets.size());
        assertEquals("Design Spec", assets.get(0).getAssetName());
        assertEquals(1, projectAssetsMapper.countByCondition(query));
        assertEquals(1, projectAssetsMapper.selectByProjectId(201L).size());

        projectAssetsMapper.deleteById(401L);
        assertNull(projectAssetsMapper.selectById(401L));
    }

    @Test
    void projectAssetsMapper_shouldDeleteAllAssetsByProjectId() {
        projectAssetsMapper.deleteByProjectId(202L);

        assertEquals(0, projectAssetsMapper.selectByProjectId(202L).size());
    }

    @Test
    void policyConfigMapper_shouldQueryInsertAndUpdateConfigs() {
        PolicyConfig existing = policyConfigMapper.selectByPolicyName("SecurityLevelPolicy");
        assertNotNull(existing);
        assertEquals(501L, existing.getPolicyId());

        List<PolicyConfig> configs = policyConfigMapper.selectByPolicyNames(
                List.of("EnvironmentAccessPolicy", "SecurityLevelPolicy")
        );
        assertEquals(List.of("SecurityLevelPolicy", "EnvironmentAccessPolicy"),
                configs.stream().map(PolicyConfig::getPolicyName).toList());

        PolicyConfig inserted = new PolicyConfig();
        inserted.setPolicyName("HistoricalExportPolicy");
        inserted.setDescription("Historical export policy");
        inserted.setConditions("{\"exportThreshold\":50}");
        inserted.setEffect("DENY");
        inserted.setPriority(30);
        inserted.setEnabled(true);
        policyConfigMapper.insert(inserted);
        assertNotNull(inserted.getPolicyId());

        inserted.setDescription("Updated export policy");
        inserted.setEnabled(false);
        policyConfigMapper.update(inserted);

        PolicyConfig refreshed = policyConfigMapper.selectByPolicyName("HistoricalExportPolicy");
        assertEquals("Updated export policy", refreshed.getDescription());
        assertEquals(false, refreshed.getEnabled());
    }
}
