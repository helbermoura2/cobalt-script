/* Cobalt HX — lightweight TV visual layer. No React, telemetry or extra dependencies. */
(() => {
  'use strict';
  const VERSION = '0.1.0';
  const old = window.CobaltHX;
  if (old && old.version === VERSION) return;
  if (old && typeof old.destroy === 'function') old.destroy();

  const ID = 'cobalt-hx';
  const SETTINGS_KEY = 'cobalt-hx-visuals';
  let root, sheet, launcher, panel, status, effects, timer, onKeys;
  let enabled = true;
  try { enabled = window.localStorage.getItem(SETTINGS_KEY) !== 'off'; } catch (_) { /* Private browsing. */ }
  const css = [
    '#cobalt-hx{position:fixed;right:26px;bottom:28px;z-index:2147483500;font-family:Arial,Helvetica,sans-serif;color:#f3f6f7;pointer-events:none}',
    '#cobalt-hx *{box-sizing:border-box}',
    '#cobalt-hx button{font:inherit;cursor:pointer;pointer-events:auto;outline:none}',
    '#cobalt-hx button:focus{outline:3px solid #5be3dc;outline-offset:3px}',
    '#cobalt-hx .hx-launch{min-height:54px;border:1px solid rgba(85,225,216,.52);border-radius:14px;padding:11px 18px;background:#0a1218;color:#eaf7f6;box-shadow:0 8px 24px rgba(0,0,0,.55);font-weight:bold;font-size:19px;letter-spacing:.5px}',
    '#cobalt-hx .hx-glyph{color:#63e4da;font-weight:900;margin-right:8px}',
    '#cobalt-hx .hx-panel{position:absolute;right:0;bottom:68px;width:340px;max-width:90vw;border:1px solid rgba(81,221,212,.32);border-radius:18px;padding:22px;background:#101820;box-shadow:0 18px 55px rgba(0,0,0,.68);pointer-events:auto;animation:hx-enter .22s ease-out}',
    '#cobalt-hx .hx-panel[hidden]{display:none}',
    '#cobalt-hx .hx-heading{margin:0 0 4px;font-size:26px;font-weight:700;letter-spacing:.4px}',
    '#cobalt-hx .hx-sub{font-size:14px;color:#aac0c5;margin:0 0 15px}',
    '#cobalt-hx .hx-status{font-size:15px;line-height:1.5;min-height:42px;color:#c7d9dc;margin:0 0 12px}',
    '#cobalt-hx .hx-action{display:block;width:100%;text-align:left;border:1px solid rgba(255,255,255,.19);border-radius:11px;background:#1d2a34;color:#fff;padding:13px 15px;margin-top:10px;font-size:17px}',
    '#cobalt-hx .hx-action.hx-primary{background:#144b51;border-color:#459c99}',
    '#cobalt-hx .hx-action:focus{background:#22575d}',
    '#cobalt-hx .hx-foot{margin:17px 0 0;color:#8babb1;font-size:12px;line-height:1.45}',
    'html.hx-visuals-on ytlr-video-renderer:focus-within img,html.hx-visuals-on ytlr-grid-video-renderer:focus-within img{filter:brightness(1.06) saturate(1.08);transition:filter .18s ease}',
    'html.hx-visuals-on ytlr-video-renderer:focus-within,html.hx-visuals-on ytlr-grid-video-renderer:focus-within{outline:2px solid rgba(85,225,216,.65);outline-offset:4px;transition:outline-color .18s ease}',
    '@keyframes hx-enter{from{opacity:0;transform:translateY(12px)}to{opacity:1;transform:translateY(0)}}',
    '@media(prefers-reduced-motion:reduce){#cobalt-hx .hx-panel{animation:none}html.hx-visuals-on ytlr-video-renderer:focus-within img,html.hx-visuals-on ytlr-grid-video-renderer:focus-within img{transition:none}}'
  ].join('\n');

  function el(name, className, text) {
    const node = document.createElement(name);
    if (className) node.className = className;
    if (typeof text === 'string') node.textContent = text;
    return node;
  }
  function persist() {
    try { window.localStorage.setItem(SETTINGS_KEY, enabled ? 'on' : 'off'); } catch (_) {}
  }
  function updateEffects() {
    document.documentElement.classList.toggle('hx-visuals-on', enabled);
    if (effects) effects.textContent = 'Efeitos visuais: ' + (enabled ? 'LIGADOS' : 'DESLIGADOS');
  }
  function setStatus(message) {
    if (status) status.textContent = String(message || '');
  }
  function isWatching() {
    try {
      const video = document.querySelector('video');
      return !!(video && video.readyState && !video.ended && window.isPipPlaying !== true && /\/watch(?:[?/#]|$)/.test(location.hash || ''));
    } catch (_) { return false; }
  }
  function refreshVisibility() {
    if (root && launcher) {
      const watching = isWatching();
      launcher.hidden = watching;
      if (watching && panel) panel.hidden = true;
    }
  }
  function togglePanel(force) {
    if (!panel) return;
    panel.hidden = typeof force === 'boolean' ? !force : !panel.hidden;
    launcher.setAttribute('aria-expanded', String(!panel.hidden));
    if (!panel.hidden) {
      setStatus('Interface HX v' + VERSION + ' pronta para uso.');
      const first = panel.querySelector('button');
      if (first && typeof first.focus === 'function') first.focus();
    } else if (launcher && typeof launcher.focus === 'function') launcher.focus();
  }
  function reloadVisual() {
    const external = window.TTBackExternal;
    if (!external || typeof external.refreshVisual !== 'function') {
      setStatus('Atualização indisponível nesta versão do controlador.');
      return;
    }
    setStatus('Procurando a versão visual mais recente...');
    external.refreshVisual();
  }
  function makeButton(label, action, klass) {
    const button = el('button', 'hx-action' + (klass ? ' ' + klass : ''), label);
    button.type = 'button';
    button.addEventListener('click', action);
    return button;
  }
  function mount() {
    if (!document.body || !document.head) return false;
    sheet = el('style');
    sheet.id = 'cobalt-hx-css';
    sheet.textContent = css;
    document.head.appendChild(sheet);
    root = el('div');
    root.id = ID;
    launcher = el('button', 'hx-launch');
    launcher.type = 'button';
    launcher.setAttribute('aria-label', 'Abrir opções e atualizações Cobalt HX');
    launcher.setAttribute('aria-expanded', 'false');
    const glyph = el('span', 'hx-glyph', 'HX');
    launcher.appendChild(glyph);
    launcher.appendChild(document.createTextNode('  Atualizar'));
    launcher.addEventListener('click', () => togglePanel(true));
    panel = el('section', 'hx-panel');
    panel.hidden = true;
    panel.setAttribute('role', 'dialog');
    panel.setAttribute('aria-label', 'Cobalt HX — opções');
    panel.appendChild(el('h2', 'hx-heading', 'Cobalt HX'));
    panel.appendChild(el('p', 'hx-sub', 'Sua edição pessoal para TV'));
    status = el('p', 'hx-status');
    status.setAttribute('role', 'status');
    panel.appendChild(status);
    panel.appendChild(makeButton('↻ Verificar e aplicar visual', reloadVisual, 'hx-primary'));
    effects = makeButton('', () => { enabled = !enabled; persist(); updateEffects(); setStatus('Preferência salva.'); });
    panel.appendChild(effects);
    panel.appendChild(makeButton('Reiniciar interface', () => {
      setStatus('Reiniciando a página, sem reinstalar o APK.');
      location.reload();
    }));
    panel.appendChild(makeButton('Fechar', () => togglePanel(false)));
    panel.appendChild(el('p', 'hx-foot', 'O botão atualiza o visual remoto. Atualizações do motor Android exigem um pacote assinado compatível.'));
    root.appendChild(panel);
    root.appendChild(launcher);
    document.body.appendChild(root);
    updateEffects();
    refreshVisibility();
    timer = setInterval(refreshVisibility, 1800);
    onKeys = event => {
      const back = [4, 27, 166, 461, 10009].includes(event.keyCode) || ['Escape', 'BrowserBack', 'GoBack'].includes(event.key);
      if (!panel.hidden && back && event.type === 'keydown') {
        event.preventDefault(); event.stopImmediatePropagation(); togglePanel(false);
      } else if (panel.hidden && !isWatching() && event.type === 'keydown' && event.keyCode === 406) {
        // Blue key on remotes that have one; on-screen button works without it.
        event.preventDefault(); event.stopImmediatePropagation(); togglePanel(true);
      }
    };
    window.addEventListener('keydown', onKeys, true);
    return true;
  }
  function destroy() {
    if (timer) clearInterval(timer);
    if (onKeys) window.removeEventListener('keydown', onKeys, true);
    if (root && root.parentNode) root.parentNode.removeChild(root);
    if (sheet && sheet.parentNode) sheet.parentNode.removeChild(sheet);
    document.documentElement.classList.remove('hx-visuals-on');
  }
  const api = window.CobaltHX = { version: VERSION, destroy, setStatus, open: () => togglePanel(true) };
  if (!mount()) {
    document.addEventListener('DOMContentLoaded', () => { if (window.CobaltHX === api) mount(); }, { once: true });
  }
})();
