package com.zifang.z.config.web.api;

import com.zifang.util.core.meta.Result;
import com.zifang.z.config.core.domain.entity.ZCluster;
import com.zifang.z.config.core.domain.service.IZClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 集群(命名空间)管理控制器.
 * <p>
 * API 基础路径: /api/cluster
 * 所属模块: z-config-web
 * 鉴权: 通过统一网关拦截,要求登录态合法
 *
 * <p>主要端点:
 * <ul>
 *   <li>GET /list — 获取所有集群(命名空间)列表</li>
 *   <li>POST /save — 新增或更新集群(命名空间)</li>
 *   <li>POST /delete — 根据主键删除集群(命名空间)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/cluster")
@Tag(name = "008_集群(命名空间)管理")
public class ClusterController {

    @Resource
    private IZClusterService clusterService;

    /**
     * 获取所有集群(命名空间)列表.
     *
     * @return 包含全部 ZCluster 实体的统一返回结构
     */
    @GetMapping("/list")
    @Operation(summary = "001_获取所有集群(命名空间)列表")
    public Result<List<ZCluster>> list() {
        return Result.success(clusterService.list());
    }

    /**
     * 新增或更新集群(命名空间).
     * <p>
     * 根据传入对象的主键是否为空,自动切换为新增或更新分支,并同步写入创建/修改时间.
     *
     * @param cluster 集群实体,主键为空时执行新增,非空时执行更新
     * @return 成功标识的统一返回结构
     */
    @PostMapping("/save")
    @Operation(summary = "002_新增/更新集群(命名空间)")
    public Result<String> save(@RequestBody ZCluster cluster) {
        if (cluster.getId() == null) {
            cluster.setGmtCreate(LocalDateTime.now());
            cluster.setGmtModified(LocalDateTime.now());
            clusterService.save(cluster);
        } else {
            cluster.setGmtModified(LocalDateTime.now());
            clusterService.updateById(cluster);
        }
        return Result.success();
    }

    /**
     * 根据主键删除集群(命名空间).
     *
     * @param id 集群主键 ID
     * @return 成功标识的统一返回结构
     */
    @PostMapping("/delete")
    @Operation(summary = "003_删除集群(命名空间)")
    public Result<String> delete(@RequestParam Long id) {
        clusterService.removeById(id);
        return Result.success();
    }
}
