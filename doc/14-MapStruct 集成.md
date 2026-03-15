# 14-MapStruct 集成

## 目录

1. [概述](#概述)
2. [为什么选择 MapStruct](#为什么选择 mapstruct)
3. [项目集成](#项目集成)
4. [Mapper 接口设计](#mapper-接口设计)
5. [使用指南](#使用指南)
6. [高级特性](#高级特性)
7. [最佳实践](#最佳实践)
8. [常见问题](#常见问题)

## 概述

MapStruct 是一个高性能的 Java Bean 映射工具，通过在编译时生成类型安全的映射代码，实现对象之间的高效转换。

### 核心优势

| 特性 | MapStruct | 其他映射框架 |
|------|-----------|-------------|
| **性能** | 编译时生成代码，运行时无反射开销 | 运行时反射，性能较低 |
| **类型安全** | 编译期检查映射配置 | 运行时才能发现错误 |
| **代码可读** | 生成的代码清晰易懂 | 黑盒操作 |
| **零依赖** | 运行时不需要 MapStruct 库 | 需要运行时依赖 |
| **Lombok 集成** | 完美支持 Lombok | 可能需要额外配置 |

### 在 iBoot 项目中的应用

iBoot 项目使用 MapStruct 1.6.3 版本，主要用于：

- **DTO ↔ Entity 转换**：请求/响应 DTO 与领域实体之间的映射
- **VO 转换**：领域对象转视图对象
- **树形结构映射**：部门、菜单等树形数据的递归映射
- **List 批量转换**：自动遍历 List 进行批量映射

## 为什么选择 MapStruct

### 传统对象映射方式的问题

**1. 手动写 Getter/Setter 转换**
```java
// 繁琐且容易出错
public UserResponse toResponse(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setNickname(user.getNickname());
    // ... 几十个字段
    return response;
}
```

**2. 使用 BeanUtils 等反射工具**
```java
// 性能差，无类型检查
UserResponse response = new UserResponse();
BeanUtils.copyProperties(user, response);
```

### MapStruct 解决方案

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
}
```

编译后生成的代码：
```java
@Override
public UserResponse toResponse(User user) {
    if (user == null) {
        return null;
    }
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setNickname(user.getNickname());
    // ... 直接字段赋值，无反射
    return response;
}
```

## 项目集成

### Maven 依赖配置

```xml
<properties>
    <mapstruct.version>1.6.3</mapstruct.version>
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
</properties>

<dependencies>
    <!-- MapStruct -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${mapstruct.version}</version>
    </dependency>

    <!-- MapStruct 编译处理器 -->
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Lombok MapStruct Binding（解决与 Lombok 的兼容性） -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>${lombok-mapstruct-binding.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Lombok 注解处理器 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>25</source>
                <target>25</target>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${mapstruct.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>${lombok-mapstruct-binding.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 编译输出

编译后生成的代码位于：
```
target/generated-sources/annotations/
└── com/iboot/admin/application/mapper/
    └── system/
        └── UserMapperImpl.java    # MapStruct 生成的实现类
```

## Mapper 接口设计

### 项目结构

```
iboot-admin/src/main/java/com/iboot/admin/application/mapper/
├── system/
│   ├── UserMapper.java      # 用户映射
│   ├── DeptMapper.java      # 部门映射（支持树形）
│   ├── RoleMapper.java      # 角色映射
│   ├── MenuMapper.java      # 菜单映射（支持树形）
│   ├── PostMapper.java      # 岗位映射
│   ├── DictMapper.java      # 字典映射
│   ├── ConfigMapper.java    # 配置映射
│   └── NoticeMapper.java    # 公告映射
├── job/
│   └── JobMapper.java       # 任务映射
├── message/
│   ├── MessageMapper.java   # 消息映射
│   └── MessageTemplateMapper.java  # 消息模板映射
└── FileMapper.java          # 文件映射
```

### 基础 Mapper 示例

**UserMapper.java**
```java
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    /**
     * Entity 转 Response
     */
    @Mapping(target = "roleNames", source = "roles")
    UserResponse toResponse(User user);

    /**
     * Request 转 Entity（创建）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "avatar", defaultValue = "")
    @Mapping(target = "status", constant = "1")
    User toEntity(CreateUserRequest request);

    /**
     * Request 转 Entity（更新）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

    /**
     * List 批量转换
     */
    List<UserResponse> toResponseList(List<User> users);
}
```

### 树形结构 Mapper 示例

**DeptMapper.java**
```java
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface DeptMapper {

    /**
     * 平铺列表转换
     */
    DeptResponse toResponse(Dept dept);

    /**
     * 树形结构转换（递归映射子部门）
     */
    @Mapping(target = "children", source = "children")
    DeptTreeResponse toTreeResponse(Dept dept);

    /**
     * List 转树形结构
     */
    default List<DeptTreeResponse> toTreeResponseList(List<Dept> depts) {
        if (depts == null) {
            return Collections.emptyList();
        }
        return depts.stream()
            .map(this::toTreeResponse)
            .collect(Collectors.toList());
    }
}
```

## 使用指南

### 1. 在 Controller 中使用

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    // 构造器注入
    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<List<UserResponse>> list() {
        List<User> users = userService.findAll();
        // MapStruct 自动处理 List 遍历
        return Result.success(userMapper.toResponseList(users));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserResponse> create(@RequestBody @Validated CreateUserRequest request) {
        User user = userMapper.toEntity(request);
        userService.save(user);
        return Result.success(userMapper.toResponse(user));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserResponse> update(
        @PathVariable Long id,
        @RequestBody @Validated UpdateUserRequest request
    ) {
        User user = userService.getById(id);
        userMapper.updateEntityFromRequest(request, user);
        userService.update(user);
        return Result.success(userMapper.toResponse(user));
    }
}
```

### 2. 在 Service 中使用

```java
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    public List<UserResponse> findAll() {
        return userMapper.toResponseList(userRepository.findAll());
    }

    public UserResponse getById(Long id) {
        return userMapper.toResponse(
            userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"))
        );
    }
}
```

### 3. 树形结构处理

```java
@Service
public class DeptService {

    private final DeptRepository deptRepository;
    private final DeptMapper deptMapper;

    public List<DeptTreeResponse> listAsTree(Long parentId) {
        // 获取所有部门
        List<Dept> allDepts = deptRepository.findAll();

        // 转换为树形 Response
        List<DeptTreeResponse> treeResponses = allDepts.stream()
            .filter(dept -> parentId.equals(dept.getParentId()))
            .map(deptMapper::toTreeResponse)
            .collect(Collectors.toList());

        // 递归构建树形结构
        return buildTree(treeResponses, allDepts);
    }

    private List<DeptTreeResponse> buildTree(
        List<DeptTreeResponse> parents,
        List<Dept> allDepts
    ) {
        for (DeptTreeResponse parent : parents) {
            List<DeptTreeResponse> children = allDepts.stream()
                .filter(dept -> parent.getId().equals(dept.getParentId()))
                .map(deptMapper::toTreeResponse)
                .collect(Collectors.toList());
            parent.setChildren(children);
            // 递归构建子树
            if (!children.isEmpty()) {
                buildTree(children, allDepts);
            }
        }
        return parents;
    }
}
```

## 高级特性

### 常用注解说明

| 注解 | 描述 | 示例 |
|------|------|------|
| `@Mapping(source, target)` | 字段映射 | `@Mapping(source = "userName", target = "username")` |
| `@Mapping(ignore = true)` | 忽略字段 | `@Mapping(target = "id", ignore = true)` |
| `@Mapping(defaultValue)` | 默认值 | `@Mapping(target = "status", defaultValue = "1")` |
| `@Mapping(constant)` | 常量值 | `@Mapping(target = "type", constant = "SYSTEM")` |
| `@Mapping(expression)` | SpEL 表达式 | `@Mapping(target = "name", expression = "java(entity.getName().trim())")` |
| `@Mappings` | 多个映射 | `@Mappings({@Mapping(...), @Mapping(...)})` |
| `@Named` | 自定义映射方法 | `@Mapping(qualifiedByName = "formatDate")` |
| `@AfterMapping` | 映射后回调 | 用于补充映射逻辑 |
| `@BeforeMapping` | 映射前回调 | 用于预处理 |
| `@MappingTarget` | 目标对象参数 | 用于 update 方法 |

### 自定义映射方法

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "createTime", qualifiedByName = "formatDate")
    UserResponse toResponse(User user);

    @Named("formatDate")
    default String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Mapping(target = "roleIds", qualifiedByName = "extractRoleIds")
    UserResponse toResponse(User user);

    @Named("extractRoleIds")
    default List<Long> extractRoleIds(List<Role> roles) {
        if (roles == null) {
            return Collections.emptyList();
        }
        return roles.stream()
            .map(Role::getId)
            .collect(Collectors.toList());
    }
}
```

### 条件映射

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    // 只有当 source 不为 null 时才映射
    @Mapping(target = "email", conditionExpression = "user.getEmail() != null")
    UserResponse toResponse(User user);

    // 使用 defaultIfNull 提供默认值
    @Mapping(target = "avatar", defaultExpression = "java(\"/default/avatar.png\")")
    UserResponse toResponse(User user);
}
```

### 映射后回调

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "fullName", ignore = true)
    UserResponse toResponse(User user);

    @AfterMapping
    default void setFullName(User user, @MappingTarget UserResponse response) {
        if (user.getNickname() != null && user.getUsername() != null) {
            response.setFullName(user.getNickname() + " (" + user.getUsername() + ")");
        }
    }
}
```

## 最佳实践

### 1. 命名规范

```java
// 推荐：清晰的命名
User toEntity(CreateUserRequest request);
UserResponse toResponse(User user);
void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);

// 不推荐：模糊的命名
User convert(User user);  // 不明确
Object map(Object obj);   // 类型不安全
```

### 2. 构造器注入

```java
// 推荐：使用构造器注入
@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface UserMapper {
}

// Controller 中使用
@RestController
public class UserController {
    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
}
```

### 3. 处理 null 值

```java
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    nullValueIterableMappingStrategy = NullValueIterableMappingStrategy.IGNORE
)
public interface UserMapper {
    // null 字段自动忽略
    UserResponse toResponse(User user);
}
```

### 4. List 映射

```java
// MapStruct 自动处理 List 遍历，无需手动遍历
List<UserResponse> toResponseList(List<User> users);

// 如果 List 可能为 null，添加 null 检查
@IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
List<UserResponse> toResponseList(List<User> users);
```

### 5. 与 Lombok 协作

确保依赖顺序正确：
```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </path>
    <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
    </path>
</annotationProcessorPaths>
```

## 常见问题

### Q1: 编译错误 "No implementation found"

**问题**：编译时找不到 MapStruct 生成的实现类

**解决方案**：
1. 确保 Maven 依赖配置正确
2. 检查 `@Mapper` 注解是否添加
3. 执行 `mvn clean compile` 重新编译
4. 检查 `target/generated-sources/annotations/` 目录是否生成实现类

### Q2: 与 Lombok 冲突

**问题**：MapStruct 无法识别 Lombok 生成的 Getter/Setter

**解决方案**：
1. 添加 `lombok-mapstruct-binding` 依赖
2. 确保 annotationProcessorPaths 顺序正确
3. 使用 `@Value` 或 `@Data` 等 Lombok 注解

### Q3: 循环引用问题

**问题**：两个实体互相引用导致无限递归

**解决方案**：
```java
@Mapping(target = "order", ignore = true)  // 忽略循环引用字段
User toResponse(Order order);

@Mapping(target = "user", ignore = true)   // 忽略循环引用字段
Order toResponse(User user);
```

### Q4: 集合字段映射失败

**问题**：List/Set 字段映射后为 null

**解决方案**：
```java
// 添加 null 检查
@IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
List<RoleResponse> toResponseList(List<Role> roles);

// 或者使用 default 值
@Mapping(target = "roles", defaultValue = "[]")
UserResponse toResponse(User user);
```

### Q5: 编译生成的代码在哪里？

**答案**：
```
iboot-admin/target/generated-sources/annotations/
└── com/iboot/admin/application/mapper/
    └── system/
        └── UserMapperImpl.java
```

查看生成的代码有助于理解 MapStruct 的工作原理和调试问题。

## 相关文档

- [02-技术栈](./02-技术栈.md) - 技术栈概览
- [09-后端技术架构详解](./09-后端技术架构详解.md) - DDD 分层架构中的 Mapper 位置
- [06-API 集成](./06-API 集成.md) - DTO 设计规范

## 参考资料

- [MapStruct 官方文档](https://mapstruct.org/documentation/)
- [MapStruct GitHub](https://github.com/mapstruct/mapstruct)
- [Lombok MapStruct Binding](https://github.com/mapstruct/lombok-mapstruct-binding)
