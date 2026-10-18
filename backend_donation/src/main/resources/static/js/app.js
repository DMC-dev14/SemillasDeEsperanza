/* ==========================================================================
   Semillas de Esperanza — Lógica Frontend Conectada a Spring Boot + MySQL
   Ubicación Spring Boot: src/main/resources/static/js/app.js
   ========================================================================== */

const TESTIMONIALS = [
  {quote:'Gracias a las aulas equipadas por Semillas, mi hijo tiene acceso a computadoras por primera vez. Hoy sueña con ser ingeniero.',name:'María Condori',role:'Madre de familia, Lima',image:'https://images.unsplash.com/photo-1531123897727-8f129e1688ce?w=80&h=80&fit=crop&auto=format'},
  {quote:'El programa de nutrición cambió todo. Mis hijos llegaban con hambre a clase y no podían concentrarse. Ahora sus notas mejoraron.',name:'Roberto Huanca',role:'Padre de familia, Trujillo',image:'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=80&h=80&fit=crop&auto=format'},
  {quote:'Como donante, la transparencia de Semillas me da confianza. Veo fotos, reportes y sé exactamente dónde va cada sol que aporto.',name:'Ana Flores',role:'Donante recurrente, Miraflores',image:'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=80&h=80&fit=crop&auto=format'},
];

const PW_REGEX = /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,100}$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

let selectedAmount = 50;
let selectedType = 'unica';
let selectedPay = 'card';
let selectedProjectId = 1;
let testIdx = 0;
let testTimer = null;
let editingProfile = false;
let changingPw = false;
let regStep = 0;
const STEP_LABELS = ['Datos personales', 'Contraseña', '¡Listo!'];

// ══════════ GESTIÓN DE SESIÓN Y FETCH API ══════════
function saveAuthSession(authData) {
  if (!authData) return;
  if (authData.token) {
    localStorage.setItem('auth_token', authData.token);
  }
  if (authData.user) {
    localStorage.setItem('auth_user', JSON.stringify(authData.user));
  }
}

function clearAuthSession() {
  localStorage.removeItem('auth_token');
  localStorage.removeItem('auth_user');
}

function getAuthToken() {
  return localStorage.getItem('auth_token');
}

function getAuthUser() {
  try {
    const raw = localStorage.getItem('auth_user');
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

async function apiFetch(url, options = {}) {
  const headers = Object.assign({ 'Content-Type': 'application/json' }, options.headers || {});
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const response = await fetch(url, Object.assign({}, options, {
    headers,
    credentials: 'same-origin'
  }));

  if (response.status === 204) {
    return null;
  }

  let data = null;
  const contentType = response.headers.get('content-type') || '';
  if (contentType.includes('application/json') || contentType.includes('application/problem+json')) {
    try {
      data = await response.json();
    } catch (_) {
      data = null;
    }
  }

  if (!response.ok) {
    let errorMsg = 'Ocurrió un error al procesar la solicitud.';
    if (data) {
      if (Array.isArray(data.errors) && data.errors.length > 0) {
        errorMsg = data.errors.join(' · ');
      } else if (data.detail) {
        errorMsg = data.detail;
      } else if (data.message) {
        errorMsg = data.message;
      }
    }
    const err = new Error(errorMsg);
    err.status = response.status;
    err.data = data;
    throw err;
  }

  return data;
}

async function handleLogout() {
  try {
    await apiFetch('/api/auth/logout', { method: 'POST' });
  } catch (_) {
    // Limpiar sesión local incluso si la red falla
  }
  clearAuthSession();
  showToast('info', 'Has cerrado sesión correctamente.');
  setTimeout(() => {
    window.location.href = '/login';
  }, 400);
}

// ══════════ MOSTRAR / OCULTAR CONTRASEÑA ══════════
function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;
  const isPassword = input.type === 'password';
  input.type = isPassword ? 'text' : 'password';
  if (btn) {
    btn.textContent = isPassword ? '🙈' : '👁️';
    btn.setAttribute('aria-label', isPassword ? 'Ocultar contraseña' : 'Mostrar contraseña');
  }
}

// ══════════ UTILIDADES DE VALIDACIÓN Y ALERTAS ══════════
function clearFieldError(inputId, errId) {
  const inp = document.getElementById(inputId);
  const err = document.getElementById(errId);
  if (inp) inp.classList.remove('error');
  if (err) err.classList.add('hidden');
}

function setFieldError(inputId, errId, message) {
  const inp = document.getElementById(inputId);
  const err = document.getElementById(errId);
  if (inp) inp.classList.add('error');
  if (err) {
    if (message) err.textContent = '⚠ ' + message;
    err.classList.remove('hidden');
  }
}

function showAlertBox(boxId, message, type = 'error') {
  const box = document.getElementById(boxId);
  if (!box) return;
  box.className = `auth-alert ${type} mb-4`;
  box.textContent = message;
  box.classList.remove('hidden');
}

function hideAlertBox(boxId) {
  const box = document.getElementById(boxId);
  if (box) box.classList.add('hidden');
}

// ══════════ MENU MÓVIL ══════════
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
  }, 4500);
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
  document.querySelectorAll('#filter-pills .pill').forEach(p => p.classList.remove('active'));
  if (btn) btn.classList.add('active');
  const cards = document.querySelectorAll('#projects-grid .project-card[data-category]');
  if (cards.length > 0) {
    cards.forEach(card => {
      const c = card.getAttribute('data-category');
      card.style.display = (cat === 'Todos' || c === cat) ? '' : 'none';
    });
  }
}

// ══════════ DONACIONES (PÚBLICO Y CLIENTE) ══════════
function initDonatePage() {
  const hiddenProj = document.getElementById('input-proyecto-id');
  if (hiddenProj && hiddenProj.value) {
    selectedProjectId = hiddenProj.value;
  }
  updateSummary();
}

function selectProject(id, el) {
  selectedProjectId = id;
  const hiddenProj = document.getElementById('input-proyecto-id');
  if (hiddenProj) hiddenProj.value = id;
  document.querySelectorAll('#project-selector .proj-opt').forEach(opt => {
    const isSel = String(opt.getAttribute('data-project-id')) === String(id);
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
  document.getElementById('amount-error')?.classList.add('hidden');
  updateSummary();
}

function onCustomAmount(inp) {
  selectedAmount = parseFloat(inp.value) || 0;
  const hiddenMonto = document.getElementById('input-monto');
  if (hiddenMonto) hiddenMonto.value = selectedAmount;
  document.querySelectorAll('.amount-btn').forEach(b => b.classList.remove('selected'));
  if (selectedAmount >= 5) {
    document.getElementById('amount-error')?.classList.add('hidden');
  }
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
  hideAlertBox('donate-server-alert');
  const nameInput = document.getElementById('donor-name');
  const emailInput = document.getElementById('donor-email');
  if (!nameInput || !emailInput) return;

  const name = nameInput.value.trim();
  const email = emailInput.value.trim();
  let valid = true;

  if (name.length < 2) {
    setFieldError('donor-name', 'name-error', 'El nombre debe tener al menos 2 caracteres');
    valid = false;
  } else {
    clearFieldError('donor-name', 'name-error');
  }

  if (!EMAIL_REGEX.test(email)) {
    setFieldError('donor-email', 'email-error', 'Ingresa un correo electrónico válido');
    valid = false;
  } else {
    clearFieldError('donor-email', 'email-error');
  }

  if (!selectedAmount || selectedAmount < 5) {
    document.getElementById('amount-error')?.classList.remove('hidden');
    valid = false;
  } else {
    document.getElementById('amount-error')?.classList.add('hidden');
  }

  if (!valid) {
    showToast('error', 'Por favor revisa los campos obligatorios de tu donación.');
    return;
  }

  const selOpt = document.querySelector('#project-selector .proj-opt.selected .proj-opt-name');
  const projTitle = selOpt ? selOpt.textContent : 'Proyecto Social';
  document.getElementById('modal-desc').innerHTML = `Estás por donar <strong style="color:var(--terra)">S/${Number(selectedAmount).toFixed(2)}</strong> al proyecto <strong>${projTitle}</strong>.`;
  document.getElementById('modal-email-desc').innerHTML = `Se registrará el comprobante para <strong>${email}</strong>.`;
  document.getElementById('donate-modal').classList.remove('hidden');
}

function closeDonateModal() {
  const modal = document.getElementById('donate-modal');
  if (modal) modal.classList.add('hidden');
}

async function confirmDonation() {
  const confirmBtn = document.getElementById('confirm-btn');
  const name = document.getElementById('donor-name').value.trim();
  const email = document.getElementById('donor-email').value.trim();
  const phone = document.getElementById('donor-phone')?.value.trim() || '';
  const hiddenProj = document.getElementById('input-proyecto-id');
  const campaignId = Number(hiddenProj?.value || selectedProjectId || 1);

  closeDonateModal();
  if (confirmBtn) {
    confirmBtn.disabled = true;
    confirmBtn.textContent = 'Procesando donación...';
  }

  try {
    const result = await apiFetch('/api/donations', {
      method: 'POST',
      body: JSON.stringify({
        campaignId,
        amount: selectedAmount,
        donorName: name,
        donorEmail: email,
        donorPhone: phone,
        paymentMethod: selectedPay,
        donationType: selectedType,
        message: `Donación ${selectedType} de ${name}`
      })
    });

    showToast('success', `¡Gracias ${name.split(' ')[0]}! Donación registrada con recibo ${result.voucherNumber} 🌱`);
    showAlertBox('donate-server-alert', `¡Donación confirmada! Recibo N° ${result.voucherNumber} · Transacción ${result.codeReferential} por S/${Number(result.amount).toFixed(2)} para "${result.campaignTitle}".`, 'success');

    const headerRole = document.getElementById('header')?.getAttribute('data-role');
    if (headerRole === 'USUARIO') {
      setTimeout(() => {
        window.location.href = '/dashboard#mis-donaciones';
      }, 1500);
    }
  } catch (err) {
    showAlertBox('donate-server-alert', err.message || 'No se pudo procesar la donación.');
    showToast('error', err.message || 'Error al registrar la donación.');
  } finally {
    if (confirmBtn) {
      confirmBtn.disabled = false;
      updateSummary();
    }
  }
}

// ══════════ DASHBOARD DE CLIENTE ══════════
function openReceiptModal(btn) {
  const modal = document.getElementById('receipt-modal');
  if (!modal || !btn) return;
  document.getElementById('rec-voucher').textContent = btn.getAttribute('data-voucher') || 'REC-2026';
  document.getElementById('rec-campaign').textContent = btn.getAttribute('data-campaign') || '-';
  document.getElementById('rec-amount').textContent = 'S/ ' + Number(btn.getAttribute('data-amount') || 0).toFixed(2);
  document.getElementById('rec-date').textContent = btn.getAttribute('data-date') || '-';
  document.getElementById('rec-method').textContent = btn.getAttribute('data-method') || '-';
  document.getElementById('rec-tx').textContent = btn.getAttribute('data-tx') || '-';
  modal.classList.remove('hidden');
}

function closeReceiptModal() {
  document.getElementById('receipt-modal')?.classList.add('hidden');
}

function toggleEditProfile() {
  editingProfile = !editingProfile;
  hideAlertBox('profile-edit-err');
  document.getElementById('profile-view')?.classList.toggle('hidden', editingProfile);
  document.getElementById('profile-edit')?.classList.toggle('hidden', !editingProfile);
  document.getElementById('save-profile-btn')?.classList.toggle('hidden', !editingProfile);
  document.getElementById('profile-actions')?.classList.toggle('hidden', editingProfile);
  const editBtn = document.getElementById('edit-profile-btn');
  if (editBtn) editBtn.textContent = editingProfile ? 'Cancelar' : 'Editar';
}

async function saveProfile() {
  hideAlertBox('profile-edit-err');
  const nameInput = document.getElementById('edit-user-name');
  const emailInput = document.getElementById('edit-user-email');
  const saveBtn = document.getElementById('btn-save-profile');

  const name = nameInput ? nameInput.value.trim() : '';
  const email = emailInput ? emailInput.value.trim() : '';

  if (name.length < 2 || name.length > 50) {
    showAlertBox('profile-edit-err', 'El nombre debe tener entre 2 y 50 caracteres.');
    return;
  }
  if (!EMAIL_REGEX.test(email)) {
    showAlertBox('profile-edit-err', 'Ingresa un formato de correo electrónico válido.');
    return;
  }

  if (saveBtn) {
    saveBtn.disabled = true;
    saveBtn.textContent = 'Guardando...';
  }

  try {
    const authData = await apiFetch('/api/auth/profile', {
      method: 'PATCH',
      body: JSON.stringify({ name, email })
    });
    saveAuthSession(authData);

    document.getElementById('profile-view-name').textContent = authData.user.name;
    document.getElementById('profile-view-email').textContent = authData.user.email;
    document.getElementById('profile-display-title').textContent = authData.user.name;
    document.getElementById('dash-user-greeting').textContent = authData.user.name + ' 👋';
    document.getElementById('profile-avatar-circle').textContent = authData.user.name.charAt(0).toUpperCase();

    toggleEditProfile();
    showToast('success', 'Perfil actualizado en la base de datos.');
  } catch (err) {
    showAlertBox('profile-edit-err', err.message || 'No se pudo actualizar el perfil.');
    showToast('error', err.message || 'Error al actualizar el perfil.');
  } finally {
    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.textContent = 'Guardar cambios';
    }
  }
}

function togglePwChange() {
  changingPw = !changingPw;
  hideAlertBox('pw-change-err');
  document.getElementById('pw-change-section')?.classList.toggle('hidden', !changingPw);
  document.getElementById('profile-actions')?.classList.toggle('hidden', changingPw && !editingProfile);
  if (!changingPw) {
    const c = document.getElementById('dash-curr-pw');
    const n = document.getElementById('dash-new-pw');
    const r = document.getElementById('dash-confirm-pw');
    if (c) c.value = '';
    if (n) n.value = '';
    if (r) r.value = '';
  }
}

async function savePwChange() {
  hideAlertBox('pw-change-err');
  const currentPassword = document.getElementById('dash-curr-pw')?.value || '';
  const newPassword = document.getElementById('dash-new-pw')?.value || '';
  const confirmPassword = document.getElementById('dash-confirm-pw')?.value || '';
  const btn = document.getElementById('btn-save-pw');

  if (!currentPassword) {
    showAlertBox('pw-change-err', 'Debes ingresar tu contraseña actual.');
    return;
  }
  if (!PW_REGEX.test(newPassword)) {
    showAlertBox('pw-change-err', 'La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.');
    return;
  }
  if (newPassword !== confirmPassword) {
    showAlertBox('pw-change-err', 'La confirmación de contraseña no coincide.');
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Guardando...';
  }

  try {
    await apiFetch('/api/auth/change-password', {
      method: 'POST',
      body: JSON.stringify({ currentPassword, newPassword })
    });
    togglePwChange();
    showToast('success', 'Contraseña actualizada correctamente en la base de datos.');
  } catch (err) {
    showAlertBox('pw-change-err', err.message || 'Error al cambiar la contraseña.');
    showToast('error', err.message || 'Error al cambiar la contraseña.');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Guardar';
    }
  }
}

// ══════════ REGISTRO DE CLIENTES (CONECTADO A /api/auth/register) ══════════
function handleRegisterFormSubmit(e) {
  if (e) e.preventDefault();
  if (regStep === 0) {
    regNext(0);
  } else if (regStep === 1) {
    regNext(1);
  }
}

async function regNext(step) {
  hideAlertBox('reg-server-alert');

  if (step === 0) {
    const name = document.getElementById('reg-name')?.value.trim() || '';
    const email = document.getElementById('reg-email')?.value.trim() || '';
    let ok = true;

    if (name.length < 2 || name.length > 50) {
      setFieldError('reg-name', 'reg-name-err', 'El nombre debe tener entre 2 y 50 caracteres');
      ok = false;
    } else {
      clearFieldError('reg-name', 'reg-name-err');
    }

    if (!EMAIL_REGEX.test(email)) {
      setFieldError('reg-email', 'reg-email-err', 'Ingresa un correo electrónico con formato válido');
      ok = false;
    } else {
      clearFieldError('reg-email', 'reg-email-err');
    }

    if (!ok) {
      showAlertBox('reg-server-alert', 'Por favor completa correctamente tu nombre y correo electrónico.');
      return;
    }

    document.getElementById('reg-step-0').classList.add('hidden');
    regStep = 1;
    document.getElementById('reg-step-1').classList.remove('hidden');
    updateSteps(1);
    return;
  }

  if (step === 1) {
    const name = document.getElementById('reg-name')?.value.trim() || '';
    const email = document.getElementById('reg-email')?.value.trim() || '';
    const pw = document.getElementById('reg-pw')?.value || '';
    const confirm = document.getElementById('reg-confirm')?.value || '';
    const terms = document.getElementById('reg-terms')?.checked || false;

    let ok = true;
    const missingRules = [];
    if (pw.length < 8 || pw.length > 100) missingRules.push('mínimo 8 caracteres');
    if (!/[A-Z]/.test(pw)) missingRules.push('una letra mayúscula');
    if (!/[a-z]/.test(pw)) missingRules.push('una letra minúscula');
    if (!/[0-9]/.test(pw)) missingRules.push('un número');

    if (missingRules.length > 0) {
      setFieldError('reg-pw', 'reg-pw-err', 'Falta: ' + missingRules.join(', '));
      ok = false;
    } else {
      clearFieldError('reg-pw', 'reg-pw-err');
    }

    if (!confirm || pw !== confirm) {
      setFieldError('reg-confirm', 'reg-confirm-err', 'Las contraseñas no coinciden');
      ok = false;
    } else {
      clearFieldError('reg-confirm', 'reg-confirm-err');
    }

    const termsErr = document.getElementById('reg-terms-err');
    if (!terms) {
      if (termsErr) termsErr.classList.remove('hidden');
      ok = false;
    } else {
      if (termsErr) termsErr.classList.add('hidden');
    }

    if (!ok) {
      showAlertBox('reg-server-alert', 'Revisa los campos marcados en rojo antes de crear tu cuenta.');
      showToast('error', 'Verifica los requisitos de la contraseña y los términos.');
      return;
    }

    const submitBtn = document.getElementById('reg-submit-btn');
    const backBtn = document.getElementById('reg-back-btn');
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = 'Creando cuenta...';
    }
    if (backBtn) backBtn.disabled = true;

    try {
      const authData = await apiFetch('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify({ name, email, password: pw })
      });

      saveAuthSession(authData);

      document.getElementById('reg-step-1').classList.add('hidden');
      regStep = 2;
      document.getElementById('reg-step-2').classList.remove('hidden');
      updateSteps(2);
      document.getElementById('reg-login-link')?.classList.add('hidden');

      const firstName = (authData.user?.name || name).split(' ')[0];
      showToast('success', `¡Cuenta creada! Bienvenido/a, ${firstName} 🌱`);

      setTimeout(() => {
        window.location.href = '/dashboard';
      }, 1000);
    } catch (err) {
      const msg = err.message || 'No se pudo completar el registro.';
      showAlertBox('reg-server-alert', msg);
      showToast('error', msg);

      // Si el correo ya está registrado (409 Conflict), regresar al paso 0 y marcar el campo correo
      if (err.status === 409 || msg.toLowerCase().includes('email')) {
        regBack();
        showAlertBox('reg-server-alert', msg);
        setFieldError('reg-email', 'reg-email-err', msg);
      }
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Crear cuenta';
      }
      if (backBtn) backBtn.disabled = false;
    }
  }
}

function regBack() {
  hideAlertBox('reg-server-alert');
  document.getElementById('reg-step-' + regStep)?.classList.add('hidden');
  regStep = Math.max(0, regStep - 1);
  document.getElementById('reg-step-' + regStep)?.classList.remove('hidden');
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
  const hasLen = pw.length >= 8 && pw.length <= 100;
  const hasUpper = /[A-Z]/.test(pw);
  const hasLower = /[a-z]/.test(pw);
  const hasNum = /[0-9]/.test(pw);

  updateReqItem('req-len', hasLen, 'Entre 8 y 100 caracteres');
  updateReqItem('req-upper', hasUpper, 'Al menos una letra mayúscula (A-Z)');
  updateReqItem('req-lower', hasLower, 'Al menos una letra minúscula (a-z)');
  updateReqItem('req-num', hasNum, 'Al menos un número (0-9)');

  const checks = [hasLen, hasUpper, hasLower, hasNum];
  const score = checks.filter(Boolean).length;
  const colors = ['#C1462F', '#8A5500', '#7C9A3E', '#2F9E6B'];
  const labels = ['Muy débil', 'Incompleta', 'Casi lista', 'Cumple todos los requisitos ✓'];
  const labelColors = ['#C1462F', '#8A5500', '#8A5500', '#2F9E6B'];

  for (let i = 0; i < 4; i++) {
    const bar = document.getElementById('pb' + i);
    if (bar) bar.style.background = i < score ? colors[score - 1] : 'var(--sand)';
  }
  const lbl = document.getElementById('pw-label');
  if (lbl) {
    if (pw && score > 0) {
      lbl.textContent = labels[score - 1];
      lbl.style.color = labelColors[score - 1];
    } else {
      lbl.textContent = '';
    }
  }
}

function updateReqItem(id, met, labelText) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.toggle('met', met);
  el.textContent = (met ? '✓ ' : '○ ') + labelText;
}

// ══════════ LOGIN (CONECTADO A /api/auth/login) ══════════
async function handleLogin(e) {
  if (e) e.preventDefault();
  hideAlertBox('login-server-alert');

  const email = document.getElementById('login-email')?.value.trim() || '';
  const password = document.getElementById('login-pw')?.value || '';
  let ok = true;

  if (!EMAIL_REGEX.test(email)) {
    setFieldError('login-email', 'login-email-err', 'Ingresa un correo electrónico válido');
    ok = false;
  } else {
    clearFieldError('login-email', 'login-email-err');
  }

  if (!password) {
    setFieldError('login-pw', 'login-pw-err', 'La contraseña no puede estar vacía');
    ok = false;
  } else {
    clearFieldError('login-pw', 'login-pw-err');
  }

  if (!ok) {
    showAlertBox('login-server-alert', 'Por favor ingresa tu correo y contraseña.');
    return false;
  }

  const submitBtn = document.getElementById('login-submit-btn');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.textContent = 'Verificando credenciales...';
  }

  try {
    const authData = await apiFetch('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password })
    });

    saveAuthSession(authData);

    const role = authData.user?.rolName || 'USUARIO';
    const userName = authData.user?.name || 'Usuario';
    showToast('success', `¡Bienvenido/a ${userName}! Redirigiendo...`);

    setTimeout(() => {
      if (role === 'ADMINISTRADOR') {
        window.location.href = '/admin';
      } else {
        window.location.href = '/dashboard';
      }
    }, 500);
  } catch (err) {
    const msg = err.message || 'Credenciales inválidas. Verifica tu correo y contraseña.';
    showAlertBox('login-server-alert', msg);
    showToast('error', msg);
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.textContent = 'Iniciar sesión';
    }
  }
}

// ══════════ PANEL DE ADMINISTRACIÓN (/admin) ══════════
function switchAdminTab(tabName) {
  const validTabs = ['resumen', 'campanas', 'donaciones', 'usuarios'];
  const target = validTabs.includes(tabName) ? tabName : 'resumen';

  document.querySelectorAll('.admin-section').forEach(sec => sec.classList.add('hidden'));
  document.getElementById('admin-sec-' + target)?.classList.remove('hidden');

  document.querySelectorAll('.admin-tab-btn').forEach(btn => {
    btn.classList.toggle('active', btn.getAttribute('data-tab') === target);
  });

  document.querySelectorAll('.admin-nav-link').forEach(link => {
    link.classList.toggle('active', link.getAttribute('data-admin-tab') === target);
  });

  if (window.location.pathname.startsWith('/admin')) {
    history.replaceState(null, '', '#' + target);
  }
}

function initAdminDashboard() {
  if (!document.getElementById('admin-sec-resumen')) return;
  const hash = (window.location.hash || '').replace('#', '');
  if (['resumen', 'campanas', 'donaciones', 'usuarios'].includes(hash)) {
    switchAdminTab(hash);
  }
  window.addEventListener('hashchange', () => {
    const h = (window.location.hash || '').replace('#', '');
    if (['resumen', 'campanas', 'donaciones', 'usuarios'].includes(h)) {
      switchAdminTab(h);
    }
  });
}

function openCampaignModal() {
  hideAlertBox('campaign-modal-err');
  document.getElementById('campaign-modal-title').textContent = 'Nueva Campaña Social';
  document.getElementById('camp-id').value = '';
  document.getElementById('camp-title').value = '';
  document.getElementById('camp-target').value = '';
  document.getElementById('camp-status').value = 'ACTIVE';
  document.getElementById('camp-image').value = '';
  document.getElementById('camp-desc').value = '';
  document.getElementById('camp-history').value = '';
  document.getElementById('campaign-modal')?.classList.remove('hidden');
}

function openEditCampaignModalFromBtn(btn) {
  if (!btn) return;
  hideAlertBox('campaign-modal-err');
  document.getElementById('campaign-modal-title').textContent = 'Editar Campaña #' + btn.getAttribute('data-id');
  document.getElementById('camp-id').value = btn.getAttribute('data-id') || '';
  document.getElementById('camp-title').value = btn.getAttribute('data-title') || '';
  document.getElementById('camp-target').value = btn.getAttribute('data-target') || '';
  document.getElementById('camp-status').value = btn.getAttribute('data-status') || 'ACTIVE';
  document.getElementById('camp-image').value = btn.getAttribute('data-img') || '';
  document.getElementById('camp-desc').value = btn.getAttribute('data-desc') || '';
  document.getElementById('camp-history').value = btn.getAttribute('data-history') || '';
  document.getElementById('campaign-modal')?.classList.remove('hidden');
}

function closeCampaignModal() {
  document.getElementById('campaign-modal')?.classList.add('hidden');
}

async function handleSaveCampaign(e) {
  if (e) e.preventDefault();
  hideAlertBox('campaign-modal-err');

  const id = document.getElementById('camp-id')?.value;
  const title = document.getElementById('camp-title')?.value.trim() || '';
  const targetAmount = parseFloat(document.getElementById('camp-target')?.value || '0');
  const status = document.getElementById('camp-status')?.value || 'ACTIVE';
  const imageUrl = document.getElementById('camp-image')?.value.trim() || '';
  const description = document.getElementById('camp-desc')?.value.trim() || '';
  const history = document.getElementById('camp-history')?.value.trim() || description;

  if (title.length < 3) {
    showAlertBox('campaign-modal-err', 'El título debe tener al menos 3 caracteres.');
    return;
  }
  if (!targetAmount || targetAmount < 100) {
    showAlertBox('campaign-modal-err', 'La meta de recaudación debe ser de al menos S/ 100.00.');
    return;
  }
  if (!description) {
    showAlertBox('campaign-modal-err', 'La descripción de la campaña es obligatoria.');
    return;
  }

  const btn = document.getElementById('btn-save-campaign');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Guardando...';
  }

  try {
    const url = id ? `/api/campaigns/${id}` : '/api/campaigns';
    const method = id ? 'PUT' : 'POST';
    await apiFetch(url, {
      method,
      body: JSON.stringify({
        title,
        description,
        history,
        imageUrl,
        targetAmount,
        status
      })
    });

    closeCampaignModal();
    showToast('success', id ? 'Campaña actualizada correctamente.' : 'Campaña creada exitosamente.');
    setTimeout(() => {
      window.location.href = '/admin#campanas';
      window.location.reload();
    }, 500);
  } catch (err) {
    showAlertBox('campaign-modal-err', err.message || 'Error al guardar la campaña.');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Guardar campaña';
    }
  }
}

async function changeCampaignStatus(id, status) {
  try {
    await apiFetch(`/api/campaigns/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    showToast('success', `Estado de campaña #${id} actualizado a ${status}.`);
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast('error', err.message || 'No se pudo actualizar el estado de la campaña.');
  }
}

async function deleteCampaign(id, title) {
  if (!confirm(`¿Estás seguro de eliminar la campaña "${title}"?`)) return;
  try {
    await apiFetch(`/api/campaigns/${id}`, { method: 'DELETE' });
    showToast('success', 'Campaña eliminada correctamente.');
    setTimeout(() => window.location.reload(), 500);
  } catch (err) {
    showToast('error', err.message || 'No se puede eliminar esta campaña.');
  }
}

async function changeDonationStatus(id, status) {
  try {
    await apiFetch(`/api/donations/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status })
    });
    showToast('success', `Estado de donación #${id} actualizado a ${status}.`);
    setTimeout(() => window.location.reload(), 600);
  } catch (err) {
    showToast('error', err.message || 'Error al actualizar el estado de la donación.');
  }
}

function openCreateUserModal() {
  hideAlertBox('create-user-err');
  document.getElementById('admin-user-name').value = '';
  document.getElementById('admin-user-email').value = '';
  document.getElementById('admin-user-role').value = 'USUARIO';
  document.getElementById('admin-user-pw').value = '';
  document.getElementById('create-user-modal')?.classList.remove('hidden');
}

function closeCreateUserModal() {
  document.getElementById('create-user-modal')?.classList.add('hidden');
}

async function handleAdminCreateUser(e) {
  if (e) e.preventDefault();
  hideAlertBox('create-user-err');

  const name = document.getElementById('admin-user-name')?.value.trim() || '';
  const email = document.getElementById('admin-user-email')?.value.trim() || '';
  const rol = document.getElementById('admin-user-role')?.value || 'USUARIO';
  const password = document.getElementById('admin-user-pw')?.value || '';

  if (name.length < 2 || name.length > 50) {
    showAlertBox('create-user-err', 'El nombre debe tener entre 2 y 50 caracteres.');
    return;
  }
  if (!EMAIL_REGEX.test(email)) {
    showAlertBox('create-user-err', 'Ingresa un correo electrónico válido.');
    return;
  }
  if (!PW_REGEX.test(password)) {
    showAlertBox('create-user-err', 'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.');
    return;
  }

  const btn = document.getElementById('btn-admin-create-user');
  if (btn) {
    btn.disabled = true;
    btn.textContent = 'Creando...';
  }

  try {
    await apiFetch('/api/users', {
      method: 'POST',
      body: JSON.stringify({ name, email, password, rol })
    });
    closeCreateUserModal();
    showToast('success', 'Usuario creado exitosamente en la base de datos.');
    setTimeout(() => {
      window.location.href = '/admin#usuarios';
      window.location.reload();
    }, 500);
  } catch (err) {
    showAlertBox('create-user-err', err.message || 'No se pudo crear el usuario.');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.textContent = 'Crear usuario';
    }
  }
}

async function toggleUserActive(id, active) {
  try {
    await apiFetch(`/api/users/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ active })
    });
    showToast('success', `Estado del usuario #${id} actualizado.`);
    setTimeout(() => window.location.reload(), 500);
  } catch (err) {
    showToast('error', err.message || 'No se pudo cambiar el estado del usuario.');
  }
}

// ══════════ CONTACTO ══════════
function handleContact(e) {
  const name = document.getElementById('c-name').value.trim();
  const email = document.getElementById('c-email').value.trim();
  const subject = document.getElementById('c-subject').value.trim();
  const message = document.getElementById('c-message').value.trim();
  let ok = true;
  if (!name) { document.getElementById('c-name-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-name-err').classList.add('hidden'); }
  if (!EMAIL_REGEX.test(email)) { document.getElementById('c-email-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-email-err').classList.add('hidden'); }
  if (!subject) { document.getElementById('c-subject-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-subject-err').classList.add('hidden'); }
  if (message.length < 20) { document.getElementById('c-message-err').classList.remove('hidden'); ok = false; }
  else { document.getElementById('c-message-err').classList.add('hidden'); }
  if (!ok) {
    if (e) e.preventDefault();
    return false;
  }
  if (e) e.preventDefault();
  document.getElementById('contact-form').classList.add('hidden');
  document.getElementById('contact-sent').classList.remove('hidden');
  document.getElementById('contact-sent-msg').innerHTML = `Gracias <strong>${name.split(' ')[0]}</strong>. Te escribiremos a <strong>${email}</strong> en menos de 24 horas hábiles.`;
  showToast('success', '¡Mensaje enviado! Te responderemos en menos de 24 horas.');
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
  initDonatePage();
  initAdminDashboard();
});
