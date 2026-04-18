function getVideoName() {
    const params = new URLSearchParams(window.location.search);
    return params.get("name");
}

function initPlayer() {
    const title = document.getElementById("title");
    const video = document.getElementById("video");
    const message = document.getElementById("message");

    const fileName = getVideoName();

    if (!fileName) {
        title.textContent = "No video selected";
        message.textContent = "Missing video name in URL.";
        video.style.display = "none";
        return;
    }

    title.textContent = fileName;
    video.src = "/video?name=" + encodeURIComponent(fileName);
    message.textContent = "";
}

initPlayer();