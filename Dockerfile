# z-config 服务端镜像（lead 005 §1.3 / §4.1）
#
# 2026-10-18 修正两处必失败点：
#   1) 原 `COPY z-config-admin/target/z-config-admin-*.jar`：z-config-admin 已从 reactor 注释掉
#      （z-config/pom.xml 标「已废弃」），该路径下永远没有产物，docker build 必然 COPY 失败。
#      可执行入口已迁到 z-config-web（ZConfigWebApplication + spring-boot-maven-plugin，
#      classifier=exec），改拷 z-config-web 的 exec jar。
#   2) 原 EXPOSE 8888 是空头广告：Netty 长连接端口实际是 12888（Constance.serveBindPort，
#      z-config-common/src/main/java/com/zifang/z/config/common/Constance.java:14）。
#      8888 全仓无任何绑定。
#
# 构建前置：mvn -pl z-config-web -am package -DskipTests
# 基础镜像走 build-arg：默认官方坐标，但本网络内 Docker Hub 不可达
# （auth.docker.io i/o timeout），需要时用 --build-arg 换成可达的镜像站前缀。
ARG RUNTIME_BASE=eclipse-temurin:17-jre

FROM ${RUNTIME_BASE}

LABEL maintainer="zifang"

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

WORKDIR /app

COPY z-config-web/target/z-config-web-*-exec.jar app.jar

# 8080 = HTTP REST / OpenAPI；12888 = Netty 配置推送长连接
EXPOSE 8080 12888

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\n\r\n" >&3 && head -1 <&3 | grep -q 200'

ENTRYPOINT ["java", "-jar", "app.jar"]
