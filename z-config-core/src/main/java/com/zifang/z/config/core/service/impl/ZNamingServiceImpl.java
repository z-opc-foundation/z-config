package com.zifang.z.config.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zifang.util.core.lang.collection.Lists;
import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;
import com.zifang.z.config.common.model.ZNamingInstance;
import com.zifang.z.config.core.domain.entity.ZCluster;
import com.zifang.z.config.core.domain.entity.ZInstance;
import com.zifang.z.config.core.domain.entity.ZServiceInfo;
import com.zifang.z.config.core.domain.entity.ZSubscription;
import com.zifang.z.config.core.domain.service.IZClusterService;
import com.zifang.z.config.core.domain.service.IZInstanceService;
import com.zifang.z.config.core.domain.service.IZServiceInfoService;
import com.zifang.z.config.core.domain.service.IZSubscriptionService;
import com.zifang.z.config.core.service.ZNamingService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.zifang.util.core.json.JsonMapperFactory;

@Service
public class ZNamingServiceImpl implements ZNamingService {
    // 工具：ObjectMapper（JSON序列化/反序列化）
    private static final ObjectMapper OBJECT_MAPPER = JsonMapperFactory.getDefault();
    private final Logger log = LogManager.getLogger(getClass());
    @Resource
    private IZServiceInfoService zServiceInfoService;
    @Resource
    private IZClusterService zClusterService;
    @Resource
    private IZInstanceService zInstanceService;
    // 新增：消费实例订阅表Service
    @Resource
    private IZSubscriptionService zSubscriptionService;

    // ===================== 核心工具方法 =====================

    /**
     * 生成实例唯一ID（格式：serviceId@@ip:port）
     */
    private String generateInstanceId(Long serviceId, String ip, Integer port) {
        return String.format("%s@@%s:%d", serviceId, ip, port);
    }

    /**
     * 生成消费实例ID（纯消费：unknown@@ip:port；双角色：复用instance_id）
     */
    private String generateConsumerInstanceId(ZNamingInstance consumerInstance) {
        // 1. 先判断是否是双角色（有对应的提供实例记录）
        if (StringUtils.isNotBlank(consumerInstance.getServiceName())) {
            String fullServiceName = consumerInstance.getGroup() + "@@" + consumerInstance.getServiceName();
            ZServiceInfo serviceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                    .eq(ZServiceInfo::getServiceName, fullServiceName)
                    .eq(ZServiceInfo::getNamespace, consumerInstance.getNamespace()));
            if (serviceInfo != null) {
                return generateInstanceId(serviceInfo.getId(), consumerInstance.getIp(), consumerInstance.getPort());
            }
        }
        // 2. 纯消费实例：unknown@@ip:port
        return String.format("unknown@@%s:%d", consumerInstance.getIp(), consumerInstance.getPort());
    }

    /**
     * 转换ZInstance → ZNamingInstance
     */
    private ZNamingInstance convertToNamingInstance(ZInstance zInstance, ZServiceInfo zServiceInfo) {
        ZNamingInstance instance = new ZNamingInstance();
        // 拆分服务名（group@@name → group + name）
        String[] serviceNameParts = zServiceInfo.getServiceName().split("@@");
        instance.setGroup(serviceNameParts[0]);
        instance.setServiceName(serviceNameParts[1]);
        instance.setNamespace(zServiceInfo.getNamespace());
        instance.setIp(zInstance.getIp());
        instance.setPort(zInstance.getPort());
        instance.setWeight(zInstance.getWeight());
        instance.setHealthy(zInstance.getHealthy());
        instance.setEnabled(zInstance.getEnabled());
        instance.setEphemeral(zInstance.getEphemeral());
        instance.setClusterName(zInstance.getClusterName());
        // 反序列化元数据
        if (StringUtils.isNotBlank(zInstance.getMetadata())) {
            try {
                instance.setMetadata(OBJECT_MAPPER.readValue(zInstance.getMetadata(), Map.class));
            } catch (JsonProcessingException e) {
                log.warn("解析实例元数据失败：{}", zInstance.getMetadata(), e);
                instance.setMetadata(new HashMap<>());
            }
        }
        // 填充instance_id
        instance.setInstanceId(zInstance.getInstanceId());
        return instance;
    }

    // ===================== 实现注册实例方法（重载） =====================
    @Override
    public void registerInstance(String serviceName, String ip, int port) {
        ZNamingInstance instance = new ZNamingInstance();
        instance.setServiceName(serviceName);
        instance.setGroup("DEFAULT_GROUP"); // 默认分组
        instance.setNamespace(""); // 默认命名空间
        instance.setIp(ip);
        instance.setPort(port);
        instance.setClusterName("DEFAULT"); // 默认集群
        instance.setWeight(1.0);
        instance.setHealthy(true);
        instance.setEnabled(true);
        instance.setEphemeral(true);
        registerInstance(instance);
    }

    @Override
    public void registerInstance(String serviceName, String ip, int port, String clusterName) {
        ZNamingInstance instance = new ZNamingInstance();
        instance.setServiceName(serviceName);
        instance.setGroup("DEFAULT_GROUP");
        instance.setNamespace("");
        instance.setIp(ip);
        instance.setPort(port);
        instance.setClusterName(StringUtils.isBlank(clusterName) ? "DEFAULT" : clusterName);
        instance.setWeight(1.0);
        instance.setHealthy(true);
        instance.setEnabled(true);
        instance.setEphemeral(true);
        registerInstance(instance);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerInstance(ZNamingInstance namingInstance) {
        // ========== 步骤1：参数校验 ==========
        if (StringUtils.isBlank(namingInstance.getServiceName())
                || StringUtils.isBlank(namingInstance.getIp())
                || namingInstance.getPort() == null) {
            log.error("注册实例失败：服务名/IP/端口不能为空");
            return "注册失败";
        }

        // ========== 步骤2：拼接Nacos标准服务名（group@@serviceName） ==========
        String group = StringUtils.defaultIfBlank(namingInstance.getGroup(), "DEFAULT_GROUP");
        String namespace = StringUtils.defaultIfBlank(namingInstance.getNamespace(), "");
        String fullServiceName = group + "@@" + namingInstance.getServiceName();
        String clusterName = StringUtils.defaultIfBlank(namingInstance.getClusterName(), "DEFAULT");
        String ipPort = namingInstance.getIp() + ":" + namingInstance.getPort();

        // ========== 步骤3：处理z_service_info表（新增/更新） ==========
        ZServiceInfo serviceInfo = getOrCreateServiceInfo(fullServiceName, namingInstance, ipPort);
        // 更新cluster_map：追加当前实例的IP:端口到对应集群
        updateServiceClusterMap(serviceInfo, clusterName, ipPort);

        // ========== 步骤4：处理z_cluster表（新增/更新） ==========
        Long serviceId = serviceInfo.getId();
        getOrCreateCluster(serviceId, clusterName);

        // ========== 步骤5：处理z_instance表（插入/更新实例） ==========
        insertOrUpdateInstance(serviceId, namingInstance);

        log.info("实例注册成功：serviceName={}, ipPort={}, cluster={}, namespace={}",
                fullServiceName, ipPort, clusterName, namespace);
        return "操作成功";
    }

    // ===================== 实现查询实例方法 =====================
    @Override
    public List<ZNamingInstance> getAllInstances(String serviceName) {
        return getAllInstances(serviceName, "DEFAULT_GROUP", "");
    }

    /**
     * 重载：按分组/命名空间查询所有提供实例
     */
    @Override
    public List<ZNamingInstance> getAllInstances(String serviceName, String group, String namespace) {
        // 1. 查询服务ID
        String fullServiceName = group + "@@" + serviceName;
        ZServiceInfo serviceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullServiceName)
                .eq(ZServiceInfo::getNamespace, namespace));
        if (serviceInfo == null) {
            log.warn("未找到服务：{}", fullServiceName);
            return Collections.emptyList();
        }

        // 2. 查询该服务下所有实例
        List<ZInstance> zInstances = zInstanceService.list(new LambdaQueryWrapper<ZInstance>()
                .eq(ZInstance::getServiceId, serviceInfo.getId()));
        if (CollectionUtils.isEmpty(zInstances)) {
            return Collections.emptyList();
        }

        // 3. 转换为ZNamingInstance
        return zInstances.stream()
                .map(zInstance -> convertToNamingInstance(zInstance, serviceInfo))
                .collect(Collectors.toList());
    }

    @Override
    public List<ZNamingInstance> selectInstances(String serviceName, boolean healthy) {
        return selectInstances(serviceName, "DEFAULT_GROUP", "", "DEFAULT", healthy);
    }

    @Override
    public List<ZNamingInstance> selectInstances(String serviceName, String clusterName, boolean healthy) {
        return selectInstances(serviceName, "DEFAULT_GROUP", "", clusterName, healthy);
    }

    /**
     * 重载：按服务名/分组/命名空间/集群/健康状态筛选提供实例
     */
    private List<ZNamingInstance> selectInstances(String serviceName, String group, String namespace, String clusterName, boolean healthy) {
        // 1. 查询服务ID
        String fullServiceName = group + "@@" + serviceName;
        ZServiceInfo serviceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullServiceName)
                .eq(ZServiceInfo::getNamespace, namespace));
        if (serviceInfo == null) {
            log.warn("未找到服务：{}", fullServiceName);
            return Collections.emptyList();
        }

        // 2. 构建实例查询条件
        LambdaQueryWrapper<ZInstance> instanceWrapper = new LambdaQueryWrapper<ZInstance>()
                .eq(ZInstance::getServiceId, serviceInfo.getId())
                .eq(ZInstance::getHealthy, healthy)
                .eq(ZInstance::getEnabled, true); // 只查启用的实例
        if (StringUtils.isNotBlank(clusterName)) {
            instanceWrapper.eq(ZInstance::getClusterName, clusterName);
        }

        // 3. 查询并转换
        List<ZInstance> zInstances = zInstanceService.list(instanceWrapper);
        if (CollectionUtils.isEmpty(zInstances)) {
            return Collections.emptyList();
        }
        return zInstances.stream()
                .map(zInstance -> convertToNamingInstance(zInstance, serviceInfo))
                .collect(Collectors.toList());
    }

    @Override
    public ZNamingInstance selectOneHealthyInstance(String serviceName) {
        List<ZNamingInstance> healthyInstances = selectInstances(serviceName, true);
        if (CollectionUtils.isEmpty(healthyInstances)) {
            log.warn("未找到{}的健康实例", serviceName);
            return null;
        }
        // 随机选一个（生产环境可按权重）
        return healthyInstances.get(new Random().nextInt(healthyInstances.size()));
    }

    // ===================== 实现注销实例方法（重载） =====================
    @Override
    public void deregisterInstance(String serviceName, String ip, int port) {
        deregisterInstance(serviceName, ip, port, "DEFAULT_GROUP", "", "DEFAULT");
    }

    @Override
    public void deregisterInstance(String serviceName, String ip, int port, String clusterName) {
        deregisterInstance(serviceName, ip, port, "DEFAULT_GROUP", "", clusterName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deregisterInstance(String serviceName, ZNamingInstance instance) {
        deregisterInstance(
                instance.getServiceName(),
                instance.getIp(),
                instance.getPort(),
                instance.getGroup(),
                instance.getNamespace(),
                instance.getClusterName()
        );
    }

    /**
     * 核心注销逻辑：删除实例 + 更新cluster_map + 清理空集群
     */
    private void deregisterInstance(String serviceName, String ip, int port, String group, String namespace, String clusterName) {
        // 1. 查询服务ID
        String fullServiceName = group + "@@" + serviceName;
        ZServiceInfo serviceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullServiceName)
                .eq(ZServiceInfo::getNamespace, namespace));
        if (serviceInfo == null) {
            log.warn("注销实例失败：服务{}不存在", fullServiceName);
            return;
        }

        // 2. 删除实例记录
        String instanceId = generateInstanceId(serviceInfo.getId(), ip, port);
        boolean deleteSuccess = zInstanceService.remove(new LambdaQueryWrapper<ZInstance>()
                .eq(ZInstance::getServiceId, serviceInfo.getId())
                .eq(ZInstance::getInstanceId, instanceId));
        if (!deleteSuccess) {
            log.warn("注销实例失败：实例{}不存在", instanceId);
            return;
        }

        // 3. 更新service_info的cluster_map（移除该实例）
        updateServiceClusterMapRemoveInstance(serviceInfo, clusterName, ip + ":" + port);

        // 4. 清理空集群（可选）
        cleanEmptyCluster(serviceInfo.getId(), clusterName);

        log.info("实例注销成功：serviceName={}, ipPort={}", fullServiceName, ip + ":" + port);
    }

    // ===================== 消费实例订阅/取消订阅（扩展方法，贴合z_subscription表） =====================

    /**
     * 消费实例订阅服务（核心：区分纯消费/双角色）
     */
    public String subscribeService(ZNamingInstance consumerInstance, String subscribeServiceName, String subscribeGroup, String subscribeNamespace) {
        // 1. 参数校验
        if (StringUtils.isBlank(consumerInstance.getIp()) || consumerInstance.getPort() == null
                || StringUtils.isBlank(subscribeServiceName)) {
            log.error("订阅失败：消费实例IP/端口或订阅服务名不能为空");
            return "订阅失败";
        }

        // 2. 生成消费实例ID（区分纯消费/双角色）
        String consumerInstanceId = generateConsumerInstanceId(consumerInstance);

        // 3. 查询被订阅服务的ID
        String fullSubscribeServiceName = subscribeGroup + "@@" + subscribeServiceName;
        ZServiceInfo subscribeServiceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullSubscribeServiceName)
                .eq(ZServiceInfo::getNamespace, subscribeNamespace));
        if (subscribeServiceInfo == null) {
            log.error("订阅失败：被订阅服务{}不存在", fullSubscribeServiceName);
            return "订阅失败";
        }

        // 4. 插入/更新订阅记录
        ZSubscription subscription = new ZSubscription();
        subscription.setConsumerInstanceId(consumerInstanceId);
        subscription.setConsumerIp(consumerInstance.getIp());
        subscription.setConsumerPort(consumerInstance.getPort());
        subscription.setSubscribeServiceId(subscribeServiceInfo.getId());
        subscription.setSubscribeNamespace(subscribeNamespace);
        subscription.setSubscribeCluster(StringUtils.defaultIfBlank(consumerInstance.getClusterName(), "DEFAULT"));
        subscription.setSubscribeTime(LocalDateTime.now());
        subscription.setStatus(true); // 有效订阅
        subscription.setGmtCreate(LocalDateTime.now());
        subscription.setGmtModified(LocalDateTime.now());

        // 反序列化消费端元数据
        if (consumerInstance.getMetadata() != null) {
            try {
                subscription.setMetadata(OBJECT_MAPPER.writeValueAsString(consumerInstance.getMetadata()));
            } catch (JsonProcessingException e) {
                log.warn("序列化消费端元数据失败", e);
            }
        }

        // 唯一键：consumer_instance_id + subscribe_service_id，存在则更新
        LambdaQueryWrapper<ZSubscription> subWrapper = new LambdaQueryWrapper<ZSubscription>()
                .eq(ZSubscription::getConsumerInstanceId, consumerInstanceId)
                .eq(ZSubscription::getSubscribeServiceId, subscribeServiceInfo.getId());
        ZSubscription existSub = zSubscriptionService.getOne(subWrapper);
        if (existSub != null) {
            subscription.setId(existSub.getId());
            subscription.setSubscribeTime(existSub.getSubscribeTime()); // 保留首次订阅时间
            zSubscriptionService.updateById(subscription);
        } else {
            zSubscriptionService.save(subscription);
        }

        log.info("订阅成功：消费实例{} 订阅服务{}", consumerInstanceId, fullSubscribeServiceName);
        return "订阅成功";
    }

    /**
     * 消费实例取消订阅服务
     */
    @Override
    public void unsubscribeService(ZNamingInstance consumerInstance, String subscribeServiceName, String subscribeGroup, String subscribeNamespace) {
        // 1. 生成消费实例ID
        String consumerInstanceId = generateConsumerInstanceId(consumerInstance);

        // 2. 查询被订阅服务ID
        String fullSubscribeServiceName = subscribeGroup + "@@" + subscribeServiceName;
        ZServiceInfo subscribeServiceInfo = zServiceInfoService.getOne(new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullSubscribeServiceName)
                .eq(ZServiceInfo::getNamespace, subscribeNamespace));
        if (subscribeServiceInfo == null) {
            log.warn("取消订阅失败：被订阅服务{}不存在", fullSubscribeServiceName);
            return;
        }

        // 3. 更新订阅状态为取消
        LambdaQueryWrapper<ZSubscription> subWrapper = new LambdaQueryWrapper<ZSubscription>()
                .eq(ZSubscription::getConsumerInstanceId, consumerInstanceId)
                .eq(ZSubscription::getSubscribeServiceId, subscribeServiceInfo.getId());
        ZSubscription subscription = zSubscriptionService.getOne(subWrapper);
        if (subscription == null) {
            log.warn("取消订阅失败：订阅关系不存在");
            return;
        }

        subscription.setStatus(false);
        subscription.setUnsubscribeTime(LocalDateTime.now());
        subscription.setGmtModified(LocalDateTime.now());
        zSubscriptionService.updateById(subscription);

        log.info("取消订阅成功：消费实例{} 取消订阅服务{}", consumerInstanceId, fullSubscribeServiceName);
    }

    // ===================== 私有辅助方法（补充/完善） =====================
    private ZServiceInfo getOrCreateServiceInfo(String fullServiceName, ZNamingInstance namingInstance, String ipPort) {
        LambdaQueryWrapper<ZServiceInfo> serviceWrapper = new LambdaQueryWrapper<ZServiceInfo>()
                .eq(ZServiceInfo::getServiceName, fullServiceName)
                .eq(ZServiceInfo::getNamespace, namingInstance.getNamespace());
        ZServiceInfo existService = zServiceInfoService.getOne(serviceWrapper);

        if (existService == null) {
            ZServiceInfo newService = new ZServiceInfo();
            newService.setServiceName(fullServiceName);
            newService.setGroup(namingInstance.getGroup());
            newService.setNamespace(namingInstance.getNamespace());
            // 初始化cluster_map
            Map<String, List<String>> clusterMap = new HashMap<>();
            clusterMap.put(namingInstance.getClusterName(), Lists.of(ipPort));
            newService.setClusterMap(JsonUtil.toJson(clusterMap));
            newService.setCacheMillis(10000);
            newService.setGmtCreate(new Date());
            newService.setGmtModified(new Date());
            zServiceInfoService.save(newService);
            return newService;
        }
        return existService;
    }

    private void updateServiceClusterMap(ZServiceInfo serviceInfo, String clusterName, String ipPort) {
        String oldClusterMap = serviceInfo.getClusterMap();
        Map<String, List<String>> clusterMap = StringUtils.isBlank(oldClusterMap)
                ? new HashMap<>()
                : JsonUtil.fromJson(oldClusterMap, new TypeReference<Map<String, List<String>>>() {
        });

        List<String> instanceList = clusterMap.computeIfAbsent(clusterName, k -> new ArrayList<>());
        if (!instanceList.contains(ipPort)) {
            instanceList.add(ipPort);
        }

        serviceInfo.setClusterMap(JsonUtil.toJson(clusterMap));
        serviceInfo.setGmtModified(new Date());
        zServiceInfoService.updateById(serviceInfo);
    }

    private void getOrCreateCluster(Long serviceId, String clusterName) {
        LambdaQueryWrapper<ZCluster> clusterWrapper = new LambdaQueryWrapper<ZCluster>()
                .eq(ZCluster::getServiceId, serviceId)
                .eq(ZCluster::getName, clusterName);
        ZCluster existCluster = zClusterService.getOne(clusterWrapper);

        if (existCluster == null) {
            ZCluster newCluster = new ZCluster();
            newCluster.setServiceId(serviceId);
            newCluster.setName(clusterName);
            newCluster.setHealthCheckInterval(5000);
            // 统一日期类型（LocalDateTime → Date 适配）
            LocalDateTime now = LocalDateTime.now();
            newCluster.setGmtCreate(now);
            newCluster.setGmtModified(now);
            zClusterService.save(newCluster);
        }
    }

    private void insertOrUpdateInstance(Long serviceId, ZNamingInstance namingInstance) {
        String instanceId = generateInstanceId(serviceId, namingInstance.getIp(), namingInstance.getPort());
        // 查询是否已存在该实例
        ZInstance existInstance = zInstanceService.getOne(new LambdaQueryWrapper<ZInstance>()
                .eq(ZInstance::getInstanceId, instanceId));

        ZInstance instance = new ZInstance();
        instance.setServiceId(serviceId);
        instance.setInstanceId(instanceId); // 填充instance_id
        instance.setIp(namingInstance.getIp());
        instance.setPort(namingInstance.getPort());
        instance.setWeight(namingInstance.getWeight() == null ? 1.0 : namingInstance.getWeight());
        instance.setHealthy(namingInstance.getHealthy() == null ? true : namingInstance.getHealthy());
        instance.setEnabled(namingInstance.getEnabled() == null ? true : namingInstance.getEnabled());
        instance.setEphemeral(namingInstance.getEphemeral() == null ? true : namingInstance.getEphemeral());
        instance.setClusterName(StringUtils.defaultIfBlank(namingInstance.getClusterName(), "DEFAULT"));

        // 元数据序列化
        if (namingInstance.getMetadata() != null) {
            try {
                instance.setMetadata(OBJECT_MAPPER.writeValueAsString(namingInstance.getMetadata()));
            } catch (JsonProcessingException e) {
                log.warn("序列化实例元数据失败", e);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        instance.setGmtCreate(now);
        instance.setGmtModified(now);

        if (existInstance != null) {
            instance.setId(existInstance.getId());
            zInstanceService.updateById(instance);
        } else {
            zInstanceService.save(instance);
        }
    }

    /**
     * 更新cluster_map：移除指定实例
     */
    private void updateServiceClusterMapRemoveInstance(ZServiceInfo serviceInfo, String clusterName, String ipPort) {
        String oldClusterMap = serviceInfo.getClusterMap();
        if (StringUtils.isBlank(oldClusterMap)) {
            return;
        }
        Map<String, List<String>> clusterMap = JsonUtil.fromJson(oldClusterMap,
                new TypeReference<Map<String, List<String>>>() {
                });
        if (!clusterMap.containsKey(clusterName)) {
            return;
        }

        List<String> instanceList = clusterMap.get(clusterName);
        instanceList.remove(ipPort);

        serviceInfo.setClusterMap(JsonUtil.toJson(clusterMap));
        serviceInfo.setGmtModified(new Date());
        zServiceInfoService.updateById(serviceInfo);
    }

    /**
     * 清理空集群
     */
    private void cleanEmptyCluster(Long serviceId, String clusterName) {
        // 查询该集群下是否还有实例
        Long instanceCount = (long) zInstanceService.count(new LambdaQueryWrapper<ZInstance>()
                .eq(ZInstance::getServiceId, serviceId)
                .eq(ZInstance::getClusterName, clusterName));
        if (instanceCount == 0) {
            // 删除空集群
            zClusterService.remove(new LambdaQueryWrapper<ZCluster>()
                    .eq(ZCluster::getServiceId, serviceId)
                    .eq(ZCluster::getName, clusterName));
            log.info("清理空集群：serviceId={}, clusterName={}", serviceId, clusterName);
        }
    }
}