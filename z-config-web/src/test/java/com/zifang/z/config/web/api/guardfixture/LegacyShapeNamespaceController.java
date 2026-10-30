package com.zifang.z.config.web.api.guardfixture;

import com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper;

import jakarta.annotation.Resource;

/**
 * 夹具控制器：**复刻线上制品的旧形状**。
 * <p>
 * 线上镜像（{@code z-opc-main:3}）吃的 z-config-web 是 Maven Central 上的
 * {@code io.github.yuku123:z-config-web:1.0.10}，发布于 2026-10-02 ——
 * 早于本仓把字段改名的那笔提交（{@code cd06726}，2026-10-18）⇒
 * 线上那个 {@code ZNamespacePermissionController} 的字段名仍是 {@code permissionMapper}。
 * <p>
 * {@code @Resource} 是**按名优先**注入 ⇒ 字段名 {@code permissionMapper} 会命中
 * 「别的模块」（z-lc）注册的同名 mapper 代理 ⇒ 类型对不上 ⇒
 * {@code BeanNotOfRequiredTypeException} ⇒ 主壳启动即挂。
 * <p>
 * 本夹具把这个形状单独做成一个类，好让回归钉能**直接复现线上异常**，
 * 而不是只在注释里描述它。
 *
 * @see com.zifang.z.config.web.api.ZNamespacePermissionInjectionGuardTest
 */
public class LegacyShapeNamespaceController {

    /** 旧形状：字段名 = 通用裸名 {@code permissionMapper}，类型 = z-config 自己的 mapper。 */
    @Resource
    private ZNamespacePermissionMapper permissionMapper;

    public ZNamespacePermissionMapper getMapper() {
        return permissionMapper;
    }
}
