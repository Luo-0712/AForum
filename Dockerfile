# syntax=docker/dockerfile:1

# ==================== 构建阶段 ====================
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先复制 pom.xml，利用 Docker 层缓存加速依赖下载
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 复制源码并打包（跳过测试，测试依赖外部数据库）
COPY src ./src
RUN mvn -B -DskipTests package

# ==================== 运行阶段 ====================
FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="AForum"

WORKDIR /app

# 安装 curl（供健康检查使用）并创建非 root 运行用户
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -r -u 1001 appuser

# 复制后端 jar
COPY --from=build /build/target/AForum-0.0.1-SNAPSHOT.jar app.jar

# 以非 root 用户运行
USER appuser

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
