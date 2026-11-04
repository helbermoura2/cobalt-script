/* Personal Cobalt controller. Loads the unchanged official TizenTube userscript.
 * No account data or diagnostics are transmitted by this controller.
 */
(() => {
  if (window.TTBackExternal) return;
  const state = window.TTBackExternal = { version: 4, upstreamLoaded: false, lastBack: null };
  const vendor = 'https://cdn.jsdelivr.net/npm/@foxreis/tizentube/dist/userScript.js';
  let pressed = false;
  let badge;
  let badgeTimer;
  function notice(text, milliseconds = 5000) {
    if (!badge) {
      badge = document.createElement('div');
      badge.id = 'tt-personal-back-status';
      badge.setAttribute('role', 'status');
      Object.assign(badge.style, { position: 'fixed', top: '24px', right: '24px',
        zIndex: '2147483647', color: '#fff', background: '#202124', padding: '16px',
        borderRadius: '8px', font: '24px sans-serif', maxWidth: '80vw', pointerEvents: 'none' });
      (document.body || document.documentElement).appendChild(badge);
    }
    clearTimeout(badgeTimer);
    badge.textContent = text;
    badge.style.display = 'block';
    badgeTimer = setTimeout(() => { badge.style.display = 'none'; }, milliseconds);
  }
  function router() {
    return Object.values(window._yttv || {}).map(x => x && x.instance)
      .find(x => x && typeof x.resolveCommand === 'function');
  }
  function watchPage(player) {
    if (/(?:^#?\/|\/)watch(?:[?/#]|$)/.test(location.hash)) return true;
    return player && ['ytLrPlayerEnabled', 'ExC9Fb'].some(name => player.classList.contains(name));
  }
  function visibleDialog(player) {
    return [...document.querySelectorAll('[role="dialog"],ytlr-overlay-section-renderer')]
      .some(node => node !== player && !node.contains(player) && node.getClientRects().length);
  }
  function back(event) {
    if (![4, 27, 166, 461, 10009].includes(event.keyCode)
      && !['Escape', 'BrowserBack', 'GoBack'].includes(event.key)) return;
    const stop = () => { event.preventDefault(); event.stopImmediatePropagation(); };
    if (pressed) {
      stop();
      if (event.type === 'keyup') pressed = false;
      return;
    }
    if (event.type !== 'keydown' || event.repeat) return;
    const player = document.querySelector('ytlr-player');
    const video = document.querySelector('video');
    state.lastBack = { keyCode: event.keyCode, key: event.key,
      playerClass: player && player.className, hash: location.hash,
      pip: window.isPipPlaying, videoReady: video && video.readyState };
    if (window.isPipPlaying !== false || !video || !video.readyState || video.ended
      || document.querySelector('input:focus,textarea:focus,[contenteditable="true"]:focus')
      || !watchPage(player) || visibleDialog(player)) return;
    const client = router();
    if (!client) return;
    try {
      if (!client.resolveCommand({ customAction: { action: 'ENTER_MP' } })) return;
      pressed = true;
      stop();
    } catch (error) {
      state.lastBack.error = String(error && error.message || error);
      notice('Voltar: não foi possível abrir o miniplayer.');
    }
  }
  for (const type of ['keydown', 'keypress', 'keyup']) addEventListener(type, back, true);
  notice('Cobalt pessoal v4: carregando…');
  try {
    const url = vendor + '?v=' + Date.now();
    const script = document.createElement('script');
    const nonceSource = document.querySelector('script[nonce]');
    if (nonceSource && nonceSource.nonce) script.nonce = nonceSource.nonce;
    if (window.trustedTypes) {
      const policy = window.trustedTypes.createPolicy('tt-personal-upstream-v4', {
        createScriptURL(value) {
          if (value !== url) throw new TypeError('Unexpected upstream URL');
          return value;
        },
      });
      script.src = policy.createScriptURL(url);
    } else script.src = url;
    script.onload = () => {
      state.upstreamLoaded = true;
      notice('Cobalt pessoal v4 carregado.');
    };
    script.onerror = () => { state.upstreamError = true; notice('Falha ao carregar o TizenTube.', 15000); };
    (document.head || document.documentElement).appendChild(script);
  } catch (error) {
    state.upstreamError = String(error && error.message || error);
    notice('Falha ao iniciar o TizenTube.', 15000);
  }
})();
