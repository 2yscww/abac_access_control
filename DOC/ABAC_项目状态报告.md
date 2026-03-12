# ABAC 访问控制系统 - 项目状态报告

## 一、项目概览

本项目实现了基于属性的访问控制（ABAC）系统，用于企业数字资产管理场景。核心特点是**多维度属性联合决策**和**分层优先级策略执行**。

---

## 二、ABAC 四要素模型

### 2.1 Subject（主体）— 谁在访问

**文件位置**：`access/subject/Subject.java`

**包含属性**：
- `employeeId` — 员工唯一标识
- `deptId` — 所属部门 ID
- `deptType` — 所属部门类型（PRODUCT/RD/QA/OPS/MANAGEMENT）
- `branchId` — 所属分公司 ID
- `level` — 员工职级（P1-P8/VP/DIRECTOR）
- `isContractor` — 是否外包人员

**来源**：登录时从 JWT token 中解析得到，由 `EmployeeAuthServiceImpl` 构建。

---

### 2.2 Resource（资源）— 访问什么

**文件位置**：`access/resource/Resource.java`

**包含属性**：
- `type` — 资源类型（PROJECT/ASSET）
- `projectPhase` — 所属项目当前阶段（立项/需求设计/研发实现/测试验证/上线交付/归档）
- `securityLevel` — 保密等级（PUBLIC/INTERNAL/CONFIDENTIAL/TOP_SECRET）
- `creatorId` — 创建人 ID
- `deptId` — 所属部门 ID

**设计意图**：Resource 是对 `Projects` 和 `ProjectAssets` 实体的属性抽取，让 PDP 与具体数据库模型解耦。

---

### 2.3 Action（动作）— 做什么操作

**文件位置**：`access/action/Action.java`

**枚举值**：
- `READ` — 读取（查看详情、列表）
- `WRITE` — 写入（创建、修改）
- `DELETE` — 删除
- `ADVANCE_PHASE` — 推进项目阶段
- `EXPORT` — 导出（预留）

---

### 2.4 Environment（环境）— 在什么环境下

**文件位置**：`access/environment/Environment.java`

**包含属性**：
- `requestTime` — 请求发起时间（用于工作时间段限制）
- `ipAddress` — 请求来源 IP（用于内外网区分）

**当前状态**：模型已定义，暂未在规则中使用，为后续扩展预留。

---

## 三、策略层架构

### 3.1 优先级分层

**文件位置**：`access/policy/PolicyLayer.java`

```
SECURITY（安全策略层）— 最高优先级
    ↓
PROJECT（项目策略层）— 次高优先级
    ↓
ROLE（角色属性层）— 最低优先级（预留）
```

**执行逻辑**：
- 高优先级层拒绝后，不再执行低优先级层
- 高优先级层可以发出 `FORCE_ALLOW`（强制放行），跳过所有后续层
- 所有层通过后，默认允许

---

### 3.2 策略评估结果

**文件位置**：`access/policy/PolicyResult.java`

- `ALLOW` — 本条规则通过，继续下一层
- `DENY` — 本条规则拒绝，立即终止
- `FORCE_ALLOW` — 强制放行，跳过所有后续层（为临时授权预留）

---

### 3.3 已实现的策略规则

#### 规则一：SecurityLevelPolicy（安全策略层）

**文件位置**：`access/policy/SecurityLevelPolicy.java`

**职责**：根据资源密级和员工职级判断是否有权访问

**规则矩阵**：

| 密级 | 最低职级要求 | 外包人员 |
|------|------------|---------|
| PUBLIC | P1（所有人）| 可访问 |
| INTERNAL | P3 | 可访问 |
| CONFIDENTIAL | P5 | **禁止** |
| TOP_SECRET | VP | **禁止** |

**特殊限制**：外包人员无论职级多高，一律不能访问机密及以上资源。

**示例**：
- P4 员工访问 CONFIDENTIAL 资源 → DENY（职级不够）
- VP 外包访问 CONFIDENTIAL 资源 → DENY（外包身份限制）
- P6 正式员工访问 CONFIDENTIAL 资源 → ALLOW

---

#### 规则二：PhaseAccessPolicy（项目策略层）

**文件位置**：`access/policy/PhaseAccessPolicy.java`

**职责**：根据项目阶段和部门类型判断是否有权访问

**规则矩阵**：

| 阶段 | 完整操作权部门 | 只读监管权 |
|------|--------------|----------|
| 立项 | 产品部、管理层 | — |
| 需求设计 | 产品部、研发部 | 管理层 |
| 研发实现 | 研发部、产品部 | 管理层 |
| 测试验证 | 测试部、研发部 | 管理层 |
| 上线交付 | 运维部、研发部 | 管理层 |
| 归档 | — | 管理层（只读）|

**特殊规则**：
- 归档阶段：只有管理层可以访问，且仅限 READ
- 管理层监管模式：非立项/归档阶段，管理层对所有阶段保有 READ 权限

**示例**：
- 研发部 READ 立项阶段项目 → DENY（不在允许列表）
- 产品部 WRITE 需求设计阶段资产 → ALLOW（在完整权限列表）
- 管理层 READ 研发阶段项目 → ALLOW（监管权）
- 管理层 DELETE 研发阶段项目 → DENY（监管权仅限 READ）

---

## 四、PDP（策略决策点）

### 4.1 核心组件

**接口**：`access/pdp/PolicyDecisionPoint.java`
**实现**：`access/pdp/AbacPolicyDecisionPoint.java`

### 4.2 工作流程

```
1. 接收请求（Subject + Resource + Action + Environment）
   ↓
2. 按层执行策略（SECURITY → PROJECT → ROLE）
   ↓
3. 每层内遍历所有规则
   ├─ 遇到 DENY → 立即终止，返回拒绝
   ├─ 遇到 FORCE_ALLOW → 跳过所有后续层，返回允许
   └─ 遇到 ALLOW → 继续下一条规则
   ↓
4. 所有层全部通过 → 返回允许
```

### 4.3 自动规则收集机制

```java
@Autowired
private List<Policy> allPolicies;  // Spring 自动注入所有 Policy Bean

@PostConstruct
public void init() {
    policyMap = allPolicies.stream()
        .collect(Collectors.groupingBy(Policy::getLayer));
}
```

**意义**：新增规则只需实现 `Policy` 接口并加 `@Component`，PDP 自动识别并按层分组，无需修改 PDP 代码。

### 4.4 决策结果

**文件位置**：`access/pdp/DecisionResult.java`

**包含信息**：
- `allowed` — 是否允许访问
- `triggerPolicy` — 触发决策的规则名称
- `reason` — 人类可读的拒绝原因

**用途**：供 PEP 执行决策，供审计日志记录详细原因。

---

## 五、数据模型变更

### 5.1 新增枚举：DeptType

**文件位置**：`model/enumValue/DeptType.java`

**枚举值**：
- `PRODUCT` — 产品部
- `RD` — 研发部
- `QA` — 测试部
- `OPS` — 运维部
- `MANAGEMENT` — 管理层

**数据库映射**：`departments.dept_type` 字段存储枚举的 `name()` 字符串（如 "RD"）

---

### 5.2 Department 模型更新

**文件位置**：`model/Department.java`

**新增字段**：`private DeptType deptType;`

---

### 5.3 Subject 模型更新

**文件位置**：`access/subject/Subject.java`

**新增字段**：`private DeptType deptType;`

**影响**：登录时需要查询员工所属部门，将 `deptType` 填入 Subject 并写入 JWT。

---

### 5.4 TypeHandler 新增

**文件位置**：`config/DeptTypeTypeHandler.java`

**职责**：MyBatis 自动将数据库的 `VARCHAR` 字段转换为 `DeptType` 枚举。

---

## 六、当前项目状态

### 6.1 已完成模块

✅ **ABAC 四要素模型**
- Subject / Resource / Action / Environment 全部定义完成

✅ **策略分层架构**
- PolicyLayer / PolicyResult / Policy 接口完成
- 支持优先级分层和 FORCE_ALLOW 机制

✅ **两条核心规则**
- SecurityLevelPolicy（密级与职级匹配）
- PhaseAccessPolicy（阶段访问控制矩阵）

✅ **PDP 决策引擎**
- AbacPolicyDecisionPoint 实现完成
- 自动规则收集机制
- DecisionResult 决策结果封装

✅ **数据模型支持**
- DeptType 枚举及 TypeHandler
- Department / Subject 模型更新
- 登录时构建完整 Subject

---

### 6.2 待实现模块

❌ **PEP（策略执行点）**
- 需要实现拦截器，在请求到达 Controller 前调用 PDP
- 需要从请求中提取 Subject / Resource / Action / Environment
- 需要根据 DecisionResult 决定放行或拒绝

❌ **Resource 构建逻辑**
- 需要在 Service 层查询项目/资产信息，构建 Resource 对象
- 需要传递给 PEP 进行决策

❌ **审计日志**
- 需要记录每次访问决策（允许/拒绝）
- 需要记录触发规则、请求参数、时间戳

❌ **PAP（策略管理点）**
- 管理员配置策略的接口（可选，当前规则硬编码）

❌ **PIP（策略信息点）**
- 动态查询额外属性（如员工信用分、项目成员关系）

❌ **临时授权机制**
- 安全策略层发出 FORCE_ALLOW 的具体实现

---

## 七、关键设计决策说明

### 7.1 为什么 Resource 不直接用 Projects 实体？

**解耦原则**：PDP 是决策引擎，不应该依赖具体的数据库模型。用 Resource 做一层抽象，以后换表结构或加新资源类型，PDP 的规则代码不需要改动。

---

### 7.2 为什么管理层在立项阶段有完整权限？

**业务语义**：立项阶段管理层是业务参与方（审批预算、签发文件），不是单纯的监管角色，所以需要 WRITE / DELETE 权限。其余阶段管理层只监管，所以只有 READ。

---

### 7.3 为什么外包人员单独限制？

**安全合规**：外包人员属于非正式员工，即使职级标记为 VP，也不应该访问机密资料。这是身份限制，优先级高于职级限制。

---

### 7.4 为什么用 PolicyResult 三值而不是 boolean？

**扩展性**：为临时授权预留 `FORCE_ALLOW`，让安全策略层可以强制放行（如紧急访问令牌），跳过项目策略的阶段限制。

---

### 7.5 为什么 PDP 用 @PostConstruct 收集规则？

**开闭原则**：新增规则只需加 `@Component`，PDP 自动识别。不需要在 PDP 里手动 `new PhaseAccessPolicy()`，符合依赖注入和开闭原则。

---

## 八、下一步工作建议

### 优先级 1：实现 PEP（必需）

没有 PEP，PDP 无法接入真实请求流程，整个 ABAC 系统无法运行。

**任务**：
1. 创建 `JwtAuthenticationFilter`（解析 token，提取 Subject）
2. 创建 `AbacAuthorizationInterceptor`（调用 PDP，执行决策）
3. 更新 `SecurityConfig`（配置白名单，加入 filter）

---

### 优先级 2：实现 Resource 构建（必需）

Controller 需要把项目/资产信息转换为 Resource 对象传给 PEP。

**任务**：
1. 在 Service 层加 `buildResource(Projects)` 方法
2. Controller 调用 Service 获取 Resource
3. 传递给 PEP 进行决策

---

### 优先级 3：审计日志（重要）

对应你文档里的"安全审计模块"，也是答辩时的重要展示点。

**任务**：
1. 创建 `access_audit_log` 表
2. 在 PDP 返回决策后记录日志
3. 提供审计日志查询接口

---

### 优先级 4：临时授权（加分项）

对应你文档里的"临时授权/例外授权机制"，体现 ABAC 的灵活性。

**任务**：
1. 创建临时授权表（授权人、被授权人、资源、有效期）
2. 在 SecurityLevelPolicy 里检查临时授权，返回 FORCE_ALLOW
3. 提供临时授权管理接口

---

## 九、文件清单

### 新增文件（本次会话）

```
access/action/Action.java
access/resource/Resource.java
access/resource/ResourceType.java
access/environment/Environment.java
access/policy/Policy.java
access/policy/PolicyResult.java
access/policy/PolicyLayer.java
access/policy/PhaseAccessPolicy.java
access/policy/SecurityLevelPolicy.java
access/pdp/PolicyDecisionPoint.java
access/pdp/DecisionResult.java
access/pdp/AbacPolicyDecisionPoint.java
model/enumValue/DeptType.java
config/DeptTypeTypeHandler.java
```

### 修改文件（本次会话）

```
model/Department.java（加 deptType 字段）
access/subject/Subject.java（加 deptType 字段）
service/impl/EmployeeAuthServiceImpl.java（构建 Subject 时填入 deptType）
```

---

## 十、测试建议

### 单元测试（可独立测试）

```java
// 测试 SecurityLevelPolicy
Subject subject = new Subject(..., EmployeeLevel.P4, ...);
Resource resource = Resource.builder()
    .securityLevel(SecurityLevel.CONFIDENTIAL)
    .build();
PolicyResult result = policy.evaluate(subject, resource, Action.READ, null);
assertEquals(PolicyResult.DENY, result); // P4 不能访问 CONFIDENTIAL
```

### 集成测试（需要 PEP 完成后）

```bash
# 研发部员工访问立项阶段项目
curl -H "Authorization: Bearer <token>" \
     http://localhost:8080/api/project/1

# 预期：403 Forbidden（PhaseAccessPolicy 拒绝）
```

---

**报告完成时间**：2026-03-09
**当前架构版本**：v1.0（PDP 完成，PEP 待实现）
