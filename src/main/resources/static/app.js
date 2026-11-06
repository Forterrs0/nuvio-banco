/* Nuvia — single-origin SPA. All money and balances are decided by the Java server. */
'use strict';

const app = document.querySelector('#app');
const modalRoot = document.querySelector('#modal-root');
const toastRoot = document.querySelector('#toast-root');
const state = {
  csrf: null, dashboard: null, user: null, transactions: [], view: 'dashboard',
  authMode: 'login', theme: localStorage.getItem('nuvia-theme') === 'light' ? 'light' : 'dark',
  balanceHidden: false, statementFilter: 'all', depositKey: null, pendingPix: null,
  pendingChart: null, busy: false, receipt: null,
  admin: {users:[], summary:null, search:'', page:0, pages:0, total:0, loading:false, error:'', requestId:0}
};
const currency = new Intl.NumberFormat('pt-BR', {style:'currency', currency:'BRL'});
const fmt = cents => currency.format((Number(cents) || 0) / 100);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const isoDate = iso => new Intl.DateTimeFormat('pt-BR',{dateStyle:'short',timeStyle:'short',timeZone:'America/Bahia'}).format(new Date(iso));
const shortDate = iso => new Intl.DateTimeFormat('pt-BR',{day:'2-digit',month:'short',timeZone:'America/Bahia'}).format(new Date(iso));
const welcomeMonth = () => new Intl.DateTimeFormat('pt-BR',{month:'short',year:'numeric',timeZone:'America/Bahia'}).format(new Date()).replace('.','').toUpperCase();
const firstName = name => String(name||'').split(/\s+/)[0];
const initials = name => String(name||'N').trim().split(/\s+/).slice(0,2).map(w=>w[0]||'').join('').toUpperCase();
const iconPaths = {
  grid:'<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  receipt:'<path d="M4 3h16v18l-4-2-4 2-4-2-4 2z"/><path d="M8 8h8M8 12h8"/>',
  card:'<rect x="2" y="5" width="20" height="14" rx="3"/><path d="M2 10h20"/>',
  send:'<path d="m22 2-7 20-4-9-9-4z"/><path d="M22 2 11 13"/>',
  arrowup:'<path d="M7 17 17 7M9 7h8v8"/>',
  arrowdown:'<path d="M7 7v10h10M7 17 17 7"/>',
  plus:'<path d="M12 5v14M5 12h14"/>',
  eye:'<path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6Z"/><circle cx="12" cy="12" r="3"/>',
  eyeoff:'<path d="M4 4 20 20M9.8 6.2A11.9 11.9 0 0 1 12 6c6.5 0 10 6 10 6a15 15 0 0 1-3 3.3M6 8C3.5 9.8 2 12 2 12s3.5 6 10 6a11 11 0 0 0 4.1-.75"/>',
  shield:'<path d="M12 22c5-2 8-6 8-12V5L12 2 4 5v5c0 6 3 10 8 12Z"/><path d="m9 12 2 2 4-4"/>',
  spark:'<path d="m12 3 2 6 6 2-6 2-2 6-2-6-6-2 6-2z"/><path d="M19 17v4M17 19h4"/>',
  logout:'<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>',
  moon:'<path d="M20 15.5A8.5 8.5 0 0 1 8.5 4a8.5 8.5 0 1 0 11.5 11.5"/>',
  sun:'<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>',
  close:'<path d="M18 6 6 18M6 6l12 12"/>',
  check:'<path d="m5 12 4 4L19 6"/>',
  info:'<circle cx="12" cy="12" r="10"/><path d="M12 11v5M12 8h.01"/>',
  copy:'<rect x="8" y="8" width="12" height="12" rx="2"/><path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"/>',
  activity:'<path d="M2 12h5l3-7 4 14 3-7h5"/>',
  user:'<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  chevron:'<path d="m9 18 6-6-6-6"/>',
  refresh:'<path d="M20 11a8 8 0 0 0-14.9-4M4 4v5h5M4 13a8 8 0 0 0 14.9 4M20 20v-5h-5"/>',
  lock:'<rect x="4" y="10" width="16" height="12" rx="2"/><path d="M8 10V6a4 4 0 0 1 8 0v4"/>',
  mail:'<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m2 6 10 8 10-8"/>',
  phone:'<path d="M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2Z"/><path d="M11 18h2"/>',
  checkcircle:'<circle cx="12" cy="12" r="10"/><path d="m8 12 3 3 5-6"/>',
  users:'<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>',
  search:'<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  calendar:'<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 3v4M17 3v4M3 10h18"/>',
  arrowleft:'<path d="m15 18-6-6 6-6"/>',
  printer:'<path d="M6 9V3h12v6M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><path d="M6 14h12v8H6z"/>',
  download:'<path d="M12 3v12m-4-4 4 4 4-4"/><path d="M4 17v4h16v-4"/>',
  share:'<circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><path d="m8.7 10.6 6.6-4.2M8.7 13.4l6.6 4.2"/>' 
};
const ico = (name, cls='') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true">${iconPaths[name]||iconPaths.info}</svg>`;
const logo = () => `<span class="logo"><span class="logo-mark">N<span>✦</span></span><span>nuvia<span class="brand-dot">.</span></span></span>`;
const btn = (name, label, extra='') => `<button type="button" data-action="${name}" ${extra}>${label}</button>`;

async function request(path, {method='GET', json, body, headers={}}={}) {
  const options = {method,credentials:'same-origin',cache:'no-store',headers:{...headers}};
  if (json !== undefined) {
    options.body = JSON.stringify(json);
    options.headers['Content-Type'] = 'application/json';
  } else if (body !== undefined) options.body = body;
  if (method !== 'GET' && method !== 'HEAD') {
    if (!state.csrf) await ensureCsrf();
    options.headers[state.csrf.headerName] = state.csrf.token;
  }
  let res;
  try { res = await fetch(path, options); }
  catch (_) { throw Error('Não foi possível conectar ao servidor. Confira se o Java está em execução.'); }
  let parsed;
  try { parsed = await res.json(); } catch (_) { parsed = {}; }
  if (!res.ok) {
    const err = Error(parsed.message || (res.status === 403 ? 'Sessão expirada. Atualize a página.' : 'Não foi possível concluir a operação.'));
    err.status = res.status;
    throw err;
  }
  return parsed;
}
async function ensureCsrf() {
  const res = await fetch('/api/auth/csrf',{credentials:'same-origin',cache:'no-store'});
  if (!res.ok) throw Error('Erro ao iniciar proteção da sessão.');
  state.csrf = await res.json();
}
async function loadDashboard() {
  state.dashboard = await request('/api/dashboard');
  state.user = state.dashboard.user;
  state.transactions = state.dashboard.recent;
}
function applyTheme() {
  document.body.dataset.theme = state.theme;
  localStorage.setItem('nuvia-theme',state.theme);
}
function toast(message, error=false) {
  toastRoot.className = 'toast-container';
  const el = document.createElement('div');
  el.className = 'toast' + (error?' error':'');
  const glyph = document.createElement('span');
  glyph.innerHTML = ico(error?'info':'checkcircle');
  const text = document.createElement('span');
  text.textContent = message;
  const close = document.createElement('button');
  close.setAttribute('aria-label','Fechar aviso');
  close.innerHTML = ico('close');
  close.addEventListener('click',()=>el.remove());
  el.append(glyph,text,close);
  toastRoot.prepend(el);
  setTimeout(()=>el.remove(),5200);
}
function showAuth(mode=state.authMode) {
  state.authMode = mode;
  closeModal();
  app.innerHTML = `<div class="auth-shell"><section class="auth-hero">${logo()}
    <div class="auth-message"><div class="eyebrow">UMA NOVA FORMA DE CUIDAR DAS SUAS FINANÇAS</div><h1>Seu dinheiro.<br><span>Seu ritmo.</span><br>Seu futuro.</h1>
      <p>Uma experiência bancária para explorar, aprender e movimentar valores entre contas Nuvia.</p></div>
    <div class="auth-hero-bottom">${ico('shield')} Ambiente acadêmico • Nenhum dinheiro real é movimentado</div></section>
    <section class="auth-stage"><div class="auth-form-card">
      <div class="auth-card-icon">${ico(mode==='login'?'lock':'user')}</div><div class="eyebrow">BEM-VINDO À NUVIA</div>
      <h2>${mode==='login'?'Entre na sua conta.':'Sua conta começa aqui.'}</h2>
      <p class="subtitle">${mode==='login'?'Acesse seu espaço financeiro com segurança.':'Cadastre-se para começar a explorar o Nuvia.'}</p>
      <div class="tab-row" role="tablist"><button type="button" role="tab" aria-selected="${mode==='login'}" class="${mode==='login'?'active':''}" data-action="login-tab">Entrar</button><button type="button" role="tab" aria-selected="${mode==='register'}" class="${mode==='register'?'active':''}" data-action="register-tab">Criar conta</button></div>
      <form id="auth-form" class="form-grid" data-mode="${mode}">
        ${mode==='register'?`<div class="field"><label for="auth-name">Nome completo</label><input id="auth-name" name="name" type="text" minlength="3" maxlength="80" autocomplete="name" placeholder="Como devemos chamar você?" required></div>`:''}
        <div class="field"><label for="auth-email">E-mail</label><input id="auth-email" name="email" type="email" maxlength="180" autocomplete="email" placeholder="voce@exemplo.com" required></div>
        ${mode==='register'?`<div class="field"><label for="auth-phone">Celular com DDD</label><input id="auth-phone" name="phone" type="tel" autocomplete="tel" placeholder="(71) 99999-9999" required><small>O e-mail e o telefone poderão ser usados para receber transferências internas.</small></div>`:''}
        <div class="field"><label for="auth-password">Senha</label><input id="auth-password" name="password" type="password" minlength="${mode==='register'?8:1}" maxlength="72" autocomplete="${mode==='register'?'new-password':'current-password'}" placeholder="${mode==='register'?'Mínimo de 8 caracteres':'Sua senha'}" required></div>
        <div id="auth-error" role="alert"></div>
        <button type="submit" class="primary-btn wide" id="auth-submit">${mode==='login'?'Acessar minha conta':'Criar minha conta'} ${ico('chevron')}</button>
      </form>
      <p class="auth-note">Projeto acadêmico Nuvia. Depósitos e Pix são internos e demonstrativos — não movimentam reais de verdade.</p>
    </div></section></div>`;
}
async function submitAuth(form) {
  const button = form.querySelector('#auth-submit');
  const error = form.querySelector('#auth-error');
  error.replaceChildren();
  const data = Object.fromEntries(new FormData(form));
  button.disabled = true;
  try {
    if (form.dataset.mode==='register') {
      await request('/api/auth/register',{method:'POST',json:data});
      toast('Conta criada! Entrando no seu espaço financeiro.');
    }
    await request('/api/auth/login',{method:'POST',body:new URLSearchParams({email:data.email,password:data.password}),headers:{'Content-Type':'application/x-www-form-urlencoded'}});
    state.csrf = null; // login rotates the Spring Security CSRF token
    await ensureCsrf();
    await loadDashboard();
    state.view = 'dashboard';
    renderApp();
  } catch (ex) {
    const message = document.createElement('div');
    message.className = 'auth-error';
    message.textContent = ex.message;
    error.replaceChildren(message);
  } finally { button.disabled = false; }
}
function renderApp() {
  const user = state.user;
  if (!user) return showAuth();
  const on = state.view;
  app.innerHTML = `<div class="app-shell"><aside class="sidebar">
    ${logo()}<div class="sidebar-label">SEU ESPAÇO</div>
    <nav class="nav-items" aria-label="Navegação principal">
      ${nav('dashboard','grid','Visão geral',on)}
      ${nav('statement','receipt','Extrato',on)}
      ${nav('card','card','Meu cartão',on)}
      ${user.admin ? nav('admin','users','Usuários',on) : ''}
    </nav><div class="sidebar-bottom">
      <div class="sidebar-promo"><span class="action-icon">${ico('spark')}</span><h3>Um novo jeito<br>de viver suas finanças.</h3><p>Feito para explorar, aprender e experimentar.</p><span class="micro">NUVIA / DEMO 2026</span></div>
      ${btn('toggle-theme',ico(state.theme==='dark'?'sun':'moon')+' <span>'+(state.theme==='dark'?'Modo claro':'Modo escuro')+'</span>','class="nav-btn"')}
      ${btn('logout',ico('logout')+' <span>Sair da conta</span>','class="nav-btn"')}
      <div class="profile"><span class="avatar">${esc(initials(user.name))}</span><div class="profile-meta"><strong title="${esc(user.name)}">${esc(user.name)}</strong><small>${user.admin?'Administrador Nuvia':'Minha conta Nuvia'}</small></div><span class="online-dot"></span></div>
    </div></aside>
    <main class="main"><div class="main-inner page-entrance">${on==='dashboard'?renderDashboard():on==='statement'?renderStatement():on==='admin'&&user.admin?renderAdmin():renderCard()}
    <footer class="footer"><span>Nuvia © 2026</span><span>Projeto acadêmico • Nenhuma operação financeira real</span></footer></div></main></div>`;
}
function nav(view,icon,label,active) {
  return `<button class="nav-btn ${active===view?'active':''}" data-action="navigate" data-view="${view}" aria-current="${active===view?'page':'false'}" title="${label}">${ico(icon)}<span>${label}</span>${active===view?'<i class="active-dot"></i>':''}</button>`;
}
// A UI is a convenience; authorization is enforced by Spring Security at /api/admin/**.
function renderAdmin() {
  const a = state.admin;
  const summary = a.summary;
  const start = a.total ? a.page * 10 + 1 : 0;
  const end = Math.min(a.total, (a.page + 1) * 10);
  const stats = [
    ['Total de contas', summary?.totalUsers, 'Contas cadastradas', 'users'],
    ['Novos cadastros', summary?.newLast30Days, 'Nos últimos 30 dias', 'calendar'],
    ['Administradores', summary?.administrators, 'Acesso privilegiado', 'shield']
  ];
  const rows = a.users.map(u => `<tr>
    <td><div class="admin-user"><span class="admin-avatar">${esc(initials(u.name))}</span><div><strong>${esc(u.name)}</strong><small>#${esc(u.id)}</small></div></div></td>
    <td class="admin-email">${esc(u.email)}</td>
    <td class="admin-phone">${esc(u.phone)}</td>
    <td><span class="admin-role ${u.admin?'is-admin':''}">${u.admin?'Administrador':'Cliente'}</span></td>
    <td><span class="admin-date">${u.createdAt?esc(isoDate(u.createdAt)):'—'}</span></td>
  </tr>`).join('');
  return `<div class="page-heading admin-heading"><div><div class="eyebrow">NUVIA / ÁREA RESTRITA</div><h1>Gestão de usuários <span class="greeting-star">✳</span></h1><p>Visualize com segurança as pessoas cadastradas na plataforma.</p></div>
    ${btn('admin-refresh',ico('refresh')+' Atualizar','class="outline-btn" '+(a.loading?'disabled':''))}</div>
    <div class="admin-kpis">${stats.map(([title,value,subtitle,icon]) => `<section class="admin-kpi"><div class="admin-kpi-top"><span>${title}</span><span class="admin-kpi-icon">${ico(icon)}</span></div><strong class="tabular">${value===undefined?'—':Number(value).toLocaleString('pt-BR')}</strong><small>${subtitle}</small></section>`).join('')}</div>
    <section class="admin-panel panel" aria-busy="${a.loading}"><div class="admin-panel-top"><div><div class="eyebrow">DIRETÓRIO</div><h2>Contas cadastradas</h2><p>Pesquisa por nome, e-mail ou telefone.</p></div><span class="admin-private">${ico('lock')} Acesso administrativo</span></div>
      <form class="admin-search-form" id="admin-search-form"><label class="admin-search" for="admin-search-input">${ico('search')}<input id="admin-search-input" name="search" value="${esc(a.search)}" maxlength="100" placeholder="Buscar usuário..." autocomplete="off" aria-label="Pesquisar usuários"></label><button type="submit" class="primary-btn" ${a.loading?'disabled':''}>Buscar</button></form>
      ${a.error?`<div class="admin-error" role="alert">${ico('info')} ${esc(a.error)}</div>`:''}
      <div class="admin-table-scroll"><table class="admin-table"><thead><tr><th>USUÁRIO</th><th>E-MAIL</th><th>TELEFONE</th><th>PERFIL</th><th>CADASTRO</th></tr></thead>
      <tbody>${a.loading?`<tr><td colspan="5"><div class="admin-empty">Carregando usuários...</div></td></tr>`:rows||`<tr><td colspan="5"><div class="admin-empty">${a.error?'Não foi possível carregar os cadastros.':a.search?'Nenhum cadastro encontrado para essa busca.':'Nenhum usuário cadastrado até agora.'}</div></td></tr>`}</tbody></table></div>
      <div class="admin-pagination"><span>${a.loading?'Consultando...':`Exibindo ${start}–${end} de ${Number(a.total).toLocaleString('pt-BR')} usuários`}</span><div class="admin-pages"><span>Página ${a.page+1} de ${Math.max(1,a.pages)}</span>
      ${btn('admin-prev',ico('arrowleft'),'class="icon-button" aria-label="Página anterior" '+(a.loading||a.page===0?'disabled':''))}
      ${btn('admin-next',ico('chevron'),'class="icon-button" aria-label="Próxima página" '+(a.loading||a.page+1>=a.pages?'disabled':''))}
      </div></div>
    </section><div class="admin-caption">${ico('shield')} Os dados só são enviados a usuários com permissão de administrador. Senhas e saldos não são expostos neste painel.</div>`;
}
async function loadAdmin(page = 0) {
  if (!state.user?.admin) return;
  const a = state.admin;
  const thisRequest = ++a.requestId;
  a.page = page;
  a.loading = true;
  a.error = '';
  renderApp();
  try {
    const params = new URLSearchParams({page:String(page),size:'10',search:a.search});
    const [users, summary] = await Promise.all([
      request('/api/admin/users?'+params.toString()),
      request('/api/admin/summary')
    ]);
    if (thisRequest !== a.requestId) return;
    a.users = users.users;
    a.total = users.total;
    a.pages = users.totalPages;
    a.page = users.page;
    a.summary = summary;
  } catch(ex) {
    if (thisRequest !== a.requestId) return;
    a.error = ex.message || 'Não foi possível consultar os usuários.';
    a.users = [];
    a.total = 0;
    a.pages = 0;
    a.summary = null;
  } finally {
    if (thisRequest === a.requestId) {
      a.loading = false;
      renderApp();
    }
  }
}
function renderDashboard() {
  const d = state.dashboard;
  const recent = d.recent || [];
  return `<header class="topbar"><div><div class="eyebrow">SEU PAINEL FINANCEIRO</div><h1>Olá, ${esc(firstName(state.user.name))} <span class="greeting-star">✳</span></h1><p>É bom ter você por aqui. Tudo pronto para um novo dia?</p></div>
    <div class="top-tools"><span class="top-date">${welcomeMonth()}</span>${btn('toggle-theme',ico(state.theme==='dark'?'sun':'moon'),'class="icon-button" title="Alternar tema" aria-label="Alternar tema"')}</div></header>
    <div class="dashboard-grid"><div class="stack">
      <section class="balance-card"><div class="balance-top"><span>Saldo disponível</span>${btn('toggle-balance',ico(state.balanceHidden?'eyeoff':'eye'),'title="Mostrar ou ocultar saldo" aria-label="Mostrar ou ocultar saldo"')}</div>
        <strong class="tabular">${state.balanceHidden?'R$ •••••':fmt(state.user.balanceCents)}</strong>
        <div class="balance-bottom"><span class="secure">${ico('shield')} No seu controle • saldo de demonstração</span><span class="mini-mark">n<span>.</span></span></div>
      </section><div class="actions-row">
        ${action('open-pix','send','Fazer Pix','')}${action('open-deposit','plus','Depositar','purple')}${action('navigate-card','card','Meu cartão','gold')}
      </div><div class="section-heading"><h2>Suas movimentações</h2>${btn('open-statement','Ver extrato completo '+ico('arrowup'),'class="text-link"')}</div>
      <section class="transactions-card">${recent.length?recent.slice(0,4).map(renderTx).join(''):empty('Nenhuma movimentação ainda. Faça um depósito demonstrativo para começar.')}</section>
    </div><div class="stack"><div class="stat-grid">
      ${stat('Entradas do mês',d.incomeMonthCents,'Movimentações recebidas','arrowdown','')}
      ${stat('Saídas do mês',d.expenseMonthCents,'Movimentações enviadas','arrowup','out')}
    </div><section class="chart-panel"><div class="chart-heading"><div><h2>Resumo financeiro</h2><small>Últimos 6 meses</small></div><span class="chart-view">${ico('activity')} Visão geral</span></div>
      <div class="legend"><span><i></i>Entradas</span><span><i class="purple"></i>Saídas</span></div>
      ${renderChart(d.months)}
    </section><section class="future">${ico('spark','text-mint')}<h2>Seu futuro está<br>em movimento.</h2><p>Explore uma experiência financeira feita para você.</p><small>NUVIA FINANCE EXPERIENCE</small></section></div></div>`;
}
function action(id, icon, label, color) {
  return `<button class="action-card" data-action="${id}"><span class="action-icon ${color}">${ico(icon)}</span><b>${label}</b>${ico('arrowup','action-arrow')}</button>`;
}
function stat(title, cents, caption, icon, type) {
  return `<section class="stat-card"><div class="stat-head"><span>${title}</span><span class="stat-icon ${type}">${ico(icon)}</span></div><strong class="tabular">${fmt(cents)}</strong><small>${caption}</small></section>`;
}
function renderChart(months=[]) {
  const max = Math.max(1,...months.flatMap(m=>[m.incomeCents,m.expenseCents]));
  const bars = months.map(m=> {
    const inc = (m.incomeCents/max)*117;
    const out = (m.expenseCents/max)*117;
    const label = esc(m.label).toUpperCase();
    return `<div class="month-cell" role="button" tabindex="0" data-action="chart-month" aria-label="${label}: entradas ${fmt(m.incomeCents)}, saídas ${fmt(m.expenseCents)}. Toque para detalhes.">
      <span class="bar" style="--h:${inc.toFixed(1)}px"></span><span class="bar out" style="--h:${out.toFixed(1)}px"></span><span class="month-label">${esc(m.label)}</span>
      <div class="chart-tooltip" role="tooltip"><strong>${label} • ${esc(m.key.split('-')[0])}</strong><dl>
        <dt>Entradas</dt><dd>${fmt(m.incomeCents)}</dd><dt>Saídas</dt><dd>${fmt(m.expenseCents)}</dd>
        <dt>Saldo líquido</dt><dd>${fmt(m.incomeCents-m.expenseCents)}</dd><dt>Operações</dt><dd>${m.count}</dd>
      </dl></div></div>`;
  }).join('');
  return `<div class="chart-body" aria-label="Gráfico financeiro dos últimos 6 meses">${bars}</div>`;
}
function renderTx(tx) {
  const isIn = tx.direction === 'IN';
  const title = tx.type==='DEMO_DEPOSIT'?'Depósito demonstrativo':`Pix ${isIn?'recebido de':'enviado para'} ${tx.counterparty}`;
  return `<div class="transaction"><span class="tx-icon ${isIn?'in':''}">${ico(isIn?'arrowdown':'arrowup')}</span><div class="tx-name"><strong>${esc(title)}</strong><small>${esc(tx.description||'Movimentação')}</small></div>
    <div class="tx-value"><strong class="tabular ${isIn?'text-mint':''}">${isIn?'+':'−'} ${fmt(Math.abs(tx.amountCents))}</strong><small>${shortDate(tx.createdAt)}</small></div>
    <button class="tx-open" type="button" data-receipt="${esc(tx.id)}" title="Abrir comprovante" aria-label="Abrir comprovante">${ico('chevron')}</button></div>`;
}
function empty(message) {return `<div class="empty"><span class="empty-icon">${ico('receipt')}</span>${esc(message)}</div>`;}
function renderStatement() {
  const filtered = state.transactions.filter(t=>state.statementFilter==='all'||(state.statementFilter==='in'?t.direction==='IN':t.direction==='OUT'));
  return `<div class="page-heading"><div><div class="eyebrow">SUAS OPERAÇÕES</div><h1>Extrato da conta</h1><p>Acompanhe depósitos demonstrativos, Pix enviados e recebidos.</p></div>${btn('refresh-statement',ico('refresh')+' Atualizar','class="outline-btn"')}</div>
    <section class="panel"><div class="section-heading" style="margin:0 0 15px"><h2>Histórico de movimentações</h2><span class="muted">Últimas 100 transações</span></div>
      <div class="tab-row" role="tablist" style="max-width:390px;margin:16px 0">
        ${[['all','Todas'],['in','Entradas'],['out','Saídas']].map(([filter,name])=>`<button class="${filter===state.statementFilter?'active':''}" data-action="statement-filter" data-filter="${filter}">${name}</button>`).join('')}
      </div><div class="statement-list">${filtered.length?filtered.map(renderTx).join(''):empty('Não há movimentações para este filtro.')}</div>
    </section>`;
}
function renderCard() {
  const user = state.user;
  const suffix = String(user.id).padStart(4,'0').slice(-4);
  return `<div class="page-heading"><div><div class="eyebrow">SEU ESPAÇO</div><h1>Meu cartão</h1><p>Uma representação virtual do seu cartão Nuvia.</p></div></div>
    <div class="panel"><div class="virtual-card" aria-label="Cartão demonstrativo Nuvia">
      <div class="card-brand">nuvia.<span>${ico('spark')}</span></div><div class="chip"></div><div class="card-number">•••• &nbsp; •••• &nbsp; •••• &nbsp; ${suffix}</div>
      <div class="card-bottom"><div><small>TITULAR</small>${esc(user.name.toUpperCase())}</div><div><small>TIPO</small>DEMONSTRAÇÃO</div></div></div>
      <div class="info-strip" style="max-width:520px;margin-top:24px">${ico('info')} Este cartão é apenas visual. Ele não possui número bancário real e não permite compras externas.</div>
      <div style="display:flex;gap:12px;flex-wrap:wrap;margin-top:24px">${btn('open-pix',ico('send')+' Fazer Pix','class="primary-btn"')}${btn('open-deposit',ico('plus')+' Depositar','class="outline-btn"')}</div>
    </div>`;
}
function closeModal() {
  modalRoot.replaceChildren();
  document.body.classList.remove('modal-open');
  state.pendingPix = null;
  state.depositKey = null;
  state.receipt = null;
}
function showModal(html) {
  modalRoot.innerHTML = `<div class="modal-backdrop" data-action="backdrop"><section class="modal" role="dialog" aria-modal="true" aria-label="Operação Nuvia">${html}</section></div>`;
  document.body.classList.add('modal-open');
  const target = modalRoot.querySelector('[data-initial-focus]') || modalRoot.querySelector('input:not([type=hidden])') || modalRoot.querySelector('button');
  target?.focus({preventScroll:true});
}
function modalHeader(label, title, subtitle) {
  return `<div class="modal-head"><div><div class="eyebrow">${label}</div><h2>${title}</h2><p>${subtitle}</p></div>${btn('close-modal',ico('close'),'class="icon-button" aria-label="Fechar"')}</div>`;
}
function moneyInput(id) {
  return `<div class="money-box"><div class="money-label">QUAL VALOR?</div><div id="${id}-display" class="money-display" aria-live="polite">R$ 0,00</div>
    <div class="money-hint">Digite os números • começando pelos centavos</div>
    <input id="${id}" class="money-keyboard" type="text" inputmode="numeric" autocomplete="off" pattern="[0-9]*" maxlength="10" aria-label="Valor em centavos; digite 5 para cinco centavos" aria-describedby="${id}-display" value="">
  </div>`;
}
function centsFrom(id) {return Number((document.getElementById(id)?.value||'').replace(/\D/g,''))||0;}
function validateCents(cents) {
  if (!Number.isSafeInteger(cents) || cents < 1 || cents > 10_000_000) throw Error('Informe um valor entre R$ 0,01 e R$ 100.000,00.');
}
function openDeposit() {
  state.depositKey = crypto.randomUUID();
  showModal(`${modalHeader('SUA CONTA NUVIA','Adicionar saldo','Escolha o valor que deseja adicionar ao saldo demonstrativo.')}
    <div class="form-grid">${moneyInput('deposit-value')}
      <div class="info-strip">${ico('info')} Este depósito é simulado. Ele credita saldo virtual no banco de dados para testar transferências entre usuários.</div>
    </div><div class="modal-actions">${btn('close-modal','Cancelar','class="outline-btn"')}${btn('deposit-confirm',ico('plus')+' Depositar','class="primary-btn"')}</div>`);
}
function openPix() {
  state.pendingPix = null;
  showModal(`${modalHeader('TRANSFERÊNCIA INTERNA','Enviar Pix','Envie saldo demonstrativo para outro usuário cadastrado no Nuvia.')}
    <div class="form-grid">
      <div class="field"><label for="pix-identifier">E-mail ou telefone do destinatário</label><input id="pix-identifier" type="text" autocomplete="off" placeholder="email@exemplo.com ou (71) 99999-9999"><small>Somente contas cadastradas no Nuvia podem receber.</small></div>
      ${moneyInput('pix-value')}
      <div class="field"><label for="pix-description">Descrição (opcional)</label><input id="pix-description" type="text" maxlength="140" placeholder="Ex.: pagamento de almoço"></div>
      <div class="info-strip">${ico('shield')} Transferência interna do Nuvia. A operação debita o remetente e credita o destinatário de maneira atômica.</div>
    </div><div class="modal-actions">${btn('close-modal','Cancelar','class="outline-btn"')}${btn('pix-preview','Continuar '+ico('chevron'),'class="primary-btn"')}</div>`);
}
function pixConfirm() {
  const p = state.pendingPix;
  if (!p) return;
  showModal(`${modalHeader('CONFIRME OS DADOS','Confirmar Pix','Verifique os dados antes de confirmar a transferência.')}
    <div class="recipient-confirm"><span class="avatar">${esc(initials(p.recipient.name))}</span><div><strong>${esc(p.recipient.name)}</strong><small>${esc(p.recipient.hint)}</small></div>${ico('checkcircle','text-mint')}</div>
    <div class="confirm-summary"><span>Você vai enviar</span><strong class="tabular">${fmt(p.amountCents)}</strong></div>
    <div class="confirm-summary"><span>Saldo após envio</span><strong class="tabular">${fmt(state.user.balanceCents-p.amountCents)}</strong></div>
    ${p.description?`<div class="confirm-summary"><span>Descrição</span><strong style="font-size:12px">${esc(p.description)}</strong></div>`:''}
    <div class="info-strip">${ico('info')} Operação demonstrativa. O valor será transferido para outra conta Nuvia, não para um banco externo.</div>
    <div class="modal-actions">${btn('pix-back','Voltar','class="outline-btn"')}${btn('pix-confirm',ico('check')+' Confirmar Pix','class="primary-btn"')}</div>`);
}
function receiptText(tx) {
  return [`NUVIA • COMPROVANTE ${tx.type==='DEMO_DEPOSIT'?'DE DEPÓSITO':'DE TRANSFERÊNCIA'}`,
    'Projeto acadêmico — valores demonstrativos','',`Valor: ${fmt(Math.abs(tx.amountCents))}`,
    `Data: ${isoDate(tx.createdAt)}`,`Origem: ${tx.senderName}`,`Destino: ${tx.recipientName}`,
    `Descrição: ${tx.description}`,`Identificador: ${tx.id}`].join('\n');
}
function showReceipt(tx) {
  state.receipt = tx;
  const isDeposit = tx.type==='DEMO_DEPOSIT';
  showModal(`${modalHeader('HISTÓRICO DA CONTA','Comprovante','Gerado a partir de uma operação registrada no Nuvia.')}
    <div class="receipt"><div class="receipt-top"><span class="receipt-check">${ico('check')}</span>
      <div class="eyebrow">${isDeposit?'DEPÓSITO DEMONSTRATIVO':'TRANSFERÊNCIA INTERNA NUVIA'}</div><strong class="tabular">${fmt(Math.abs(tx.amountCents))}</strong><small>${isoDate(tx.createdAt)}</small></div>
      <dl><dt>De</dt><dd>${esc(tx.senderName)}</dd><dt>Para</dt><dd>${esc(tx.recipientName)}</dd>
        <dt>Descrição</dt><dd>${esc(tx.description)}</dd><dt>Identificador</dt><dd class="id">${esc(tx.id)}</dd><dt>Situação</dt><dd class="text-mint">Concluído</dd></dl>
      <p class="receipt-print-disclaimer">Projeto acadêmico • Operação simulada. Não é um comprovante Pix oficial nem confirma uma movimentação bancária real.</p>
    </div>
    <div class="receipt-actions" aria-label="Opções do comprovante">
      ${btn('print-receipt',ico('printer')+' Imprimir','class="outline-btn"')}
      ${btn('download-receipt',ico('download')+' Baixar PDF','class="primary-btn"')}
      ${btn('share-receipt',ico('share')+' Compartilhar','class="outline-btn"')}
      ${btn('copy-receipt',ico('copy')+' Copiar dados','class="outline-btn"')}
    </div>`);
}
function receiptPdfFilename(tx) {
  return 'nuvia-comprovante-'+String(tx.id).replace(/[^a-z0-9-]/gi,'')+'.pdf';
}
async function receiptPdfBlob(tx) {
  if (!tx?.id) throw Error('Abra um comprovante antes de continuar.');
  let res;
  try {
    res = await fetch('/api/transactions/'+encodeURIComponent(tx.id)+'/receipt.pdf?download=true', {
      method:'GET', credentials:'same-origin', cache:'no-store'
    });
  } catch (_) { throw Error('Não foi possível conectar ao servidor para gerar o PDF.'); }
  if (!res.ok) {
    const payload = await res.json().catch(()=>({}));
    throw Error(payload.message || (res.status===401?'Entre novamente na sua conta.':'Não foi possível gerar o comprovante.'));
  }
  const blob=await res.blob();
  if(!blob.size || !blob.type.includes('pdf')) throw Error('O servidor não retornou um PDF válido.');
  return blob;
}
function saveReceiptPdf(blob,tx) {
  const url=URL.createObjectURL(blob);
  const anchor=document.createElement('a');
  anchor.href=url;
  anchor.download=receiptPdfFilename(tx);
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  setTimeout(()=>URL.revokeObjectURL(url),30000);
}
function animateMoney(input) {
  const display = document.getElementById(input.id+'-display');
  if (!display) return;
  const digits = input.value.replace(/\D/g,'').replace(/^0+(?=\d)/,'').slice(0,10);
  if (input.value!==digits) input.value=digits;
  display.textContent=fmt(Number(digits||0));
  display.classList.remove('pop');
  void display.offsetWidth; // restart a tiny animation without using a framework
  display.classList.add('pop');
}
function setBusy(active, clickedButton) {
  state.busy = active;
  if (clickedButton) {
    clickedButton.disabled = active;
    clickedButton.dataset.originalHtml ??= clickedButton.innerHTML;
    if (active) clickedButton.textContent='Processando...';
    else clickedButton.innerHTML=clickedButton.dataset.originalHtml;
  }
}
async function refreshAfterTransaction(view = 'dashboard') {
  await loadDashboard();
  state.view = view;
  if (view==='statement') state.transactions = await request('/api/transactions?limit=100');
  renderApp();
}
async function runAction(action, el) {
  if (state.busy) return;
  switch (action) {
    case 'login-tab': showAuth('login'); break;
    case 'register-tab': showAuth('register'); break;
    case 'toggle-theme': state.theme=state.theme==='dark'?'light':'dark';applyTheme();if(state.user) renderApp();else showAuth();break;
    case 'toggle-balance': state.balanceHidden=!state.balanceHidden;renderApp();break;
    case 'navigate': await navigate(el.dataset.view);break;
    case 'navigate-card': await navigate('card');break;
    case 'open-statement': await navigate('statement');break;
    case 'open-pix': openPix();break;
    case 'open-deposit': openDeposit();break;
    case 'close-modal': closeModal();break;
    case 'statement-filter': state.statementFilter=el.dataset.filter;renderApp();break;
    case 'chart-month': {
      const wasOpen=el.classList.contains('is-open');
      document.querySelectorAll('.month-cell').forEach(n=>n.classList.remove('is-open'));
      if(!wasOpen) el.classList.add('is-open');
      break;
    }
    case 'refresh-statement': await navigate('statement');toast('Extrato atualizado.');break;
    case 'admin-refresh': await loadAdmin(state.admin.page);break;
    case 'admin-prev': if(state.admin.page>0)await loadAdmin(state.admin.page-1);break;
    case 'admin-next': if(state.admin.page+1<state.admin.pages)await loadAdmin(state.admin.page+1);break;
    case 'logout': {
      setBusy(true,el);
      try {await request('/api/auth/logout',{method:'POST'});state.user=null;state.dashboard=null;state.csrf=null;await ensureCsrf();showAuth('login');}
      catch (ex) {toast(ex.message,true);} finally {setBusy(false,el);}break;
    }
    case 'deposit-confirm': {
      const amountCents = centsFrom('deposit-value');
      try {validateCents(amountCents);}catch (e){return toast(e.message,true);}
      setBusy(true,el);
      try {
        const tx=await request('/api/transactions/deposit',{method:'POST',json:{amountCents,idempotencyKey:state.depositKey}});
        await refreshAfterTransaction();showReceipt(tx);toast('Saldo demonstrativo adicionado com sucesso.');
      }catch(ex){toast(ex.message,true);}finally{setBusy(false,el);}break;
    }
    case 'pix-preview': {
      const identifier=document.getElementById('pix-identifier')?.value.trim()||'';
      const amountCents=centsFrom('pix-value');
      const description=document.getElementById('pix-description')?.value.trim()||'';
      if(!identifier)return toast('Informe o e-mail ou telefone do destinatário.',true);
      try{validateCents(amountCents);}catch(e){return toast(e.message,true);}
      if(amountCents>state.user.balanceCents)return toast('Saldo insuficiente para esta transferência.',true);
      setBusy(true,el);
      try{
        const recipient=await request('/api/recipients/resolve?identifier='+encodeURIComponent(identifier));
        state.pendingPix={identifier,amountCents,description,recipient,key:crypto.randomUUID()};
        pixConfirm();
      }catch(ex){toast(ex.message,true);}finally{setBusy(false,el);}break;
    }
    case 'pix-back': {
      const p=state.pendingPix;openPix();
      if(p){document.getElementById('pix-identifier').value=p.identifier;document.getElementById('pix-value').value=String(p.amountCents);animateMoney(document.getElementById('pix-value'));document.getElementById('pix-description').value=p.description;}
      break;
    }
    case 'pix-confirm': {
      const p=state.pendingPix;if(!p)return;
      setBusy(true,el);
      try{
        const tx=await request('/api/transactions/transfer',{method:'POST',json:{identifier:p.identifier,amountCents:p.amountCents,description:p.description,idempotencyKey:p.key}});
        await refreshAfterTransaction();showReceipt(tx);toast('Pix interno concluído! Os dois saldos foram atualizados.');
      }catch(ex){toast(ex.message,true);}finally{setBusy(false,el);}break;
    }
    case 'print-receipt': {
      if(!state.receipt)return toast('Abra um comprovante para imprimir.',true);
      window.print();
      break;
    }
    case 'download-receipt': {
      const tx=state.receipt;
      setBusy(true,el);
      try{saveReceiptPdf(await receiptPdfBlob(tx),tx);toast('Comprovante PDF gerado.');}
      catch(ex){toast(ex.message,true);}
      finally{setBusy(false,el);}
      break;
    }
    case 'share-receipt': {
      const tx=state.receipt;
      setBusy(true,el);
      try {
        const blob=await receiptPdfBlob(tx);
        const file=new File([blob],receiptPdfFilename(tx),{type:'application/pdf'});
        if(navigator.share && navigator.canShare?.({files:[file]})) {
          await navigator.share({title:'Comprovante Nuvia (demonstração)',
            text:'Comprovante de operação simulada do projeto acadêmico Nuvia.',files:[file]});
        } else {
          saveReceiptPdf(blob,tx);
          toast('PDF salvo. Anexe o arquivo no WhatsApp, e-mail ou aplicativo desejado.');
        }
      } catch(ex) {
        if(ex.name!=='AbortError')toast(ex.message||'Não foi possível compartilhar.',true);
      } finally{setBusy(false,el);}
      break;
    }
    case 'copy-receipt': {
      try{await navigator.clipboard.writeText(receiptText(state.receipt));toast('Comprovante copiado.');}
      catch(_){toast('Seu navegador não permitiu copiar automaticamente.',true);}break;
    }
  }
}
async function navigate(view) {
  if(view==='admin' && !state.user?.admin) return;
  state.view = view;
  if(view==='admin') return loadAdmin(0);
  if (view==='statement') {
    try{state.transactions=await request('/api/transactions?limit=100');}
    catch(ex){toast(ex.message,true);}
  }
  renderApp();
}
document.addEventListener('click',async event=>{
  const receipt = event.target.closest('[data-receipt]');
  if (receipt) {
    try{showReceipt(await request('/api/transactions/'+encodeURIComponent(receipt.dataset.receipt)));}
    catch (ex){toast(ex.message,true);}
    return;
  }
  const target = event.target.closest('[data-action]');
  if(!target)return;
  if(target.dataset.action==='backdrop') {
    if(event.target===target)closeModal();
    return;
  }
  if(target.disabled) return;
  try{await runAction(target.dataset.action,target);}catch(ex){toast(ex.message||'Erro inesperado.',true);}
});
document.addEventListener('input',event=>{
  if (event.target.classList?.contains('money-keyboard')) animateMoney(event.target);
});
document.addEventListener('keydown',event=>{
  if(event.key==='Escape'&&modalRoot.childElementCount)closeModal();
  if((event.key==='Enter'||event.key===' ')&&event.target.classList?.contains('month-cell')){
    event.preventDefault();runAction('chart-month',event.target);
  }
  if(event.key==='Enter'&&event.target.classList?.contains('money-keyboard')){
    event.preventDefault();
    const action=event.target.id.startsWith('deposit')?'deposit-confirm':'pix-preview';
    document.querySelector(`[data-action="${action}"]`)?.click();
  }
});
document.addEventListener('submit',event=>{
  if(event.target.id==='admin-search-form'){
    event.preventDefault();
    state.admin.search = String(new FormData(event.target).get('search')||'').trim();
    loadAdmin(0);
    return;
  }
  if(event.target.id==='auth-form'){
    event.preventDefault();
    submitAuth(event.target);
  }
});
(async function start(){
  applyTheme();
  try{
    await ensureCsrf();
    await loadDashboard();
    renderApp();
  }catch(ex){
    showAuth();
    if(ex.status!==401)toast(ex.message,true);
  }
})();
