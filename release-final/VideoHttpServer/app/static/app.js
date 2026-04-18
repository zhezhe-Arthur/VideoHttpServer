async function loadVideos() {
    const app = document.getElementById("app");

    try {
        const response = await fetch("/api/videos");
        const videos = await response.json();

        if (!Array.isArray(videos) || videos.length === 0) {
            app.textContent = "没有找到视频文件";
            return;
        }

        const ul = document.createElement("ul");

        videos.forEach(fileName => {
            const li = document.createElement("li");
            const a = document.createElement("a");

            a.href = "/player.html?name=" + encodeURIComponent(fileName);
            a.textContent = fileName;

            li.appendChild(a);
            ul.appendChild(li);
        });

        app.innerHTML = "";
        app.appendChild(ul);
    } catch (error) {
        console.error(error);
        app.textContent = "加载视频列表失败";
    }
}

loadVideos();