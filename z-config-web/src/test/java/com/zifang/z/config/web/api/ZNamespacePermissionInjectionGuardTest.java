package com.zifang.z.config.web.api;

import com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;

import jakarta.annotation.Resource;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 守卫 z-config-web 里的 mapper 注入**按名命中正确的 bean**（卡 {@code TASK-20261024-113}）。
 *
 * <p><b>线上那一次崩溃的完整形状</b>（2026-10-24，z-opc-main CrashLoop，
 * 镜像 {@code z-opc-main:3}）：
 * <pre>
 * org.springframework.beans.factory.BeanCreationException:
 *   Error creating bean with name 'ZNamespacePermissionController': Injection of resource dependencies failed;
 *   nested exception is org.springframework.beans.factory.BeanNotOfRequiredTypeException:
 *   Bean named 'permissionMapper' is expected to be of type
 *   'com.zifang.z.config.core.domain.mapper.ZNamespacePermissionMapper'
 *   but was actually of type 'jdk.proxy2.$Proxy352'
 * </pre>
 *
 * <p><b>三件事同时成立才会炸</b>，本钉把三件都钉住：
 * <ol>
 *   <li>线上镜像吃的 z-config-web 是 Maven Central 上的
 *       {@code io.github.yuku123:z-config-web:1.0.10}（发布于 2026-10-02），
 *       <b>早于</b>本仓把字段改名的 {@code cd06726}（2026-10-18）
 *       ⇒ 线上那个控制器的字段名仍是通用裸名 {@code permissionMapper}；</li>
 *   <li>别的模块（z-lc）注册了一个**同名**的 mapper bean {@code permissionMapper}
 *       ⇒ {@code @Resource} 的「按名优先」把它抢走；</li>
 *   <li>抢到的那个 bean 类型对不上 ⇒ 直接抛异常 ⇒ 上下文启动失败。</li>
 * </ol>
 *
 * <p><b>为什么不能只写一句「字段名要规范」</b>：那种钉是空话。本钉跑的是**真容器**：
 * 一个真的 MyBatis 扫描（注册出真的 {@code jdk.proxy2.$ProxyNN} mapper bean）
 * + 真的 {@code @Resource} 注入 ⇒ 断言 1 复现线上异常原文，
 * 断言 2 用**真实的** {@link ZNamespacePermissionController} 证明改名后的形状能注入成功。
 *
 * @see com.zifang.z.config.web.api.guardfixture.LegacyShapeNamespaceController
 */
class ZNamespacePermissionInjectionGuardTest {

    /** 别的模块（夹具）注册的同名 mapper 所在包。 */
    private static final String FOREIGN_MAPPER_PKG =
            "com.zifang.z.config.web.api.guardfixture.foreign";

    /** z-config 自己的 mapper 包。 */
    private static final String OWN_MAPPER_PKG = "com.zifang.z.config.core.domain.mapper";

    /** 夹具：最小 SqlSessionFactory（不连真实库；mapper 从不被调用）。 */
    @Configuration
    static class SqlSessionFactoryConfig {
        @Bean
        public SqlSessionFactory sqlSessionFactory() {
            UnpooledDataSource ds = new UnpooledDataSource(
                    "com.mysql.cj.jdbc.Driver", "jdbc:mysql://127.0.0.1:1/none", "u", "p");
            return new SqlSessionFactoryBuilder().build(
                    new org.apache.ibatis.session.Configuration(
                            new org.apache.ibatis.mapping.Environment(
                                    "test", new JdbcTransactionFactory(), ds)));
        }
    }

    /**
     * 真扫描：z-config 自己的 mapper 包 + 「别的模块」的同名 mapper 包。
     * ⇒ 容器里同时存在 bean {@code zNamespacePermissionMapper} 与 bean {@code permissionMapper}。
     */
    @Configuration
    @MapperScan(basePackages = {OWN_MAPPER_PKG, FOREIGN_MAPPER_PKG})
    static class MapperScanConfig {
    }

    private static AnnotationConfigApplicationContext freshContext(Class<?>... controllers) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SqlSessionFactoryConfig.class, MapperScanConfig.class);
        for (Class<?> c : controllers) {
            ctx.register(c);
        }
        return ctx;
    }

    @Test
    @DisplayName("同构复现：@Resource 字段名撞上别的模块的同名 mapper bean ⇒ 必须抛 BeanNotOfRequiredTypeException")
    void legacyShapeMustReproduceTheProductionException() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(SqlSessionFactoryConfig.class, MapperScanConfig.class);
        ctx.register(com.zifang.z.config.web.api.guardfixture.LegacyShapeNamespaceController.class);

        Throwable thrown = assertThrows(Throwable.class, ctx::refresh,
                "旧形状（@Resource ZNamespacePermissionMapper permissionMapper）必须复现线上那次启动失败");
        assertTrue(hasCause(thrown, BeanNotOfRequiredTypeException.class), "异常链里必须有 BeanNotOfRequiredTypeException，实际 = " + thrown);
        BeanNotOfRequiredTypeException typeMismatch = findCause(thrown, BeanNotOfRequiredTypeException.class);
        assertTrue(typeMismatch.getMessage().contains("permissionMapper"), "异常原文必须点名被抢走的 bean 名（线上原文：Bean named 'permissionMapper'），实际 = "
                        + typeMismatch.getMessage());
        assertTrue(typeMismatch.getMessage().contains(ZNamespacePermissionMapper.class.getName()), "异常原文必须点明期望的类型，实际 = " + typeMismatch.getMessage());
    }

    @Test
    @DisplayName("真实控制器：字段名与 bean 名对齐 ⇒ 在有同名异类 bean 的容器里也必须注入成功")
    void realControllerMustInjectSuccessfullyEvenWhenAForeignBeanSharesTheOldName() throws Exception {
        try (AnnotationConfigApplicationContext ctx = freshContext(ZNamespacePermissionController.class)) {
            ctx.refresh();

            // 前提：容器里确实存在那个「抢名」的 bean —— 否则这条钉是尺坏了（阳性对照）
            assertTrue(ctx.containsBean("permissionMapper"),
                    "容器里应当存在别的模块注册的 permissionMapper bean（阳性对照失败）");
            assertTrue(ctx.containsBean(ZNamespacePermissionMapper.class.getSimpleName()),
                    "容器里应当存在 z-config 自己的 " + ZNamespacePermissionMapper.class.getSimpleName()
                            + " bean（阳性对照失败）；注意 MyBatis-Spring 的 bean 名 = "
                            + "Introspector.decapitalize(接口简名)，而 decap(\"ZNamespacePermissionMapper\") "
                            + "= \"ZNamespacePermissionMapper\"（第二个字母是大写 ⇒ 首字母不降）");

            ZNamespacePermissionController controller = ctx.getBean(ZNamespacePermissionController.class);
            Field field;
            try {
                field = ZNamespacePermissionController.class.getDeclaredField("zNamespacePermissionMapper");
            } catch (NoSuchFieldException e) {
                throw new AssertionError("ZNamespacePermissionController 上找不到 zNamespacePermissionMapper 字段 —— "
                        + "有人把字段名改回去了（那就是本卡线上那次崩溃的形状）", e);
            }
            field.setAccessible(true);
            Object injected = field.get(controller);
            assertNotNull(injected, "真实控制器的 mapper 必须注入成功（字段名不得被同名异类 bean 抢走）");
            assertSame(ctx.getBean(ZNamespacePermissionMapper.class), injected,
                    "注入进来的必须就是 z-config 自己的那个 mapper（不是别的同名 bean 的代理）");
        }
    }

    @Test
    @DisplayName("通则：z-config-web 里 @Resource 注入 mapper 的字段名不得被「别的 mapper」的规范 bean 名占用")
    void resourceMapperFieldNamesMustNotBeSquattableByAnotherMapper() {
        // 规范 bean 名 → 接口 FQN。MyBatis-Spring 的命名规则 = Introspector.decapitalize(接口简名)。
        Map<String, String> canonicalNameToFqn = new LinkedHashMap<>();
        for (Class<?> mapper : mapperInterfacesOnClasspath()) {
            canonicalNameToFqn.put(java.beans.Introspector.decapitalize(mapper.getSimpleName()),
                    mapper.getName());
        }
        assertFalse(canonicalNameToFqn.isEmpty(), "classpath 上一个 mapper 接口都没扫到 —— 尺坏了（阳性对照失败）");

        List<String> violations = new ArrayList<>();
        List<Class<?>> scanned = classesUnder("com.zifang.z.config.web");

        for (Class<?> clazz : scanned) {
            if (clazz.getName().contains(".guardfixture.")) {
                // 夹具刻意复刻旧形状（那正是要钉住的历史形状），不参与本条通则。
                continue;
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getAnnotation(Resource.class) == null) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (!fieldType.isInterface() || fieldType.getAnnotation(Mapper.class) == null) {
                    continue;
                }
                // 别的 mapper 的规范 bean 名 == 本字段名 ⇒ 这个字段名是「可被抢走」的
                // （线上：字段名 permissionMapper 抢走它的正是 z-lc 的 PermissionMapper）
                String squatter = canonicalNameToFqn.get(field.getName());
                if (squatter != null && !squatter.equals(fieldType.getName())) {
                    violations.add(clazz.getName() + "#" + field.getName()
                            + " 的规范 bean 名会被别的 mapper " + squatter
                            + " 占用 ⇒ @Resource 按名注入会拿到类型不对的代理");
                }
            }
        }

        assertFalse(scanned.isEmpty(), "扫 z-config.web 一个类都没扫到 —— 尺坏了（阳性对照失败）");
        assertTrue(violations.isEmpty(), "@Resource 是按名优先注入：字段名一旦是别的 mapper 的规范 bean 名，"
                + "就会被那个 mapper 的代理抢走 ⇒ 类型对不上 ⇒ 启动即挂（线上已踩：TASK-20261024-113）；"
                + "违规项：" + violations);
    }

    /** classpath 上的 mapper 接口（@Mapper 标注的接口）。 */
    private static List<Class<?>> mapperInterfacesOnClasspath() {
        List<Class<?>> mappers = new ArrayList<>();
        for (Class<?> clazz : classesUnder("com.zifang")) {
            if (clazz.isInterface() && clazz.getAnnotation(Mapper.class) != null) {
                mappers.add(clazz);
            }
        }
        return mappers;
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        return findCause(t, type) != null;
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        Throwable cur = t;
        while (cur != null) {
            if (type.isInstance(cur)) {
                return type.cast(cur);
            }
            cur = cur.getCause();
        }
        return null;
    }

    /** 扫出某个包下的所有类（走 classpath）。 */
    private static List<Class<?>> classesUnder(String basePackage) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            org.springframework.core.io.Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:" + ClassUtils.convertClassNameToResourcePath(basePackage)
                            + "/**/*.class");
            for (org.springframework.core.io.Resource resource : resources) {
                String path = resource.getURL().getPath();
                String marker = basePackage.replace('.', '/');
                int idx = path.indexOf(marker);
                if (idx < 0) {
                    continue;
                }
                String relative = path.substring(idx + marker.length() + 1);
                relative = relative.substring(0, relative.length() - ".class".length());
                String className = basePackage + "." + relative.replace('/', '.');
                try {
                    classes.add(Class.forName(className, false,
                            Thread.currentThread().getContextClassLoader()));
                } catch (Throwable ignored) {
                    // 扫不到的（可选依赖等）不参与判定
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("扫 " + basePackage + " 下的类失败 —— 尺坏了", e);
        }
        return classes;
    }
}
