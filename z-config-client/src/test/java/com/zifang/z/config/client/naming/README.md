# 服务发现模块测试文档

## 概述

本文档描述了 `z-config` 服务发现模块的测试用例，包括测试目标、测试场景、运行方式等。

## 测试结构

```
naming/
├── ZNamingServiceHttpTest.java       # HTTP接口基础测试
├── NamingServiceIntegrationTest.java # 集成测试和性能测试
├── NamingServiceTestSuite.java       # 测试套件（汇总所有测试）
└── README.md                         # 本文档
```

## 测试分类

### 1. 基础功能测试 (ZNamingServiceHttpTest)

| 测试方法                                       | 描述        | 验证点                |
|--------------------------------------------|-----------|--------------------|
| `testBasicServiceRegistrationAndDiscovery` | 基础服务注册与发现 | 注册成功、能查询到实例、实例信息正确 |
| `testRegisterInstanceWithCluster`          | 带集群的注册    | 实例注册到指定集群、按集群查询正确  |
| `testHealthCheckAndLoadBalance`            | 健康检查与负载均衡 | 健康实例筛选、负载均衡分发      |
| `testFullServiceDiscoveryFlow`             | 完整服务发现流程  | 注册-发现-扩容-下线全流程     |
| `testClusterAndGroupIsolation`             | 集群与分组隔离   | 不同集群/分组之间隔离        |
| `testConcurrentRegistration`               | 并发注册测试    | 多线程并发注册稳定性         |

### 2. 集成与性能测试 (NamingServiceIntegrationTest)

| 测试方法                                       | 描述      | 验证点                 |
|--------------------------------------------|---------|---------------------|
| `testSmoke`                                | 冒烟测试    | 服务是否可用、基础功能是否正常     |
| `testBasicServiceRegistrationAndDiscovery` | 基础功能验证  | 单次注册-发现-注销流程        |
| `testHealthCheckAndLoadBalance`            | 健康与负载验证 | 多实例场景下的健康检查和负载均衡    |
| `testFullServiceDiscoveryFlow`             | 完整流程验证  | 模拟真实场景的服务生命周期       |
| `testConcurrentRegistration`               | 并发压力测试  | 50线程并发注册，验证系统性能和稳定性 |

## 运行测试

### 方式1：运行单个测试类

```bash
# 运行HTTP基础测试
mvn test -Dtest=ZNamingServiceHttpTest

# 运行集成测试
mvn test -Dtest=NamingServiceIntegrationTest
```

### 方式2：运行测试套件（所有测试）

```bash
mvn test -Dtest=NamingServiceTestSuite
```

### 方式3：运行单个测试方法

```bash
mvn test -Dtest=ZNamingServiceHttpTest#testBasicServiceRegistrationAndDiscovery
```

### 方式4：在IDE中运行

1. 右键点击测试类或方法
2. 选择 "Run" 或 "Debug"

## 配置说明

### 服务器地址配置

修改测试类中的以下常量：

```java
// 服务器主机地址
private static final String SERVER_HOST = "127.0.0.1";  // 本地测试
// private static final String SERVER_HOST = "101.37.80.51";  // 远程服务器

// 服务器端口
private static final int SERVER_PORT = 8084;
```

### 数据库准备

1. 确保MySQL数据库已启动
2. 执行 `z-config.sql` 初始化数据库表结构
3. 确保 `z-config-admin` 能正常连接数据库

### 服务端准备

1. 启动 `z-config-admin` 服务
2. 确认服务端口（默认8080 HTTP + 12888 Netty）正常监听
3. 可以通过访问 `http://localhost:8084/actuator/health` 检查服务健康状态

## 预期输出

测试成功时的典型输出：

```
========== 测试1：基础服务注册与发现 ==========
[步骤1] 注册服务...
注册结果: 操作成功

[步骤2] 查询服务实例...
实例信息:
  - 实例ID: 1@@192.168.1.100:8080
  - IP: 192.168.1.100
  - 端口: 8080
  - 健康状态: 健康

✅ 测试1通过：基础服务注册与发现功能正常
清理完成：服务已注销
```

## 常见问题

### 1. 连接超时

**现象**: `java.net.ConnectException: Connection refused`

**解决**:

- 检查服务器地址和端口是否正确
- 确认服务端是否已启动
- 检查防火墙设置

### 2. 注册失败

**现象**: 返回 `Result.fail("服务名不能为空")` 或其他失败消息

**解决**:

- 检查请求参数是否正确
- 检查服务端日志获取详细错误信息
- 确认数据库连接正常

### 3. 测试数据残留

**现象**: 测试后发现数据库中有残留数据

**解决**:

- 测试类中的 `finally` 块会自动清理，但异常时可能失败
- 可以手动执行清理SQL或重新初始化数据库

## 扩展测试

可以根据业务需求添加更多测试场景：

1. **持久化实例测试**: 测试 `ephemeral=false` 的持久化实例
2. **权重测试**: 测试带权重的负载均衡
3. **保护阈值测试**: 测试服务保护阈值功能
4. **元数据测试**: 测试实例元数据的存储和查询
5. **心跳检测测试**: 测试实例心跳和健康状态变更
