function getAiModel() {
    return localStorage.getItem("ai_model") || "gemini";
}

function setAiModel(model) {
    localStorage.setItem("ai_model", model);
}

window.getAiModel = getAiModel;
window.setAiModel = setAiModel;