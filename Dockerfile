# 基础镜像：Eclipse Temurin 21 JDK（支持amd64/arm64架构）
FROM eclipse-temurin:21-jdk

# 维护者信息
LABEL maintainer="zifang"

# 设置时区 + 装 wget (供 HEALTHCHECK 使用)
RUN apt-get update && apt-get install -y --no-install-recommends tzdata wget && \
    ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apt-get remove -y tzdata && apt-get autoremove -y && apt-get clean

# 工作目录
WORKDIR /app

# 复制SpringBoot jar包
COPY z-config-admin/target/z-config-admin-*.jar app.jar

# 暴露端口：HTTP 8080，Netty服务 8888
EXPOSE 8080 8888

# 健康检查（FEATURE034 T4）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q -O- http://127.0.0.1:8080/doc.html || exit 1

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]
