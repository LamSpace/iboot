# JavaDoc 规范

本文是 iBoot 后端 Java 代码的注释规范。`CLAUDE.md` 只保留硬性规则摘要，具体写法、正反例和评审清单以本文为准。

## 1. 目标

让读者在不阅读实现的情况下，能够准确回答三个问题：

1. 这个类型/方法/字段在系统中承担什么职责？
2. 调用它需要满足什么前置条件？会产生什么副作用？
3. 失败时会以什么形式表现？

注释描述**契约**，不描述逐行实现。不要用注释替代清晰的命名和代码结构。

## 2. 适用范围

- 适用于 `iboot-admin` 下新增或修改的 Java 代码。
- 存量代码的批量重写按独立任务执行，不在日常改动中顺手进行。
- 注释语言：说明文字使用中文；`@param`、`@return`、`@throws` 等标签名保持英文。
- 中文与英文单词、数字之间保留一个空格，例如“根据用户 ID 查询”“返回 10 条记录”。
- 使用中文标点；`{@code ...}` 内部的代码保持原样。

## 3. 类级注释

每个 `class`、`interface`、`enum`、`record`、`@interface` 都必须有类级 Javadoc。

### 3.1 模板

```java
/**
 * 一句话摘要，以句号结尾。
 *
 * <p>职责与边界：这个类负责什么、不负责什么。
 *
 * <p>关键约束：事务边界、线程安全、缓存、异步、幂等性、权限要求等。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see RelatedType
 */
```

### 3.2 要求

- 第一句是独立摘要，能单独出现在 Javadoc 索引页，以句号结尾。
- 摘要后空一行，再用 `<p>` 分段。不要用裸空行分段。
- 至少说明：**职责**、**在分层中的位置**、**关键协作对象**。
- 有事务、线程安全、缓存、异步、权限、幂等性等约束时，必须写明。
- `@author` 统一为 `iBoot Team`，`@since` 统一为 `1.0.0`。
- 只在确有关联时添加 `@see`，不要堆砌链接。

### 3.3 示例

```java
/**
 * 用户应用服务。
 *
 * <p>负责用户聚合根的用例编排：创建、查询、更新、状态切换、密码修改与重置，
 * 以及用户与角色、岗位、部门的关联维护。本类不直接访问数据库，
 * 持久化操作统一通过 {@link UserRepository} 完成。
 *
 * <p>所有写操作在 {@link org.springframework.transaction.annotation.Transactional}
 * 事务中执行；用户密码在写入前由 {@link PasswordEncoder} 加密。
 *
 * @author iBoot Team
 * @since 1.0.0
 * @see UserRepository
 * @see com.iboot.admin.interfaces.controller.UserController
 */
```

反例：

```java
/**
 * 用户应用服务
 *
 * @author iBoot
 */
```

问题：没有说明职责边界、协作对象和事务约束；`@author` 与项目约定不一致。

## 4. 方法级注释

所有手写的 `public` / `protected` 方法（含接口方法）都必须有 Javadoc。

### 4.1 模板

```java
/**
 * 一句话摘要，以句号结尾。
 *
 * <p>行为细节、前置条件、副作用；复杂逻辑再分段说明。
 *
 * @param name 语义与约束，而不是复述参数名
 * @return 返回值语义，包括 null、空集合、分页边界的含义
 * @throws XxxException 抛出条件
 */
```

### 4.2 要求

- 第一句是独立摘要，说明“做什么”，不要以“该方法用于……”开头。
- `<p>` 段落说明前置条件、副作用、事务、缓存、审计、异步等行为。
- `@param` 必须写语义和约束（取值范围、是否可为 `null`、单位、默认值），禁止同义反复。
- `@return` 必须写返回值语义；返回集合时说明是否可能为空；查询不到时说明返回 `null` 还是抛异常。
- `@throws` 只写调用方需要处理的异常，并说明触发条件；不要罗列所有可能的运行时异常。
- 重写父类/接口方法且行为完全一致时，可以用 `{@inheritDoc}`，但新增语义仍需补充。
- 简单 getter/setter、`equals`、`hashCode`、`toString` 不写 Javadoc。
- 简单构造器不写 Javadoc；有校验、副作用或非默认语义的构造器（例如设置默认状态码、
  注册静态引用）应说明其行为。

### 4.3 示例

```java
/**
 * 创建新用户。
 *
 * <p>依次校验用户名、邮箱、手机号的唯一性；若存在同名逻辑删除记录，先清理再创建。
 * 密码在持久化前使用 {@link PasswordEncoder} 加密。任一唯一性校验失败时事务回滚。
 *
 * @param user 待创建的用户，用户名与密码不能为 {@code null}
 * @return 已持久化的用户，包含数据库生成的主键
 * @throws BusinessException 用户名、邮箱或手机号已存在时抛出
 */
```

反例：

```java
/**
 * 创建新用户
 *
 * @param user 用户实体
 * @return 创建后的用户实体
 */
```

问题：没有说明唯一性校验、逻辑删除清理、密码加密和异常条件；参数描述没有提供额外信息。

## 5. 字段级注释

以下字段必须有 Javadoc：

- DTO 的所有字段（`request`、`response`、`export`）。
- 枚举的所有常量。
- 非显而易见的私有字段：状态位、缓存、超时、重试、并发控制、单位不明显的数值等。
- 配置类中与配置项对应的字段。

简单且自解释的私有字段可以只写一行；不要为局部变量写 Javadoc。

### 5.1 示例

```java
/**
 * 用户 ID，数据库主键。
 */
private Long id;

/**
 * 状态：0-停用，1-启用。
 */
private Integer status;

/**
 * 令牌有效期，单位秒。
 */
private long expireSeconds;
```

DTO 字段同时保留 OpenAPI 注解：

```java
/**
 * 用户名，登录时使用，长度 3～20 个字符。
 */
@Schema(description = "用户名")
@NotBlank(message = "用户名不能为空")
@Size(min = 3, max = 20, message = "用户名长度必须在 3 到 20 个字符之间")
private String username;
```

Javadoc 面向 IDE 和源码读者，`@Schema` 面向 OpenAPI 文档使用者；两者都保留，内容不要互相矛盾。

## 6. 生成成员排除规则

以下成员由 delombok 生成，**不写 Javadoc**：

- `getXxx` / `setXxx`
- `equals` / `hashCode` / `toString` / `canEqual`
- 由 `@RequiredArgsConstructor` / `@AllArgsConstructor` / `@NoArgsConstructor` 生成的构造器
- 由 `@Builder` 生成的 builder 方法

识别特征：成员前有 `@SuppressWarnings("all")`。批量改写时不要修改这些成员，也不要在它们上方插入注释。

字段本身的注释会通过 IDE 和生成方法继承展示，不需要重复。

## 7. 深度预算

不要给每个类都写 `java.util.concurrent.locks.Lock` 那样的长篇文档。按类型控制篇幅：

| 类型 | 类注释 | 方法注释 | 字段注释 |
|------|--------|----------|----------|
| 核心领域、安全、并发、应用服务 | 8～20 行 | 3～10 行 | 1～3 行 |
| Controller、Mapper、Repository | 4～10 行 | 2～5 行 | 1～2 行 |
| DTO、PO、Config、Enum | 2～6 行 | 生成成员不写 | 1～3 行 |

判断标准：注释是否提供了命名和类型之外的信息。如果删掉注释不会损失任何语义，就不要写。

## 8. 标签与 HTML

| 写法 | 用途 |
|------|------|
| `{@code text}` | 类型、关键字、字面量、`null`、短代码片段 |
| `{@link Type}` | 关联类型、方法、字段 |
| `{@linkplain Type 文本}` | 需要自定义显示文本的链接 |
| `<p>` | 段落分隔 |
| `<ul>` / `<li>` | 列表 |
| `<pre>{@code ...}</pre>` | 多行示例 |
| `@param` | 参数语义与约束 |
| `@return` | 返回值语义 |
| `@throws` | 异常触发条件 |
| `@see` | 相关类型或方法 |
| `@since` | 引入版本，统一 `1.0.0` |
| `@author` | 统一 `iBoot Team` |
| `@deprecated` | 必须同时给出替代方案 |

注意：

- Javadoc 中的 `<`、`>` 必须转义为 `&lt;`、`&gt;`，或放进 `{@code ...}`。
- 不要使用 `<br>` 模拟分段，用 `<p>`。
- 泛型参数用 `@param <T> 类型参数的语义`。
- 不要写 `@version`、变更历史、个人姓名或日期。

## 9. 常见反例

```java
// 反例 1：同义反复
/** 获取用户名 */
public String getUsername() { ... }

// 反例 2：只重复类型
/** 用户 ID */
private Long id;   // 可以接受，但补充“数据库主键”更好

// 反例 3：注释与代码不一致
/** 返回 null 表示用户不存在 */
public User getUserById(Long id) {
    return userRepository.findById(id).orElseThrow(...);
}

// 反例 4：未转义泛型
/** 返回 List<String> */
public List<String> list() { ... }   // 应写作 {@code List<String>}

// 反例 5：给生成成员写注释
/** 获取 ID */
@SuppressWarnings("all")
public Long getId() { ... }
```

## 10. 验证与评审

提交前执行：

```bash
mvn -s /home/lam/repo/settings.xml -q -DskipTests compile
mvn -s /home/lam/repo/settings.xml javadoc:javadoc
```

评审清单：

- [ ] 每个类型都有类级 Javadoc，且第一句可独立阅读。
- [ ] 每个手写 public/protected 方法都有方法级 Javadoc。
- [ ] `@param` / `@return` / `@throws` 描述的是语义和约束，不是参数名的复述。
- [ ] DTO 字段、枚举常量、非显而易见私有字段都有字段注释。
- [ ] 生成成员没有被添加注释或改写。
- [ ] 没有未转义的 `<` / `>`，没有 `@return` 用在 `void` 方法上。
- [ ] `git diff` 中只有注释行变化。
