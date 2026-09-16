package com.zifang.z.config.web.api;


import com.zifang.util.core.lang.StringUtil;
import com.zifang.util.core.meta.Result;
import com.zifang.z.config.common.model.ZNamingInstance;
import com.zifang.z.config.common.model.naming.ZNamingInstanceDeregisterRequest;
import com.zifang.z.config.common.model.naming.ZNamingInstanceRegisterRequest;
import com.zifang.z.config.common.model.naming.ZNamingSubscribeRequest;
import com.zifang.z.config.common.model.naming.ZNamingUnsubscribeRequest;
import com.zifang.z.config.core.domain.entity.ZServiceInfo;
import com.zifang.z.config.core.domain.service.IZServiceInfoService;
import com.zifang.z.config.core.service.ZNamingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 命名服务核心Controller.
 * <p>
 * API 基础路径: /api/naming
 * 所属模块: z-config-web
 * 鉴权: 通过统一网关拦截,要求登录态合法
 * 功能：提供服务注册、发现、注销、订阅/取消订阅的REST接口
 * 设计：
 * 1. 移除javax.validation注解，改为手动参数校验，保证兼容性
 * 2. 移除Swagger注解，仅保留核心业务逻辑
 * 3. 所有接口返回统一的Result对象，便于前端解析
 *
 * <p>主要端点:
 * <ul>
 *   <li>POST /registerInstance — 注册服务实例(完整参数)</li>
 *   <li>POST /registerInstance/simple — 注册服务实例(简化参数)</li>
 *   <li>POST /registerInstance/withCluster — 注册服务实例(指定集群)</li>
 *   <li>GET  /getAllInstances — 查询服务的所有实例</li>
 *   <li>GET  /selectInstances/healthy — 按健康状态查询实例</li>
 *   <li>GET  /selectOneHealthyInstance — 查询单个健康实例</li>
 *   <li>POST /deregisterInstance — 注销服务实例(完整参数)</li>
 *   <li>POST /deregisterInstance/simple — 注销服务实例(简化参数)</li>
 *   <li>POST /deregisterInstance/withCluster — 注销服务实例(指定集群)</li>
 *   <li>POST /subscribe — 消费实例订阅服务</li>
 *   <li>POST /unsubscribe — 消费实例取消订阅服务</li>
 *   <li>GET  /listServices — 获取已注册服务列表</li>
 * </ul>
 *
 * @author 开发者
 * @date 2026-02-12
 */
@Tag(name = "配置中心 - 命名服务")
@RestController
@RequestMapping("/api/naming")
public class ZNamingController {

    private final Logger log = LogManager.getLogger(this.getClass());

    /**
     * 命名服务核心业务层
     */
    @Resource
    private ZNamingService zNamingService;

    @Resource
    private IZServiceInfoService zServiceInfoService;

    // ===================== 私有工具方法：参数校验 =====================

    /**
     * 校验端口合法性
     *
     * @param port 待校验的端口号
     * @return true=合法（1-65535），false=不合法
     */
    private boolean isValidPort(Integer port) {
        return port != null && port >= 1 && port <= 65535;
    }

    /**
     * 简单校验IP地址格式（非空 + 包含小数点）
     * 说明：如需严格校验可替换为正则表达式（如^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$）
     *
     * @param ip 待校验的IP地址
     * @return true=格式合法，false=格式不合法
     */
    private boolean isValidIp(String ip) {
        return StringUtil.isNotBlank(ip) && ip.contains(".");
    }

    // ===================== 服务实例注册接口 =====================

    /**
     * 注册服务实例（完整参数）
     * 功能：支持自定义分组、集群、元数据等所有参数的实例注册
     * 请求方式：POST
     * 请求路径：/naming/registerInstance
     * 请求体：ZNamingInstanceRegisterRequest
     *
     * @param request 注册请求参数
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注册服务实例（完整参数）")
    @PostMapping("/registerInstance")
    public Result<String> registerInstance(@RequestBody ZNamingInstanceRegisterRequest request) {
        if (StringUtil.isBlank(request.getServiceName())) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(request.getIp())) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(request.getPort())) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        ZNamingInstance zNamingInstance = new ZNamingInstance();
        BeanUtils.copyProperties(request, zNamingInstance);
        String result = zNamingService.registerInstance(zNamingInstance);
        return Result.success(result);
    }

    /**
     * 注册服务实例（简化参数）
     * 功能：快速注册，使用默认分组（DEFAULT_GROUP）、默认集群（DEFAULT）
     * 请求方式：POST
     * 请求路径：/naming/registerInstance/simple
     * 请求参数：serviceName（服务名）、ip（实例IP）、port（实例端口）
     *
     * @param serviceName 服务名称
     * @param ip          实例IP地址
     * @param port        实例端口
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注册服务实例（简化参数）")
    @PostMapping("/registerInstance/simple")
    public Result<String> registerInstanceSimple(
            @RequestParam String serviceName,
            @RequestParam String ip,
            @RequestParam Integer port) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(ip)) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(port)) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        zNamingService.registerInstance(serviceName, ip, port);
        return Result.success("注册成功");
    }

    /**
     * 注册服务实例（指定集群）
     * 功能：简化参数 + 自定义集群名，分组使用默认值（DEFAULT_GROUP）
     * 请求方式：POST
     * 请求路径：/naming/registerInstance/withCluster
     * 请求参数：serviceName（服务名）、ip（实例IP）、port（实例端口）、clusterName（集群名）
     *
     * @param serviceName 服务名称
     * @param ip          实例IP地址
     * @param port        实例端口
     * @param clusterName 集群名称
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注册服务实例（指定集群）")
    @PostMapping("/registerInstance/withCluster")
    public Result<String> registerInstanceWithCluster(
            @RequestParam String serviceName,
            @RequestParam String ip,
            @RequestParam Integer port,
            @RequestParam String clusterName) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(ip)) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(port)) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        zNamingService.registerInstance(serviceName, ip, port, clusterName);
        return Result.success("注册成功");
    }

    // ===================== 服务实例查询接口 =====================

    /**
     * 查询服务所有实例
     * 功能：查询指定服务的所有提供实例（不区分健康状态）
     * 请求方式：GET
     * 请求路径：/naming/getAllInstances
     * 请求参数：serviceName（服务名）、group（分组，默认DEFAULT_GROUP）、namespace（命名空间，默认空）
     *
     * @param serviceName 服务名称
     * @param group       服务分组
     * @param namespace   命名空间
     * @return Result<List<ZNamingInstance>> 实例列表
     */
    @Operation(summary = "查询服务所有实例")
    @GetMapping("/getAllInstances")
    public Result<List<ZNamingInstance>> getAllInstances(
            @RequestParam String serviceName,
            @RequestParam(required = false, defaultValue = "DEFAULT_GROUP") String group,
            @RequestParam(required = false, defaultValue = "") String namespace) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }

        List<ZNamingInstance> instances = zNamingService.getAllInstances(serviceName);
        if (StringUtil.isNotBlank(group) || StringUtil.isNotBlank(namespace)) {
            instances = zNamingService.getAllInstances(serviceName, group, namespace);
        }
        return Result.success(instances);
    }

    /**
     * 查询服务指定健康状态的实例
     * 功能：查询指定服务的健康/不健康实例，可选集群过滤
     * 请求方式：GET
     * 请求路径：/naming/selectInstances/healthy
     * 请求参数：serviceName（服务名）、healthy（是否健康）、clusterName（集群名，可选）
     *
     * @param serviceName 服务名称
     * @param healthy     是否健康（true=健康，false=不健康）
     * @param clusterName 集群名称（可选）
     * @return Result<List<ZNamingInstance>> 符合条件的实例列表
     */
    @Operation(summary = "按健康状态查询实例")
    @GetMapping("/selectInstances/healthy")
    public Result<List<ZNamingInstance>> selectInstances(
            @RequestParam String serviceName,
            @RequestParam boolean healthy,
            @RequestParam(required = false) String clusterName) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }

        List<ZNamingInstance> instances;
        if (StringUtil.isBlank(clusterName)) {
            instances = zNamingService.selectInstances(serviceName, healthy);
        } else {
            instances = zNamingService.selectInstances(serviceName, clusterName, healthy);
        }
        return Result.success(instances);
    }

    /**
     * 查询服务单个健康实例
     * 功能：随机返回指定服务的一个健康实例（基础负载均衡能力）
     * 请求方式：GET
     * 请求路径：/naming/selectOneHealthyInstance
     * 请求参数：serviceName（服务名）
     *
     * @param serviceName 服务名称
     * @return Result<ZNamingInstance> 单个健康实例（无则返回null）
     */
    @Operation(summary = "查询单个健康实例")
    @GetMapping("/selectOneHealthyInstance")
    public Result<ZNamingInstance> selectOneHealthyInstance(
            @RequestParam String serviceName) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }

        ZNamingInstance instance = zNamingService.selectOneHealthyInstance(serviceName);
        return Result.success(instance);
    }

    // ===================== 服务实例注销接口 =====================

    /**
     * 注销服务实例（完整参数）
     * 功能：支持自定义分组、集群、命名空间的实例注销
     * 请求方式：DELETE
     * 请求路径：/naming/deregisterInstance
     * 请求体：ZNamingInstanceDeregisterRequest
     *
     * @param request 注销请求参数
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注销服务实例（完整参数）")
    @PostMapping("/deregisterInstance")
    public Result<String> deregisterInstance(@RequestBody ZNamingInstanceDeregisterRequest request) {
        // 手动参数校验
        if (StringUtil.isBlank(request.getServiceName())) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(request.getIp())) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(request.getPort())) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        ZNamingInstance zNamingInstance = new ZNamingInstance();
        BeanUtils.copyProperties(request, zNamingInstance);
        zNamingService.deregisterInstance(request.getServiceName(), zNamingInstance);
        return Result.success("注销成功");
    }

    /**
     * 注销服务实例（简化参数）
     * 功能：快速注销，使用默认分组（DEFAULT_GROUP）、默认集群（DEFAULT）
     * 请求方式：DELETE
     * 请求路径：/naming/deregisterInstance/simple
     * 请求参数：serviceName（服务名）、ip（实例IP）、port（实例端口）
     *
     * @param serviceName 服务名称
     * @param ip          实例IP地址
     * @param port        实例端口
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注销服务实例（简化参数）")
    @PostMapping("/deregisterInstance/simple")
    public Result<String> deregisterInstanceSimple(
            @RequestParam String serviceName,
            @RequestParam String ip,
            @RequestParam Integer port) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(ip)) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(port)) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        zNamingService.deregisterInstance(serviceName, ip, port);
        return Result.success("注销成功");
    }

    /**
     * 注销服务实例（指定集群）
     * 功能：简化参数 + 自定义集群名，分组使用默认值（DEFAULT_GROUP）
     * 请求方式：DELETE
     * 请求路径：/naming/deregisterInstance/withCluster
     * 请求参数：serviceName（服务名）、ip（实例IP）、port（实例端口）、clusterName（集群名）
     *
     * @param serviceName 服务名称
     * @param ip          实例IP地址
     * @param port        实例端口
     * @param clusterName 集群名称
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "注销服务实例（指定集群）")
    @PostMapping("/deregisterInstance/withCluster")
    public Result<String> deregisterInstanceWithCluster(
            @RequestParam String serviceName,
            @RequestParam String ip,
            @RequestParam Integer port,
            @RequestParam String clusterName) {
        // 手动参数校验
        if (StringUtil.isBlank(serviceName)) {
            return Result.fail("服务名不能为空");
        }
        if (!isValidIp(ip)) {
            return Result.fail("实例IP不能为空且格式需合法（如192.168.1.100）");
        }
        if (!isValidPort(port)) {
            return Result.fail("实例端口不能为空且需在1-65535之间");
        }

        zNamingService.deregisterInstance(serviceName, ip, port, clusterName);
        return Result.success("注销成功");
    }

    // ===================== 消费实例订阅/取消订阅接口 =====================

    /**
     * 消费实例订阅服务
     * 功能：关联消费实例与被订阅服务，支持纯消费/双角色实例
     * 请求方式：POST
     * 请求路径：/naming/subscribe
     * 请求体：ZNamingSubscribeRequest
     *
     * @param request 订阅请求参数
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "消费实例订阅服务")
    @PostMapping("/subscribe")
    public Result<String> subscribeService(@RequestBody ZNamingSubscribeRequest request) {
        // 手动参数校验
        if (!isValidIp(request.getConsumerIp())) {
            return Result.fail("消费实例IP不能为空且格式需合法（如192.168.1.200）");
        }
        if (!isValidPort(request.getConsumerPort())) {
            return Result.fail("消费实例端口不能为空且需在1-65535之间");
        }
        if (StringUtil.isBlank(request.getSubscribeServiceName())) {
            return Result.fail("订阅的服务名不能为空");
        }

        // 构建消费实例对象
        ZNamingInstance consumerInstance = new ZNamingInstance();
        consumerInstance.setServiceName(request.getConsumerServiceName());
        consumerInstance.setGroup(request.getConsumerGroup());
        consumerInstance.setNamespace(request.getConsumerNamespace());
        consumerInstance.setIp(request.getConsumerIp());
        consumerInstance.setPort(request.getConsumerPort());
        consumerInstance.setClusterName(request.getSubscribeCluster());
        consumerInstance.setMetadata(request.getMetadata());

        // 调用订阅方法
        String result = zNamingService.subscribeService(
                consumerInstance,
                request.getSubscribeServiceName(),
                request.getSubscribeGroup(),
                request.getSubscribeNamespace()
        );
        return Result.success(result);
    }

    /**
     * 消费实例取消订阅服务
     * 功能：解除消费实例与被订阅服务的关联
     * 请求方式：POST
     * 请求路径：/naming/unsubscribe
     * 请求体：ZNamingUnsubscribeRequest
     *
     * @param request 取消订阅请求参数
     * @return Result<String> 操作结果（成功/失败提示）
     */
    @Operation(summary = "消费实例取消订阅服务")
    @PostMapping("/unsubscribe")
    public Result<String> unsubscribeService(@RequestBody ZNamingUnsubscribeRequest request) {
        // 手动参数校验
        if (!isValidIp(request.getConsumerIp())) {
            return Result.fail("消费实例IP不能为空且格式需合法（如192.168.1.200）");
        }
        if (!isValidPort(request.getConsumerPort())) {
            return Result.fail("消费实例端口不能为空且需在1-65535之间");
        }
        if (StringUtil.isBlank(request.getSubscribeServiceName())) {
            return Result.fail("订阅的服务名不能为空");
        }

        // 构建消费实例对象
        ZNamingInstance consumerInstance = new ZNamingInstance();
        consumerInstance.setServiceName(request.getConsumerServiceName());
        consumerInstance.setGroup(request.getConsumerGroup());
        consumerInstance.setNamespace(request.getConsumerNamespace());
        consumerInstance.setIp(request.getConsumerIp());
        consumerInstance.setPort(request.getConsumerPort());

        // 调用取消订阅方法
        zNamingService.unsubscribeService(
                consumerInstance,
                request.getSubscribeServiceName(),
                request.getSubscribeGroup(),
                request.getSubscribeNamespace()
        );
        return Result.success("取消订阅成功");
    }

    // ===================== 服务列表查询接口 =====================

    /**
     * 获取服务列表
     * 功能：查询所有已注册的服务列表
     * 请求方式：GET
     * 请求路径：/naming/listServices
     *
     * @return Result<List<Map<String, Object>>> 服务列表
     */
    @Operation(summary = "获取已注册服务列表")
    @GetMapping("/listServices")
    public Result<List<Map<String, Object>>> listServices() {
        // 从服务信息表中查询所有服务
        List<ZServiceInfo> serviceList = zServiceInfoService.list();

        if (CollectionUtils.isEmpty(serviceList)) {
            return Result.success(new ArrayList<>());
        }

        // 转换为前端需要的数据格式
        List<Map<String, Object>> result = serviceList.stream().map(service -> {
            Map<String, Object> map = new HashMap<>();

            // 解析服务名（格式：group@@serviceName）
            String serviceName = service.getServiceName();
            String group = "DEFAULT_GROUP";
            String name = serviceName;
            if (serviceName != null && serviceName.contains("@@")) {
                String[] parts = serviceName.split("@@");
                group = parts[0];
                name = parts[1];
            }

            map.put("serviceName", name);
            map.put("group", group);
            map.put("namespace", service.getNamespace() != null ? service.getNamespace() : "public");
            map.put("clusters", service.getClusterMap());

            // 查询该服务下的实例数量
            int totalCount = 0;
            int healthyCount = 0;

            // 从实例表中统计
            try {
                // 获取所有实例进行过滤
                List<ZNamingInstance> instances = zNamingService.getAllInstances(serviceName);
                if (instances != null) {
                    totalCount = instances.size();
                    healthyCount = (int) instances.stream().filter(ZNamingInstance::getHealthy).count();
                }
            } catch (Exception e) {
                log.warn("获取服务实例统计失败: {}", serviceName, e);
            }

            map.put("healthyInstanceCount", healthyCount);
            map.put("totalInstanceCount", totalCount);
            map.put("status", totalCount > 0 && healthyCount == totalCount ? "健康" : "异常");

            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }
}