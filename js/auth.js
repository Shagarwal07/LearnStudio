function requireAuth() {
    const token = localStorage.getItem("lms_token") || localStorage.getItem("token");

    if (!token) {
        window.location.href = "login.html";
    }
}

window.requireAuth = requireAuth;