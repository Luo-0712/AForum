# syntax=docker/dockerfile:1

# ==================== 构建阶段 ====================
FROM node:22-alpine AS build
WORKDIR /app

# 先复制依赖清单，利用 Docker 层缓存
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# ==================== 运行阶段 ====================
FROM nginx:1.27-alpine

# 替换默认站点配置（含 SPA 回退与 API/WebSocket 反向代理）
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
