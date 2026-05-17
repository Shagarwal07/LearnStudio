// If we are on port 5500 (Live Server), point to the local Spring Boot
// Otherwise, use relative paths (unified Docker/Production)
const API = window.location.port === '5500' 
  ? 'http://localhost:8080/api' 
  : '/api';

// Wake up Render backend on page load (free tier cold start)
if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
  fetch(`${API}/health`).catch(() => {});
}

// ── Token helpers ─────────────────────────────────────────
const getToken = () => localStorage.getItem('lms_token');
const setToken = (t) => localStorage.setItem('lms_token', t);
const setUser  = (u) => localStorage.setItem('lms_user', JSON.stringify(u));
const getUser  = () => JSON.parse(localStorage.getItem('lms_user') || 'null');
const logout   = () => { localStorage.clear(); window.location.href = 'login.html'; };

const authHeaders = () => ({
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${getToken()}`
});

// Small shared helper so all pages deep-link consistently.
function buildCourseDetailUrl(courseId) {
  return `course-detail.html?id=${encodeURIComponent(courseId)}`;
}

// ── Auth ─────────────────────────────────────────────────
async function apiLogin(email, password) {
  const res = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  if (!res.ok) {
    // ── Developer Bypass ──
    // Allows immediate access to admin tools if the backend/DB fails during development
    if (email === 'admin@learnhub.com' && password === 'admin123') {
      const devUser = { token: 'dev-debug-token', name: 'System Admin', email, role: 'ADMIN' };
      setToken(devUser.token);
      setUser({ name: devUser.name, email: devUser.email, role: devUser.role });
      return devUser;
    }

    const text = await res.text();
    try {
      const err = JSON.parse(text);
      throw new Error(err.message || 'Invalid credentials');
    } catch(e) {
      throw new Error('Invalid credentials (HTTP ' + res.status + ')');
    }
  }
  const data = await res.json();
  setToken(data.token);
  setUser({ name: data.name, email: data.email, role: data.role });
  return data;
}

async function apiRegister(name, email, password, role) {
  const res = await fetch(`${API}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password, role })
  });
  if (!res.ok) throw new Error('Registration failed');
  const data = await res.json();
  setToken(data.token);
  setUser({ name: data.name, email: data.email, role: data.role });
  return data;
}

async function apiGoogleLogin(credential) {
  const res = await fetch(`${API}/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ credential })
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    const msg = err.message || '';
    throw new Error(msg.includes('NOT_REGISTERED')
      ? 'No account found. Please register first using Sign Up with Google.'
      : 'Google login failed');
  }
  const data = await res.json();
  setToken(data.token);
  setUser({ name: data.name, email: data.email, role: data.role });
  return data;
}

async function apiGoogleRegister(credential, role) {
  const res = await fetch(`${API}/auth/google/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ credential, role })
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Google registration failed');
  }
  const data = await res.json();
  setToken(data.token);
  setUser({ name: data.name, email: data.email, role: data.role });
  return data;
}

// ── Courses ──────────────────────────────────────────────
// async function apiGetCourses(search = '') {
//   const url = search ? `${API}/courses?search=${search}` : `${API}/courses`;
//   const res = await fetch(url);
//   return res.json();
// }

async function apiGetCourses(search = '') {
  const url = search
    ? `${API}/courses?search=${search}`
    : `${API}/courses`;

  const res = await fetch(url);

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}`);
  }

  return await res.json();
}

async function apiGetCourseDetails(id) {
  const res = await fetch(`${API}/courses/${id}/details`);
  return res.json();
}

async function apiGetCourse(id) {
  const res = await fetch(`${API}/courses/${id}/details`);
  return res.json();
}

async function apiCreateCourse(courseData) {
  const res = await fetch(`${API}/courses`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify(courseData)
  });
  if (!res.ok) throw new Error('Failed to create course: ' + res.status);
  return res.json();
}

async function apiUpdateCourse(courseId, courseData) {
  const res = await fetch(`${API}/courses/${courseId}`, {
    method: 'PUT', headers: authHeaders(),
    body: JSON.stringify(courseData)
  });
  if (!res.ok) throw new Error('Failed to update course: ' + res.status);
  return res.json();
}

async function apiDeleteCourse(courseId) {
  const res = await fetch(`${API}/courses/${courseId}`, { method: 'DELETE', headers: authHeaders() });
  if (!res.ok) throw new Error('Failed to delete course: ' + res.status);
  return res.text(); // Often returns empty or simple string on success
}

// ── Enrollments ──────────────────────────────────────────
async function apiEnroll(courseId) {
  const res = await fetch(`${API}/enrollments/${courseId}`, {
    method: 'POST', headers: authHeaders()
  });
  const text = await res.text();
  console.log('Enroll response status:', res.status, 'body:', text);
  if (!res.ok) {
    // parse error message from Spring Boot
    try {
      const err = JSON.parse(text);
      throw new Error(err.message || err.error || 'Enrollment failed: ' + res.status);
    } catch(e) {
      throw new Error(text || 'Enrollment failed');
    }
  }
  return text ? JSON.parse(text) : {};
}

async function apiMyEnrollments() {
  const res = await fetch(`${API}/enrollments/my`, {
    method: 'GET',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error('Failed to load enrollments: ' + res.status);
  return res.json();
}

async function apiCourseProgress(courseId) {
    const token = getToken();
    console.log("Progress token exists (lms_token):", !!token);
    if (!token) console.error("No token found in localStorage for progress request.");

    const response = await fetch(`${API}/enrollments/progress/${encodeURIComponent(courseId)}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error(`Failed to load progress: ${response.status}`);
    }

    return await response.json();
}

async function apiCompleteLesson(courseId, lessonId) {
    const token = getToken();

    const response = await fetch(`${API}/enrollments/progress/${courseId}/lessons/${lessonId}/complete`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error(`Failed to complete lesson: ${response.status}`);
    }

    return await response.json();
}

// ── AI ───────────────────────────────────────────────────
async function apiAiSolveDoubt(question, courseName, model = 'gemini') {
  const url = `${API}/ai/doubt`;
  const response = await fetch(url, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ question, courseName, model })
  });
  
  console.log("AI request URL:", url);
  console.log("AI token exists:", !!localStorage.getItem("lms_token"));
  console.log("AI response status:", response.status);

  const text = await response.text();
  console.log("AI response body:", text);

  if (text === "ERROR:QUOTA_EXCEEDED") {
    return { error: "QUOTA_EXCEEDED", message: "AI quota reached. Please try later." };
  }
  try { return JSON.parse(text); } catch(e) { return text; }
}

async function apiAiRecommend(enrolledCourses, model = 'gemini') {
  const url = `${API}/ai/recommend`;
  const response = await fetch(url, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ enrolledCourses, model })
  });

  console.log("AI request URL:", url);
  console.log("AI token exists:", !!localStorage.getItem("lms_token"));
  console.log("AI response status:", response.status);

  const text = await response.text();
  console.log("AI response body:", text);

  if (text === "ERROR:QUOTA_EXCEEDED") {
    return { error: "QUOTA_EXCEEDED", message: "AI quota reached. Please try later." };
  }
  try { return JSON.parse(text); } catch(e) { return text; }
}

async function apiAiQuiz(topic, model = 'gemini') {
  const url = `${API}/ai/quiz`;
  const response = await fetch(url, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ topic, model })
  });

  console.log("AI request URL:", url);
  console.log("AI token exists:", !!localStorage.getItem("lms_token"));
  console.log("AI response status:", response.status);

  if (!response.ok) return "ERROR: " + response.status;

  const text = await response.text();
  console.log("AI response body:", text);

  if (text === "ERROR:QUOTA_EXCEEDED") {
    return { error: "QUOTA_EXCEEDED", message: "AI quota reached. Please try later." };
  }
  return text;
}