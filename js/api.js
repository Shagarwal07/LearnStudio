const API = 'https://learnstudio-production.up.railway.app/api';

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
  if (!res.ok) throw new Error('Invalid credentials');
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
  if (!res.ok) throw new Error('Google login failed');
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
      throw new Error(text || 'Enrollment failed: ' + res.status);
    }
  }
  return text ? JSON.parse(text) : {};
}

async function apiMyEnrollments() {
  const res = await fetch(`${API}/enrollments/my`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Failed to fetch enrollments: ' + res.status);
  return res.json();
}

// Reads persisted, per-user lesson progress for one enrolled course.
async function apiGetCourseProgress(courseId) {
  const res = await fetch(`${API}/enrollments/${courseId}/progress`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Failed to fetch course progress: ' + res.status);
  return res.json();
}

// Marks a lesson complete and expects the backend to return refreshed course progress.
async function apiCompleteLesson(courseId, lessonId) {
  const res = await fetch(`${API}/enrollments/${courseId}/lessons/${lessonId}/complete`, {
    method: 'POST',
    headers: authHeaders()
  });
  if (!res.ok) throw new Error('Failed to complete lesson: ' + res.status);
  return res.json();
}

// ── User ─────────────────────────────────────────────────
async function apiGetMe() {
  const res = await fetch(`${API}/users/me`, { headers: authHeaders() });
  return res.json();
}

// ── Guard: redirect to login if not authenticated ────────
function requireAuth() {
  if (!getToken()) window.location.href = 'login.html';
}

// ── AI Features ──────────────────────────────────────────
async function aiSolveDoubt(question, courseName) {
  const res = await fetch(`${API}/ai/doubt`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ question, courseName })
  });
  const data = await res.json();
  return data.answer;
}

async function aiRecommend(enrolledCourses) {
  const res = await fetch(`${API}/ai/recommend`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ enrolledCourses })
  });
  const data = await res.json();
  return data.recommendations;
}

async function aiGenerateQuiz(topic) {
  const res = await fetch(`${API}/ai/quiz`, {
    method: 'POST', headers: authHeaders(),
    body: JSON.stringify({ topic })
  });
  const data = await res.json();
  return data.quiz;
}