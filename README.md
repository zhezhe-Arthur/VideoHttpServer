# Video Folder Server

一个基于 Java 内置 `HttpServer` 实现的局域网视频文件夹服务。

它可以扫描指定目录中的视频文件，在浏览器中显示视频列表，并通过局域网让其他设备访问和播放。

---

## 功能特性

- 扫描配置目录中的视频文件
- 网页显示视频列表
- 支持基础视频播放
- 支持 `Range` 请求，改善拖动和缓冲体验
- 默认使用固定端口，端口被占用时自动切换随机可用端口
- 启动时读取 `config.properties`
- 如果配置文件不存在，会自动生成默认配置模板

---

## 当前支持的视频文件类型

当前会扫描这些扩展名：

- `.mp4`
- `.webm`
- `.mkv`
- `.mov`
- `.avi`

注意：

- 浏览器最适合播放 `mp4`
- `webm` 兼容性取决于浏览器和设备
- `mkv` 在很多浏览器中可能会直接下载，而不是在线播放

---

## 运行环境

- JDK 17 或更高版本
- IntelliJ IDEA（开发时可选）

---

## 项目结构

```text
VideoHttpServer
├─ src
│  ├─ VideoFolderServer.java
│  └─ static
│     ├─ index.html
│     └─ app.js
├─ config.properties
└─ README.md