package com.zifang.z.config.web.api.guardfixture.foreign;

import org.apache.ibatis.annotations.Mapper;

/**
 * 夹具 mapper：**别的模块**注册的、与 z-config 无关的同名 mapper。
 * <p>
 * 线上那个抢走 {@code permissionMapper} 的 bean 就是这个形状：
 * MyBatis-Spring 用 {@code AnnotationBeanNameGenerator} 命名 ⇒ bean 名 =
 * 首字母小写的接口简名 = {@code permissionMapper}；
 * 代理是 {@code jdk.proxy2.$ProxyNN}（JDK 动态代理，不是 CGLIB）。
 * <p>
 * 本夹具刻意**用同名的接口**（{@code PermissionMapper}）复刻那一侧，
 * 好让本钉跑的是真形状，而不是「拿个随便的 bean 凑数」。
 *
 * @see com.zifang.z.config.web.api.ZNamespacePermissionInjectionGuardTest
 */
@Mapper
public interface PermissionMapper {

    /** 夹具方法：从不被调用，只需要一个可代理的接口方法。 */
    String ping();
}
