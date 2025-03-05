let stompClient = null;
let doctorId = null;

function login() {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    fetch("http://localhost:8080/api/v1/public/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: email, password: password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.cookie = `token=${data.data.token}; path=/`;
            doctorId = parseJwt(data.data.token).doctorId;
            alert("Login successful!");
            document.getElementById("login-form").style.display = "none";
            document.getElementById("dashboard").style.display = "block";
        } else {
            alert("Login failed!");
        }
    })
    .catch(error => console.error("Error:", error));
}

function connectWebSocket() {
    if (!doctorId) {
        alert("You must be logged in first!");
        return;
    }

    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log("Connected: " + frame);
        stompClient.subscribe(`/topic/appointments/${doctorId}`, function (message) {
            showMessage(JSON.parse(message.body));
        });
    });
}

function showMessage(message) {
    const messageDiv = document.getElementById("messages");
    const p = document.createElement("p");
    p.textContent = JSON.stringify(message, null, 2);
    messageDiv.appendChild(p);
}

function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(base64));
    } catch (e) {
        return null;
    }
}
