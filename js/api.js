const API = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'
  ? 'http://localhost:8080/api'
  : 'https://learnstudio-1.onrender.com/api';

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
async function apiGetCourses(search = '') {
  const url = search ? `${API}/courses?search=${search}` : `${API}/courses`;
  const res = await fetch(url);
  return res.json();
}

async function apiGetCourse(id) {
  const res = await fetch(`${API}/courses/${id}`);
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

// ── AI ───────────────────────────────────────────────────
async function apiAiSolveDoubt(question, courseName, model = 'gemini') {
  const res = await fetch(`${API}/ai/doubt`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ question, courseName, model })
  });
  return res.json();
}

async function apiAiRecommend(enrolledCourses, model = 'gemini') {
  const res = await fetch(`${API}/ai/recommend`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ enrolledCourses, model })
  });
  return res.json();
}

async function apiAiQuiz(topic, model = 'gemini') {
  const res = await fetch(`${API}/ai/quiz`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ topic, model })
  });
  return res.json();
}