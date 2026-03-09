package com.xie.platform.access.pdp;

import com.xie.platform.access.action.Action;
import com.xie.platform.access.environment.Environment;
import com.xie.platform.access.policy.impl.PhaseAccessPolicy;
import com.xie.platform.access.policy.impl.SecurityLevelPolicy;
import com.xie.platform.access.resource.Resource;
import com.xie.platform.access.resource.ResourceType;
import com.xie.platform.access.subject.Subject;
import com.xie.platform.model.enumValue.DeptType;
import com.xie.platform.model.enumValue.EmployeeLevel;
import com.xie.platform.model.enumValue.ProjectPhase;
import com.xie.platform.model.enumValue.SecurityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDP 单元测试
 *
 * 测试策略：
 * 1. 不启动 Spring 容器，手动构造 PDP 和规则
 * 2. 构造各种 Subject + Resource + Action 组合
 * 3. 验证决策结果是否符合预期
 */
class AbacPolicyDecisionPointTest {

    private AbacPolicyDecisionPoint pdp;
    private Environment defaultEnv;
    private List<TestRecord> testRecords = new ArrayList<>();

    static class TestRecord {
        String testName;
        boolean passed;
        boolean expectedAllow;
        boolean actualAllow;
        String triggerPolicy;
        String reason;

        TestRecord(String testName, boolean passed, boolean expectedAllow, boolean actualAllow, String triggerPolicy, String reason) {
            this.testName = testName;
            this.passed = passed;
            this.expectedAllow = expectedAllow;
            this.actualAllow = actualAllow;
            this.triggerPolicy = triggerPolicy;
            this.reason = reason;
        }
    }

    @BeforeEach
    void setUp() {
        // 手动构造 PDP，注入两条规则
        pdp = new AbacPolicyDecisionPoint();
        pdp.allPolicies = Arrays.asList(
                new SecurityLevelPolicy(),
                new PhaseAccessPolicy()
        );
        pdp.init(); // 触发 @PostConstruct 逻辑

        // 默认环境上下文
        defaultEnv = Environment.builder()
                .requestTime(LocalDateTime.now())
                .ipAddress("192.168.1.100")
                .build();

        System.out.println("\n========== ABAC 策略决策点测试报告 ==========");
        System.out.println("已加载策略：SecurityLevelPolicy, PhaseAccessPolicy");
        System.out.println("测试环境：IP=" + defaultEnv.getIpAddress() + ", Time=" + defaultEnv.getRequestTime());
        System.out.println("============================================\n");
    }

    private void printTestResult(String testName, Subject subject, Resource resource, Action action, DecisionResult result) {
        System.out.println("【测试】" + testName);
        System.out.println("  主体：" + formatSubject(subject));
        System.out.println("  资源：" + formatResource(resource));
        System.out.println("  操作：" + action);
        System.out.println("  决策：" + (result.isAllowed() ? "✅ 允许" : "❌ 拒绝"));
        System.out.println("  触发策略：" + result.getTriggerPolicy());
        System.out.println("  原因：" + result.getReason());
        System.out.println();
    }

    private void recordTest(String testName, boolean expectedAllow, DecisionResult result) {
        boolean actualAllow = result.isAllowed();
        boolean passed = (expectedAllow == actualAllow);
        testRecords.add(new TestRecord(testName, passed, expectedAllow, actualAllow,
            result.getTriggerPolicy(), result.getReason()));
    }

    @org.junit.jupiter.api.AfterAll
    static void printSummary() {
        System.out.println("\n========== 测试总结报告 ==========");
    }

    @org.junit.jupiter.api.AfterEach
    void printTestSummary() {
        if (!testRecords.isEmpty()) {
            TestRecord last = testRecords.get(testRecords.size() - 1);
            System.out.println(">>> 测试结果：" + (last.passed ? "✅ PASS" : "❌ FAIL"));
            System.out.println(">>> 预期：" + (last.expectedAllow ? "允许" : "拒绝") +
                             " | 实际：" + (last.actualAllow ? "允许" : "拒绝"));
            System.out.println("=".repeat(50) + "\n");
        }
    }

    private String formatSubject(Subject subject) {
        return String.format("%s部门 %s %s",
            subject.getDeptType(),
            subject.getLevel(),
            subject.getIsContractor() ? "(外包)" : "(正式)");
    }

    private String formatResource(Resource resource) {
        return String.format("%s [%s] [%s阶段]",
            resource.getType(),
            resource.getSecurityLevel(),
            resource.getProjectPhase() != null ? resource.getProjectPhase() : "无");
    }

    // ========== 安全策略层测试 ==========

    @Test
    @DisplayName("安全策略：P4 员工访问 CONFIDENTIAL 资源 → 拒绝（职级不够）")
    void testSecurityPolicy_InsufficientLevel() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P4, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("P4 员工访问 CONFIDENTIAL 资源", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        recordTest("P4 员工访问 CONFIDENTIAL 资源", false, result);
        assertEquals("SecurityLevelPolicy", result.getTriggerPolicy());
        assertTrue(result.getReason().contains("安全策略拒绝"));
    }

    @Test
    @DisplayName("安全策略：P6 正式员工访问 CONFIDENTIAL 资源 → 通过")
    void testSecurityPolicy_SufficientLevel() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("P6 正式员工访问 CONFIDENTIAL 资源", subject, resource, Action.READ, result);

        assertTrue(result.isAllowed());
        recordTest("P6 正式员工访问 CONFIDENTIAL 资源", true, result);
    }

    @Test
    @DisplayName("安全策略：外包 VP 访问 CONFIDENTIAL 资源 → 拒绝（外包限制）")
    void testSecurityPolicy_ContractorDenied() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.VP, true); // 外包
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("外包 VP 访问 CONFIDENTIAL 资源", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        assertEquals("SecurityLevelPolicy", result.getTriggerPolicy());
    }

    @Test
    @DisplayName("安全策略：外包员工访问 INTERNAL 资源 → 通过")
    void testSecurityPolicy_ContractorAllowedInternal() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P3, true); // 外包
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("外包员工访问 INTERNAL 资源", subject, resource, Action.READ, result);

        assertTrue(result.isAllowed());
    }

    // ========== 项目策略层测试 ==========

    @Test
    @DisplayName("项目策略：研发部 READ 立项阶段项目 → 拒绝（不在允许列表）")
    void testPhasePolicy_RdReadInitPhase() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P5, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.INIT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("研发部 READ 立项阶段项目", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
        assertTrue(result.getReason().contains("项目策略拒绝"));
    }

    @Test
    @DisplayName("项目策略：产品部 WRITE 需求设计阶段资产 → 通过")
    void testPhasePolicy_ProductWriteRequirement() {
        Subject subject = new Subject(1L, 1L, DeptType.PRODUCT, 1L, EmployeeLevel.P5, false);
        Resource resource = Resource.builder()
                .type(ResourceType.ASSET)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.REQUIREMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.WRITE, defaultEnv);
        printTestResult("产品部 WRITE 需求设计阶段资产", subject, resource, Action.WRITE, result);

        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("项目策略：管理层 READ 研发阶段项目 → 通过（监管权）")
    void testPhasePolicy_ManagementReadDevelopment() {
        Subject subject = new Subject(1L, 1L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("管理层 READ 研发阶段项目", subject, resource, Action.READ, result);

        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("项目策略：管理层 DELETE 研发阶段项目 → 拒绝（监管权仅限 READ）")
    void testPhasePolicy_ManagementDeleteDevelopment() {
        Subject subject = new Subject(1L, 1L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.DELETE, defaultEnv);
        printTestResult("管理层 DELETE 研发阶段项目", subject, resource, Action.DELETE, result);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    @DisplayName("项目策略：管理层 DELETE 立项阶段项目 → 通过（立项阶段管理层是业务参与方）")
    void testPhasePolicy_ManagementDeleteInitPhase() {
        Subject subject = new Subject(1L, 1L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.INIT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.DELETE, defaultEnv);
        printTestResult("管理层 DELETE 立项阶段项目", subject, resource, Action.DELETE, result);

        assertTrue(result.isAllowed());
    }

    @Test
    @DisplayName("项目策略：测试部 READ 归档项目 → 拒绝（归档只允许管理层）")
    void testPhasePolicy_QaReadArchived() {
        Subject subject = new Subject(1L, 1L, DeptType.QA, 1L, EmployeeLevel.P5, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.ARCHIVED)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("测试部 READ 归档项目", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
    }

    @Test
    @DisplayName("项目策略：管理层 WRITE 归档项目 → 拒绝（归档只读）")
    void testPhasePolicy_ManagementWriteArchived() {
        Subject subject = new Subject(1L, 1L, DeptType.MANAGEMENT, 1L, EmployeeLevel.VP, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.ARCHIVED)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.WRITE, defaultEnv);
        printTestResult("管理层 WRITE 归档项目", subject, resource, Action.WRITE, result);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy());
    }

    // ========== 多层联合决策测试 ==========

    @Test
    @DisplayName("多层决策：P4 研发访问 CONFIDENTIAL 研发阶段项目 → 拒绝（安全策略层拦截）")
    void testMultiLayer_SecurityLayerBlocks() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P4, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.CONFIDENTIAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("P4 研发访问 CONFIDENTIAL 研发阶段项目（安全层拦截）", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        assertEquals("SecurityLevelPolicy", result.getTriggerPolicy()); // 安全策略层先拒绝
    }

    @Test
    @DisplayName("多层决策：P6 研发访问 INTERNAL 立项阶段项目 → 拒绝（项目策略层拦截）")
    void testMultiLayer_ProjectLayerBlocks() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.INIT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("P6 研发访问 INTERNAL 立项阶段项目（项目层拦截）", subject, resource, Action.READ, result);

        assertFalse(result.isAllowed());
        assertEquals("PhaseAccessPolicy", result.getTriggerPolicy()); // 安全策略通过，项目策略拒绝
    }

    @Test
    @DisplayName("多层决策：P6 研发访问 INTERNAL 研发阶段项目 → 通过（两层都通过）")
    void testMultiLayer_AllLayersPass() {
        Subject subject = new Subject(1L, 1L, DeptType.RD, 1L, EmployeeLevel.P6, false);
        Resource resource = Resource.builder()
                .type(ResourceType.PROJECT)
                .securityLevel(SecurityLevel.INTERNAL)
                .projectPhase(ProjectPhase.DEVELOPMENT)
                .build();

        DecisionResult result = pdp.evaluate(subject, resource, Action.READ, defaultEnv);
        printTestResult("P6 研发访问 INTERNAL 研发阶段项目（两层都通过）", subject, resource, Action.READ, result);

        assertTrue(result.isAllowed());
        assertEquals("default-allow", result.getTriggerPolicy());
    }
}
