#!/bin/bash
set -e

# 配置
PROJECT_NAME="z-config"
IMAGE_NAME="${PROJECT_NAME}:latest"
CONTAINER_NAME="${PROJECT_NAME}"
HOST_HTTP_PORT=8080
HOST_NETTY_PORT=8888
CONFIG_FILE="./application-prod.yml"

# 日志函数
log() {
    echo -e "\033[32m[$(date +'%Y-%m-%d %H:%M:%S')] $1\033[0m"
}

log "===================== 启动 Z-Config 服务 ====================="

# 检查镜像是否存在
if [ -z "$(docker images -q ${IMAGE_NAME})" ]; then
    log "错误：Docker镜像 ${IMAGE_NAME} 不存在，请先执行 ./build.sh 构建镜像"
    exit 1
fi

# 停止旧容器
if [ "$(docker ps -aq -f name=${CONTAINER_NAME})" ]; then
    log "停止并删除旧容器..."
    docker stop ${CONTAINER_NAME} > /dev/null
    docker rm ${CONTAINER_NAME} > /dev/null
fi

# 处理自定义配置文件
VOLUME_MOUNT=""
if [ -f "${CONFIG_FILE}" ]; then
    log "加载自定义配置文件：${CONFIG_FILE}"
    VOLUME_MOUNT="-v $(realpath ${CONFIG_FILE}):/app/application-prod.yml"
else
    log "未找到自定义配置文件${CONFIG_FILE}，使用默认配置"
fi

# 启动新容器
log "启动新容器..."
docker run -d \
    --name ${CONTAINER_NAME} \
    --restart=always \
    -p ${HOST_HTTP_PORT}:8080 \
    -p ${HOST_NETTY_PORT}:8888 \
    ${VOLUME_MOUNT} \
    ${IMAGE_NAME}

log "容器启动成功！"
log "HTTP服务地址：http://localhost:${HOST_HTTP_PORT}"
log "Netty配置服务端口：${HOST_NETTY_PORT}"
log ""
log "容器状态："
docker ps --filter name=${CONTAINER_NAME}
log ""
log "查看日志命令：docker logs -f ${CONTAINER_NAME}"
log "停止服务命令：docker stop ${CONTAINER_NAME}"
