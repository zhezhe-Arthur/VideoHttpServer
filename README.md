# Video Folder Server
在家庭wifi和局域网络中快速分享视频

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

## 配置文件

- 项目使用 config.properties 作为运行配置文件。
- 仓库中提供示例文件：config.properties.example
- 你可以复制一份并改为：config.properties
- 配置中有：video.dir=D:/MovieServer
- 需要改成真实存在的目录，并可以存放需要分享的视频

## 启动方式

- 在IDEA中直接运行VideoFolderServer.java
- 启动后控制台会输出：
```text
Trying preferred port: 7777
Server started
Computer access: http://localhost:7777/
Phone/iPad access: http://192.168.xxx.xx:7777/
```
- 如果首选端口被占用，程序会输出新的实际端口。

## 访问方式
- 手机和电脑必须在同一个局域网 / Wi-Fi 下
- 在浏览器输入Phone/iPad access地址访问
- localhost 在手机上指向的是手机自己，不是电脑

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