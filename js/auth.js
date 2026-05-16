function requireAuth() {
    const token = localStorage.getItem("lms_token");

    if (!token) {
        window.location.href = "login.html";
    }
}