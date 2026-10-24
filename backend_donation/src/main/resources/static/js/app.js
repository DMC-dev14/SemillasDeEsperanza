/* ==========================================================================
   Semillas de Esperanza — Lógica Frontend (Spring Boot + Thymeleaf + Bootstrap 5)
   Ubicación Spring Boot: src/main/resources/static/js/app.js
   ========================================================================== */

const PROJECTS = [
  {id:'aulas-futuro',title:'Aulas del Futuro',category:'Educación',location:'Villa El Salvador, Lima',shortDesc:'Equipamos aulas modernas para 280 niños en Villa El Salvador con tecnología y material educativo.',description:'Construimos y equipamos aulas modernas para 280 niños en situación de vulnerabilidad en Villa El Salvador. El proyecto incluye computadoras, libros, material didáctico y capacitación para docentes.',goal:45000,raised:35100,donors:312,image:'https://images.unsplash.com/photo-1641736047654-48c2546fcf19?w=800&h=450&fit=crop&auto=format'},
  {id:'sonrisas',title:'Sonrisas Saludables',category:'Salud',location:'San Sebastián, Cusco',shortDesc:'Brigadas de salud mensuales para niños en comunidades rurales sin acceso a atención médica.',description:'Brigadas médicas mensuales que llevan atención pediátrica, odontológica y nutricional a comunidades rurales de Cusco. Atendemos a más de 150 niños por visita.',goal:28000,raised:12640,donors:189,image:'https://images.unsplash.com/photo-1698795635788-7291ee73e670?w=800&h=450&fit=crop&auto=format'},
  {id:'techo',title:'Techo y Esperanza',category:'Vivienda',location:'Belén, Iquitos',shortDesc:'Rehabilitamos viviendas en zonas de riesgo para familias con niños en Iquitos.',description:'Rehabilitamos viviendas precarias en las orillas del río Itaya. Cada casa rehabilitada recibe piso de concreto, techo seguro y servicio básico de agua potable.',goal:60000,raised:37200,donors:241,image:'https://images.unsplash.com/photo-1698795635846-cd9049746761?w=800&h=450&fit=crop&auto=format'},
  {id:'mesa-llena',title:'Mesa Llena',category:'Alimentación',location:'El Porvenir, Trujillo',shortDesc:'Almuerzo nutritivo diario para 420 niños de escuelas públicas en Trujillo.',description:'Programa de nutrición escolar que garantiza un almuerzo balanceado diario a 420 niños de tres escuelas públicas en El Porvenir.',goal:32000,raised:29120,donors:408,image:'https://images.unsplash.com/photo-1698795635937-31264cdbf359?w=800&h=450&fit=crop&auto=format'},
  {id:'talentos',title:'Talentos sin Límites',category:'Educación',location:'San Juan de Lurigancho, Lima',shortDesc:'Talleres artísticos y deportivos para adolescentes en riesgo de deserción escolar.',description:'Talleres de arte, música y deporte para adolescentes en riesgo de deserción escolar. El programa combina habilidades blandas, orientación vocacional y mentoría.',goal:18500,raised:6105,donors:97,image:'https://images.unsplash.com/photo-1700936206562-8c4cdb1d1926?w=800&h=450&fit=crop&auto=format'},
  {id:'agua',title:'Agua es Vida',category:'Salud',location:'Sechura, Piura',shortDesc:'Sistemas de agua potable para 1,200 personas en comunidades remotas de Piura.',description:'Instalación de sistemas de captación y purificación de agua en comunidades de Sechura donde las familias recorren kilómetros para obtener agua no potable.',goal:52000,raised:28600,donors:276,image:'https://images.unsplash.com/photo-1698795635788-7291ee73e670?w=800&h=450&fit=crop&auto=format'},
];

const TESTIMONIALS = [
  {quote:'Gracias a las aulas equipadas por Semillas, mi hijo tiene acceso a computadoras por primera vez. Hoy sueña con ser ingeniero.',name:'María Condori',role:'Madre de familia, Lima',image:'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=80&h=80&fit=crop&auto=format'},
  {quote:'El programa de nutrición cambió todo. Mis hijos llegaban con hambre a clase y no podían concentrarse. Ahora sus notas mejoraron.',name:'Roberto Huanca',role:'Padre de familia, Trujillo',image:'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format'},
  {quote:'Como donante, la transparencia de Semillas me da confianza. Veo fotos, reportes y sé exactamente dónde va cada sol que aporto.',name:'Ana Flores',role:'Donante recurrente, Miraflores',image:'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&h=80&fit=crop&auto=format'},
];

const TOP_DONORS = [
  {rank:1,total:2800,donations:14,badge:'🥇'},
  {rank:2,total:1950,donations:9,badge:'🥈'},
  {rank:3,total:1400,donations:7,badge:'🥉'},
  {rank:4,total:950,donations:5,badge:''},
  {rank:5,total:750,donations:4,badge:''},
];

const FEED_NAMES = ['Carlos M.','Anónimo','Sofía R.','Anónimo','Javier L.','Ana P.'];
const FEED_PROJECTS = ['Aulas del Futuro','Mesa Llena','Sonrisas Saludables','Techo y Esperanza'];
const FEED_AMOUNTS = [20,50,100,200,500];
const BADGE_MAP = {Educación:'badge-edu',Salud:'badge-sal',Vivienda:'badge-viv',Alimentación:'badge-ali'};

let selectedAmount = 50;
let selectedType = 'unica';
let selectedPay = 'card';
let selectedProjectId = 'mesa-llena';
let testIdx = 0;
let testTimer = null;
let feedInterval = null;
let editingProfile = false;
let changingPw = false;
let regStep = 0;
const STEP_LABELS = ['Datos personales','Contraseña','¡Listo!'];

// ══════════ MENU ══════════
function toggleMenu() {
  const m = document.getElementById('mobile-menu');
  if (m) m.classList.toggle('open');
}
function closeMenu() {
  const m = document.getElementById('mobile-menu');
  if (m) m.classList.remove('open');
}

// ══════════ TOAST ══════════
function showToast(type, msg) {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.innerHTML = `<p>${msg}</p>`;
  el.onclick = () => el.remove();
  container.appendChild(el);
  setTimeout(() => {
    el.style.opacity = '0';
    el.style.transform = 'translateY(20px)';
    el.style.transition = '.3s';
    setTimeout(() => el.remove(), 300);
  }, 4000);
}

// ══════════ HOME / TESTIMONIALS ══════════
function initHome() {
  if (document.getElementById('testimonial-text')) {
    renderTestimonial(testIdx);
    clearInterval(testTimer);
    testTimer = setInterval(() => {
      testIdx = (testIdx + 1) % TESTIMONIALS.length;
      renderTestimonial(testIdx);
    }, 5000);
  }
  setTimeout(() => {
    document.querySelectorAll('.progress-fill').forEach(f => {
      const w = f.style.width;
      f.style.width = '0';
      setTimeout(() => f.style.width = w, 50);
    });
  }, 100);
}

function renderTestimonial(i) {
  const t = TESTIMONIALS[i];
  const textEl = document.getElementById('testimonial-text');
  if (!textEl) return;
  textEl.textContent = t.quote;
  document.getElementById('testimonial-name').textContent = t.name;
  document.getElementById('testimonial-role').textContent = t.role;
  document.getElementById('testimonial-img').src = t.image;
  const dotsEl = document.getElementById('testimonial-dots');
  if (dotsEl) {
    dotsEl.innerHTML = TESTIMONIALS.map((_, j) =>
      `<div class="dot${j === i ? ' active' : ''}" onclick="setTestimonial(${j})"></div>`
    ).join('');
  }
}

function setTestimonial(i) {
  testIdx = i;
  renderTestimonial(i);
  clearInterval(testTimer);
  testTimer = setInterval(() => {
    testIdx = (testIdx + 1) % TESTIMONIALS.length;
    renderTestimonial(testIdx);
  }, 5000);
}

function handleNewsletter(e) {
  if (e) e.preventDefault();
  showToast('success', '¡Suscripción confirmada! 🌱');
  const inp = document.getElementById('newsletter-email');
  if (inp) inp.value = '';
}

// ══════════ PROJECTS FILTER ══════════
function filterProjects(cat, btn) {
  document.querySelectorAll('.pill').forEach(p => p.classList.remove('active'));
  if (btn) btn.classList.add('active');
  const cards = document.querySelectorAll('#projects-grid .project-card[data-category]');
  if (cards.length > 0) {
    cards.forEach(card => {
      const c = card.getAttribute('data-category');
      card.style.display = (cat === 'Todos' || c === cat) ? '' : 'none';
    });
  }
}

function initDetailFromQuery() {
  const params = new URLSearchParams(window.location.search);
  const id = params.get('id');
  if (!id) return;
  const p = PROJECTS.find(x => x.id === id);
  if (!p) return;
  const pct = Math.round((p.raised / p.goal) * 100);
  const raised = p.raised >= 1000 ? 'S/' + (p.raised / 1000).toFixed(1) + 'k' : 'S/' + p.raised;
  const goal = p.goal >= 1000 ? 'S/' + (p.goal / 1000).toFixed(1) + 'k' : 'S/' + p.goal;
  const img = document.getElementById('detail-img');
  const badge = document.getElementById('detail-badge');
  const loc = document.getElementById('detail-location');
  const title = document.getElementById('detail-title');
  const desc = document.getElementById('detail-description');
  const rEl = document.getElementById('detail-raised');
  const gEl = document.getElementById('detail-goal');
  const fEl = document.getElementById('detail-progress-fill');
  const pEl = document.getElementById('detail-progress-pct');
  const dEl = document.getElementById('detail-donors');
  if (img) { img.src = p.image; img.alt = p.title; }
  if (badge) { badge.className = 'badge ' + BADGE_MAP[p.category]; badge.textContent = p.category; }
  if (loc) loc.textContent = '📍 ' + p.location;
  if (title) title.textContent = p.title;
  if (desc) desc.textContent = p.description;
  if (rEl) rEl.textContent = raised;
  if (gEl) gEl.textContent = goal;
  if (fEl) fEl.style.width = pct + '%';
  if (pEl) pEl.textContent = pct + '%';
  if (dEl) dEl.textContent = p.donors.toLocaleString('es-PE');
}

// ══════════ DONATE ══════════
function selectProject(id, el) {
  selectedProjectId = id;
  const hiddenProj = document.getElementById('input-proyecto-id');
  if (hiddenProj) hiddenProj.value = id;
  document.querySelectorAll('#project-selector .proj-opt').forEach(opt => {
    const isSel = opt.getAttribute('data-project-id') === id;
    opt.classList.toggle('selected', isSel);
    const check = opt.querySelector('.proj-check-icon');
    if (check) check.classList.toggle('hidden', !isSel);
  });
  updateSummary();
}

function selectAmount(a, btn) {
  selectedAmount = a;
  const custom = document.getElementById('custom-amount');
  if (custom) custom.value = '';
  const hiddenMonto = document.getElementById('input-monto');
  if (hiddenMonto) hiddenMonto.value = a;
  document.querySelectorAll('.amount-btn').forEach(b => b.classList.remove('selected'));
  if (btn) btn.classList.add('selected');
  updateSummary();
}

function onCustomAmount(inp) {
  selectedAmount = parseInt(inp.value) || 0;
  const hiddenMonto = document.getElementById('input-monto');
  if (hiddenMonto) hiddenMonto.value = selectedAmount;
  document.querySelectorAll('.amount-btn').forEach(b => b.classList.remove('selected'));
  updateSummary();
}

function setType(t) {
  selectedType = t;
  const hiddenTipo = document.getElementById('input-tipo-donacion');
  if (hiddenTipo) hiddenTipo.value = t;
  const uBtn = document.getElementById('type-unica');
  const rBtn = document.getElementById('type-recurrente');
  const note = document.getElementById('recurring-note');
  if (uBtn) uBtn.classList.toggle('active', t === 'unica');
  if (rBtn) rBtn.classList.toggle('active', t === 'recurrente');
  if (note) note.classList.toggle('hidden', t === 'unica');
  updateSummary();
}

function selectPay(p) {
  selectedPay = p;
  const hiddenMetodo = document.getElementById('input-metodo-pago');
  if (hiddenMetodo) hiddenMetodo.value = p;
  ['card', 'transfer', 'wallet'].forEach(x => {
    const btn = document.getElementById('pay-' + x);
    if (btn) btn.classList.toggle('selected', x === p);
  });
  const cf = document.getElementById('card-fields');
  if (cf) cf.classList.toggle('hidden', p !== 'card');
  updateSummary();
}

function updateSummary() {
  const btn = document.getElementById('confirm-btn');
  if (btn) btn.textContent = `Confirmar donación · S/${selectedAmount || '–'}`;
}

function openDonateModal() {
  const name = document.getElementById('donor-name').value.trim();
  const email = document.getElementById('donor-email').value.trim();
  let valid = true;
  if (!name) { document.getElementById('name-error').classList.remove('hidden'); valid = false; }
  else { document.getElementById('name-error').classList.add('hidden'); }
  if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) { document.getElementById('email-error').classList.remove('hidden'); valid = false; }
  else { document.getElementById('email-error').classList.add('hidden'); }
  if (!selectedAmount || selectedAmount < 5) { document.getElementById('amount-error').classList.remove('hidden'); valid = false; }
  else { document.getElementById('amount-error').classList.add('hidden'); }
  if (!valid) return;

  const selOpt = document.querySelector('#project-selector .proj-opt.selected .proj-opt-name');
  const projTitle = selOpt ? selOpt.textContent : 'Mesa Llena';
  document.getElementById('modal-desc').innerHTML = `Estás por donar <strong style="color:var(--terra)">S/${selectedAmount}</strong> a <strong>${projTitle}</strong>.`;
  document.getElementById('modal-email-desc').innerHTML = `Se enviará un comprobante a <strong>${email}</strong>.`;
  document.getElementById('donate-modal').classList.remove('hidden');
}

function closeDonateModal() {
  const modal = document.getElementById('donate-modal');
  if (modal) modal.classList.add('hidden');
}

function confirmDonation() {
  closeDonateModal();
  const form = document.getElementById('donate-spring-form');
  const name = document.getElementById('donor-name').value.split(' ')[0];
  showToast('success', `¡Gracias ${name}! Tu donación de S/${selectedAmount} fue confirmada. 🌱`);
  if (form && form.getAttribute('data-submit-backend') === 'true') {
    form.submit();
    return;
  }
  document.getElementById('donor-name').value = '';
  document.getElementById('donor-email').value = '';
  selectedAmount = 50;
  document.querySelectorAll('.amount-btn').forEach((b, i) => b.classList.toggle('selected', i === 1));
  updateSummary();
}

// ══════════ DASHBOARD ══════════
function initDashboard() {
  const feed = document.getElementById('live-feed');
  if (!feed) return;
  clearInterval(feedInterval);
  feedInterval = setInterval(() => {
    const item = {
      name: FEED_NAMES[Math.floor(Math.random() * FEED_NAMES.length)],
      amount: FEED_AMOUNTS[Math.floor(Math.random() * FEED_AMOUNTS.length)],
      project: FEED_PROJECTS[Math.floor(Math.random() * FEED_PROJECTS.length)]
    };
    const el = document.createElement('div');
    el.className = 'feed-item';
    el.innerHTML = `<div class="feed-avatar">${item.name[0]}</div><div style="flex:1;min-width:0"><p class="text-sm truncate" style="color:var(--bark)"><strong>${item.name}</strong> donó <strong class="feed-amount">S/${item.amount}</strong></p><p class="text-xs text-clay truncate">a ${item.project}</p></div><span class="text-xs text-clay" style="white-space:nowrap;flex-shrink:0">ahora mismo</span>`;
    el.style.opacity = '0';
    feed.insertBefore(el, feed.firstChild);
    setTimeout(() => el.style.opacity = '1', 50);
    const items = feed.querySelectorAll('.feed-item');
    if (items.length > 5) items[items.length - 1].remove();
    items.forEach((it, i) => { it.style.opacity = Math.max(.3, 1 - i * .15); });
  }, 4000);
}

function toggleEditProfile() {
  editingProfile = !editingProfile;
  document.getElementById('profile-view').classList.toggle('hidden', editingProfile);
  document.getElementById('profile-edit').classList.toggle('hidden', !editingProfile);
  document.getElementById('save-profile-btn').classList.toggle('hidden', !editingProfile);
  document.getElementById('profile-actions').classList.toggle('hidden', editingProfile);
  document.getElementById('edit-profile-btn').textContent = editingProfile ? 'Cancelar' : 'Editar';
}

function saveProfile() {
  toggleEditProfile();
  showToast('success', 'Perfil actualizado correctamente.');
}

function togglePwChange() {
  changingPw = !changingPw;
  document.getElementById('pw-change-section').classList.toggle('hidden', !changingPw);
  document.getElementById('profile-actions').classList.toggle('hidden', changingPw && !editingProfile);
}

function savePwChange() {
  togglePwChange();
  showToast('success', 'Contraseña actualizada correctamente.');
}

// ══════════ REGISTER ══════════
function regNext(step) {
  if (step === 0) {
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    let ok = true;
    if (!name) { document.getElementById('reg-name-err').classList.remove('hidden'); ok = false; }
    else { document.getElementById('reg-name-err').classList.add('hidden'); }
    if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) { document.getElementById('reg-email-err').classList.remove('hidden'); ok = false; }
    else { document.getElementById('reg-email-err').classList.add('hidden'); }
    if (!ok) return;
  }
  if (step === 1) {
    const pw = document.getElementById('reg-pw').value;
    const confirm = document.getElementById('reg-confirm').value;
    const terms = document.getElementById('reg-terms').checked;
    let ok = true;
    if (pw.length < 8) { document.getElementById('reg-pw-err').classList.remove('hidden'); ok = false; }
    else { document.getElementById('reg-pw-err').classList.add('hidden'); }
    if (pw !== confirm) { document.getElementById('reg-confirm-err').classList.remove('hidden'); ok = false; }
    else { document.getElementById('reg-confirm-err').classList.add('hidden'); }
    if (!terms) { document.getElementById('reg-terms-err').classList.remove('hidden'); ok = false; }
    else { document.getElementById('reg-terms-err').classList.add('hidden'); }
    if (!ok) return;
    const name = document.getElementById('reg-name').value.split(' ')[0];
    showToast('success', `¡Cuenta creada! Bienvenido/a, ${name} 🌱`);
  }
  document.getElementById('reg-step-' + step).classList.add('hidden');
  regStep = step + 1;
  document.getElementById('reg-step-' + regStep).classList.remove('hidden');
  updateSteps(regStep);
  if (regStep === 2) document.getElementById('reg-login-link').classList.add('hidden');
}

function regBack() {
  document.getElementById('reg-step-' + regStep).classList.add('hidden');
  regStep--;
  document.getElementById('reg-step-' + regStep).classList.remove('hidden');
  updateSteps(regStep);
}

function updateSteps(current) {
  for (let i = 0; i < 3; i++) {
    const dot = document.getElementById('sdot-' + i);
    if (!dot) continue;
    if (i < current) { dot.className = 'step-dot done'; dot.textContent = '✓'; }
    else if (i === current) { dot.className = 'step-dot current'; dot.textContent = i + 1; }
    else { dot.className = 'step-dot'; dot.textContent = i + 1; }
  }
  for (let i = 0; i < 2; i++) {
    const line = document.getElementById('sline-' + i);
    if (line) line.classList.toggle('done', i < current);
  }
  const lbl = document.getElementById('step-label');
  if (lbl) lbl.textContent = STEP_LABELS[current];
}

function updatePwStrength(pw) {
  const checks = [pw.length >= 8, /[A-Z]/.test(pw), /[0-9]/.test(pw), /[^A-Za-z0-9]/.test(pw)];
  const score = checks.filter(Boolean).length;
  const colors = ['#C1462F', '#7C9A3E', '#7C9A3E', '#2F9E6B'];
  const labels = ['Muy débil', 'Débil', 'Buena', 'Fuerte'];
  const labelColors = ['#C1462F', '#8A5500', '#8A5500', '#2F9E6B'];
  for (let i = 0; i < 4; i++) {
    const bar = document.getElementById('pb' + i);
    if (bar) bar.style.background = i < score ? colors[score - 1] : 'var(--sand)';
  }
  const lbl = document.getElementById('pw-label');
  if (lbl) {
    if (pw && score > 0) { lbl.textContent = labels[score - 1]; lbl.style.color = labelColors[score - 1]; }
    else { lbl.textContent = ''; }
  }
}

// ══════════ CONTACT ══════════
function handleContact(e) {
  const name = document.getElementById('c-name').value.trim();
  const email = document.getElementById('c-email').value.trim();
  const subject = document.getElementById('c-subject').value.trim();
  const message = document.getElementById('c-message').value.trim();
  let ok = true;
  if (!name) { document.getElementById('c-name-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-name-err').classList.add('hidden'); }
  if (!email.match(/^[^\s@]+@[^\s@]+\.[^\s@]+$/)) { document.getElementById('c-email-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-email-err').classList.add('hidden'); }
  if (!subject) { document.getElementById('c-subject-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-subject-err').classList.add('hidden'); }
  if (message.length < 20) { document.getElementById('c-message-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-message-err').classList.add('hidden'); }
  if (!ok) {
    if (e) e.preventDefault();
    return false;
  }
  const form = document.getElementById('contact-form');
  if (!form || form.getAttribute('data-submit-backend') !== 'true') {
    if (e) e.preventDefault();
    document.getElementById('contact-form').classList.add('hidden');
    document.getElementById('contact-sent').classList.remove('hidden');
    document.getElementById('contact-sent-msg').innerHTML = `Gracias <strong>${name.split(' ')[0]}</strong>. Te escribiremos a <strong>${email}</strong> en menos de 24 horas hábiles.`;
    showToast('success', '¡Mensaje enviado! Te responderemos en menos de 24 horas.');
  }
}

function resetContact() {
  document.getElementById('contact-form').classList.remove('hidden');
  document.getElementById('contact-sent').classList.add('hidden');
  document.getElementById('c-name').value = '';
  document.getElementById('c-email').value = '';
  document.getElementById('c-subject').value = '';
  document.getElementById('c-message').value = '';
}

// ══════════ AUTO-INIT POR PÁGINA ══════════
document.addEventListener('DOMContentLoaded', () => {
  initHome();
  initDetailFromQuery();
  initDashboard();
  updateSummary();
});
