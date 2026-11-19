/* Cobalt HX 0.2.1 — original, lightweight TV identity (no third-party UI libraries). */
(() => {
  'use strict';
  const VERSION = '0.2.1';
  const previous = window.CobaltHX;
  if (previous && previous.version === VERSION) return;
  if (previous && typeof previous.destroy === 'function') previous.destroy();

  const STORAGE = 'cobalt-hx-preferences';
  const KEY = 'cobalt-hx';
  const THEME = { bronze: 'bronze', cyan: 'cyan' };
  let settings = { effects: true, theme: 'bronze', intensity: 'cinema' };
  try {
    const saved = JSON.parse(window.localStorage.getItem(STORAGE) || '{}');
    if (saved && typeof saved === 'object') {
      settings.effects = saved.effects !== false;
      settings.theme = saved.theme === 'cyan' ? 'cyan' : 'bronze';
      settings.intensity = saved.intensity === 'leve' ? 'leve' : 'cinema';
    }
  } catch (_) { /* Optional local storage can be disabled by TV privacy settings. */ }

  const css = [
    ':root{--hx-bronze:#d6a46e;--hx-cyan:#69e6dc;--hx-glass:#08131dde;--hx-border:#d6a46e80}',
    'html.hx-theme-cyan{--hx-bronze:#6edbdc;--hx-border:#69e6dc80}',
    '#cobalt-hx{position:fixed;inset:0;z-index:2147483000;pointer-events:none;font-family:Arial,Helvetica,sans-serif;color:#f5f6f7}',
    '#cobalt-hx *{box-sizing:border-box}',
    '#cobalt-hx button{cursor:pointer;font-family:inherit;pointer-events:auto}',
    '#cobalt-hx button:focus{outline:4px solid var(--hx-cyan)!important;outline-offset:3px}',
    '#cobalt-hx .hx-signature{position:absolute;top:18px;left:52%;transform:translateX(-50%);padding:9px 20px;display:flex;gap:13px;align-items:center;white-space:nowrap;border:1px solid var(--hx-border);border-radius:0 0 15px 15px;background:linear-gradient(120deg,#080e17ec,#17232bd9);box-shadow:0 10px 28px #0008;pointer-events:none}',
    '#cobalt-hx .hx-name{font-size:22px;letter-spacing:2px;font-weight:800;color:#f4f2ed}',
    '#cobalt-hx .hx-name b{color:var(--hx-cyan)}',
    '#cobalt-hx .hx-tag{font-size:11px;letter-spacing:1.5px;font-weight:700;color:var(--hx-bronze);border-left:1px solid #b5c1c35c;padding-left:13px}',
    '#cobalt-hx .hx-accent{position:absolute;left:10px;top:19%;height:55%;width:4px;border-radius:5px;background:linear-gradient(0deg,transparent,var(--hx-bronze),var(--hx-cyan),transparent);opacity:.65}',
    '#cobalt-hx .hx-launch{position:absolute;right:34px;bottom:28px;border:2px solid var(--hx-border);border-radius:18px;background:linear-gradient(130deg,#091822,#1c3441);color:#efffff;font-size:23px;font-weight:700;padding:15px 23px;box-shadow:0 12px 38px #000b}',
    '#cobalt-hx .hx-launch strong{display:inline-block;padding:3px 7px;margin-right:12px;border-radius:7px;background:#113c41;color:var(--hx-cyan);letter-spacing:1px}',
    '#cobalt-hx .hx-launch:focus{background:#234955}',
    '#cobalt-hx .hx-overlay{position:absolute;inset:0;background:#0009;pointer-events:auto}',
    '#cobalt-hx .hx-overlay[hidden],#cobalt-hx .hx-launch[hidden],#cobalt-hx .hx-signature[hidden]{display:none!important}',
    '#cobalt-hx .hx-panel{position:absolute;left:50%;top:50%;transform:translate(-50%,-50%);width:660px;max-width:88vw;max-height:90vh;overflow:auto;padding:34px 42px 28px;border:2px solid var(--hx-border);border-radius:25px;background:linear-gradient(140deg,#09141df9,#172732fa 75%,#221d1af7);box-shadow:0 30px 100px #000e}',
    '#cobalt-hx .hx-panel:before{content:"";position:absolute;top:0;left:40px;right:40px;height:4px;background:linear-gradient(90deg,var(--hx-bronze),var(--hx-cyan))}',
    '#cobalt-hx .hx-title{font-size:38px;margin:0 0 5px;font-weight:800;letter-spacing:.8px}',
    '#cobalt-hx .hx-title span{color:var(--hx-cyan)}',
    '#cobalt-hx .hx-subtitle{font-size:19px;color:#b6c9cb;margin:0 0 22px}',
    '#cobalt-hx .hx-state{font-size:19px;color:#e6e5e1;border-left:4px solid var(--hx-bronze);padding:10px 13px;margin:0 0 14px;min-height:42px;background:#25394670}',
    '#cobalt-hx .hx-action{display:block;width:100%;text-align:left;color:#edf7f8;background:#1a303d;border:1px solid #7a959d77;border-radius:12px;font-size:23px;font-weight:650;margin-top:10px;padding:15px 19px}',
    '#cobalt-hx .hx-action.primary{background:#15484c;border-color:#74ddd4a8}',
    '#cobalt-hx .hx-action:focus{background:#28606c;transform:translateX(3px)}',
    '#cobalt-hx .hx-footer{font-size:15px;color:#9eb5b9;margin:19px 0 0;line-height:1.5}',
    'html.hx-effects-on ytlr-video-renderer:focus-within,html.hx-effects-on ytlr-grid-video-renderer:focus-within,html.hx-effects-on ytlr-rich-grid-media:focus-within,html.hx-effects-on ytlr-tile-renderer:focus-within{outline:3px solid var(--hx-cyan)!important;outline-offset:5px;border-radius:14px;box-shadow:0 0 0 8px #69e6dc26,0 16px 32px #0009!important}',
    'html.hx-effects-on ytlr-video-renderer:focus-within img,html.hx-effects-on ytlr-grid-video-renderer:focus-within img{filter:saturate(1.17) contrast(1.06);border-radius:13px}',
    'html.hx-effects-on ytlr-guide-entry-renderer:focus-within,html.hx-effects-on ytlr-guide-item-renderer:focus-within{background:#69e6dc22!important;border-left:4px solid var(--hx-bronze);border-radius:8px}',
    'html.hx-effects-on ytlr-search-input:focus-within,html.hx-effects-on [role=searchbox]:focus{box-shadow:0 0 0 2px var(--hx-bronze)!important;border-radius:20px}',
    'html.hx-effects-on ytlr-menu-service-item-renderer:focus-within,html.hx-effects-on ytlr-menu-navigation-item-renderer:focus-within{background:#69e6dc25!important;border-radius:8px}',
    'html.hx-effects-on ytlr-miniplayer-renderer,html.hx-effects-on ytlr-miniplayer{box-shadow:0 0 0 2px var(--hx-bronze),0 14px 48px #000b;border-radius:13px}',
    'html.hx-effects-on.hx-cinema ytlr-video-renderer:focus-within,html.hx-effects-on.hx-cinema ytlr-grid-video-renderer:focus-within{transform:translateY(-4px);transition:transform .2s ease-out,box-shadow .2s ease-out}',
    '@media(prefers-reduced-motion:reduce){#cobalt-hx .hx-action:focus,html.hx-effects-on.hx-cinema ytlr-video-renderer:focus-within,html.hx-effects-on.hx-cinema ytlr-grid-video-renderer:focus-within{transition:none;transform:none}}'
  ].join('\n');

  let root, style, signature, accent, launcher, overlay, panel, status, buttons = [];
  let poll, keyListener, routeListener, retryTimer;
  const element = (tag, cls, value) => {
    const node = document.createElement(tag);
    if (cls) node.className = cls;
    if (typeof value === 'string') node.textContent = value;
    return node;
  };
  const save = () => {
    try { window.localStorage.setItem(STORAGE, JSON.stringify(settings)); } catch (_) {}
  };
  const report = message => {
    if (status) status.textContent = String(message || '');
  };
  const watchingFullscreen = () => {
    try {
      const video = document.querySelector('video');
      return !!(video && video.readyState && !video.ended && window.isPipPlaying !== true &&
        /(?:^#?\/|\/)watch(?:[?/#]|$)/.test(location.hash || ''));
    } catch (_) { return false; }
  };
  const hasMini = () => {
    try { return window.isPipPlaying === true; } catch (_) { return false; }
  };
  function updateVisibility() {
    if (!root || !overlay) return;
    const hide = watchingFullscreen() || hasMini();
    if (signature) signature.hidden = hide;
    if (launcher) launcher.hidden = hide || !overlay.hidden;
    if (accent) accent.hidden = hide;
  }
  function repaint() {
    const html = document.documentElement;
    if (!html || !html.classList) return;
    html.classList.toggle('hx-effects-on', settings.effects);
    html.classList.toggle('hx-cinema', settings.effects && settings.intensity === 'cinema');
    html.classList.toggle('hx-theme-cyan', settings.theme === 'cyan');
    if (buttons[1]) buttons[1].textContent = 'Efeitos visuais: ' + (settings.effects ? 'LIGADOS' : 'DESLIGADOS');
    if (buttons[2]) buttons[2].textContent = 'Paleta: ' + (settings.theme === 'bronze' ? 'BRONZE IMPERIAL' : 'TURQUESA');
    if (buttons[3]) buttons[3].textContent = 'Animações: ' + (settings.intensity === 'cinema' ? 'CINEMA' : 'LEVES');
  }
  function open() {
    if (!overlay) return;
    overlay.hidden = false;
    overlay.style.display = 'block';
    updateVisibility();
    report('Cobalt HX ' + VERSION + ' • edição personalizada ativa.');
    if (buttons[0] && typeof buttons[0].focus === 'function') buttons[0].focus();
  }
  function close() {
    if (!overlay) return;
    overlay.hidden = true;
    overlay.style.display = 'none';
    updateVisibility();
    if (launcher && !launcher.hidden && typeof launcher.focus === 'function') launcher.focus();
  }
  function refresh() {
    const control = window.TTBackExternal;
    if (!control || typeof control.refreshVisual !== 'function') {
      report('Atualização visual indisponível nesta instalação.');
      return;
    }
    report('Verificando a última versão do visual...');
    try { control.refreshVisual(); } catch (_) { report('Falha na verificação. Visual atual preservado.'); }
  }
  function action(text, handler, primary) {
    const button = element('button', 'hx-action' + (primary ? ' primary' : ''), text);
    button.type = 'button';
    button.addEventListener('click', handler);
    panel.appendChild(button);
    buttons.push(button);
    return button;
  }
  function mount() {
    if (!document.body || !document.head) return false;
    style = element('style');
    style.id = 'cobalt-hx-css';
    const nonceSource = document.querySelector('style[nonce],script[nonce]');
    if (nonceSource && nonceSource.nonce) style.nonce = nonceSource.nonce;
    style.textContent = css;
    document.head.appendChild(style);

    root = element('div');
    root.id = KEY;
    signature = element('div', 'hx-signature');
    const name = element('span', 'hx-name', 'COBALT ');
    name.appendChild(element('b', '', 'HX'));
    signature.appendChild(name);
    signature.appendChild(element('span', 'hx-tag', 'PERSONAL TV'));
    accent = element('div', 'hx-accent');
    launcher = element('button', 'hx-launch');
    launcher.type = 'button';
    launcher.appendChild(element('strong', '', 'HX'));
    launcher.appendChild(document.createTextNode(' Atualizar e personalizar'));
    launcher.setAttribute('aria-label', 'Abrir configurações do Cobalt HX');
    launcher.addEventListener('click', open);
    overlay = element('div', 'hx-overlay');
    overlay.hidden = true;
    overlay.setAttribute('role', 'dialog');
    overlay.setAttribute('aria-label', 'Configurações Cobalt HX');
    panel = element('section', 'hx-panel');
    const title = element('h2', 'hx-title');
    title.appendChild(document.createTextNode('COBALT '));
    title.appendChild(element('span', '', 'HX'));
    panel.appendChild(title);
    panel.appendChild(element('p', 'hx-subtitle', 'Sua experiência de TV, do seu jeito • v' + VERSION));
    status = element('div', 'hx-state', 'Visual personalizado ativo');
    status.setAttribute('role', 'status');
    panel.appendChild(status);
    action('↻ Atualizar interface agora', refresh, true);
    action('', () => { settings.effects = !settings.effects; save(); repaint(); report('Efeitos atualizados.'); });
    action('', () => { settings.theme = settings.theme === 'bronze' ? 'cyan' : 'bronze'; save(); repaint(); report('Paleta aplicada.'); });
    action('', () => { settings.intensity = settings.intensity === 'cinema' ? 'leve' : 'cinema'; save(); repaint(); report('Intensidade de animação alterada.'); });
    action('Reiniciar aplicativo', () => {
      report('Recarregando a interface do Cobalt...');
      if (typeof location.reload === 'function') location.reload();
    });
    action('Fechar painel', close);
    panel.appendChild(element('p', 'hx-footer', 'Atualizações visuais são remotas. Atualizações do Android exigem APK assinado compatível. OK seleciona; Voltar fecha o painel.'));
    overlay.appendChild(panel);
    root.appendChild(signature);
    root.appendChild(accent);
    root.appendChild(launcher);
    root.appendChild(overlay);
    document.body.appendChild(root);

    // Critical fallbacks when the YouTube TV Content Security Policy blocks style sheets.
    Object.assign(root.style, { position: 'fixed', inset: '0', zIndex: '2147483000', pointerEvents: 'none' });
    Object.assign(launcher.style, { position: 'absolute', right: '34px', bottom: '28px', padding: '15px 23px',
      fontSize: '23px', color: 'white', background: '#102e3b', border: '2px solid #69e6dc', borderRadius: '18px',
      pointerEvents: 'auto' });
    Object.assign(signature.style, { position: 'absolute', top: '18px', left: '52%', color: 'white',
      padding: '10px 20px', background: '#101d27', border: '1px solid #d6a46e' });
    Object.assign(overlay.style, { position: 'absolute', inset: '0', background: '#000a', pointerEvents: 'auto' });
    Object.assign(panel.style, { position: 'absolute', top: '50%', left: '50%', transform: 'translate(-50%,-50%)',
      width: '660px', maxWidth: '88vw', padding: '30px', border: '2px solid #d6a46e',
      borderRadius: '22px', background: '#0c1924', color: '#fff' });
    // HTML hidden must be respected even if CSS is blocked.
    overlay.style.display = 'none';

    repaint();
    updateVisibility();
    poll = setInterval(updateVisibility, 1250);
    keyListener = event => {
      if (event.type !== 'keydown') return;
      const back = [4, 27, 166, 461, 10009].includes(event.keyCode) ||
        ['Escape', 'BrowserBack', 'GoBack'].includes(event.key);
      if (!overlay.hidden) {
        if (back) { event.preventDefault(); event.stopImmediatePropagation(); close(); return; }
        if ([19, 20, 38, 40].includes(event.keyCode) || ['ArrowUp','ArrowDown'].includes(event.key)) {
          event.preventDefault(); event.stopImmediatePropagation();
          let i = buttons.indexOf(document.activeElement);
          if (i < 0) i = 0;
          const step = event.keyCode === 19 || event.keyCode === 38 || event.key === 'ArrowUp' ? -1 : 1;
          i = (i + step + buttons.length) % buttons.length;
          buttons[i].focus();
          return;
        }
      } else if (event.keyCode === 406 && !watchingFullscreen() && !hasMini()) {
        event.preventDefault(); event.stopImmediatePropagation(); open();
      }
    };
    window.addEventListener('keydown', keyListener, true);
    return true;
  }
  function destroy() {
    if (poll) clearInterval(poll);
    if (retryTimer) clearTimeout(retryTimer);
    if (keyListener) window.removeEventListener('keydown', keyListener, true);
    if (root && root.parentNode) root.parentNode.removeChild(root);
    if (style && style.parentNode) style.parentNode.removeChild(style);
    const html = document.documentElement;
    if (html && html.classList) {
      html.classList.remove('hx-effects-on');
      html.classList.remove('hx-cinema');
      html.classList.remove('hx-theme-cyan');
    }
  }
  const api = window.CobaltHX = { version: VERSION, open, close, destroy, isOpen: () => !!(overlay && !overlay.hidden), setStatus: report, refresh,
    get preferences() { return { effects: settings.effects, theme: settings.theme, intensity: settings.intensity }; } };
  if (!mount()) {
    document.addEventListener('DOMContentLoaded', () => { if (window.CobaltHX === api) mount(); }, { once: true });
  }
})();
