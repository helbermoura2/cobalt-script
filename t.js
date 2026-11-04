/* Personal Cobalt controller. Loads the unchanged official TizenTube userscript.
 * No account data or diagnostics are transmitted by this controller.
 */
(() => {
  if (window.TTBackExternal) return;
  const state = window.TTBackExternal = { version: 4, revision: 5, upstreamLoaded: false, lastBack: null };
  const vendor = 'https://cdn.jsdelivr.net/npm/@foxreis/tizentube/dist/userScript.js';
  let pressed = false;
  let badge;
  let badgeTimer;
  const holdMilliseconds = 650;
  let okPress = null;
  let replayingOk = false;
  let suppressKeyboardClickUntil = 0;
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
  function restoreInfo() {
    if (window.isPipPlaying !== true || document.hidden) return null;
    const video = document.querySelector('video');
    const client = router();
    if (!video || !video.readyState || video.ended || video.isConnected === false || !Number.isFinite(video.currentTime) || !client) return null;
    for (const registry of Object.values(window._yttv || {})) {
      const mappings = registry && registry.mappings;
      const locator = registry && typeof registry.get === 'function' ? registry : mappings;
      if (!locator || typeof locator.get !== 'function') continue;
      try {
        const service = locator.get('PlayerService');
        const config = service && service.loadedPlaybackConfig;
        if (config && config.watchEndpoint && config.watchEndpoint.videoId) return { video, client, config };
      } catch (_) { /* Another registry may contain the service. */ }
    }
    return null;
  }
  function restoreVideo(expectedVideo) {
    const info = restoreInfo();
    if (!info || info.video !== expectedVideo) return false;
    const { video, client, config } = info;
    const player = document.querySelector('ytlr-player');
    const previousBackground = player && player.style.backgroundColor;
    if (player) player.style.backgroundColor = 'rgb(0, 0, 0)';
    try {
      const result = client.resolveCommand({ clickTrackingParams: config.clickTrackingParams,
        commandMetadata: config.commandMetadata,
        watchEndpoint: { ...config.watchEndpoint, startTimeSeconds: Math.floor(video.currentTime) } });
      if (result === false) throw new Error('Fullscreen command rejected');
      window.isPipPlaying = false;
      return true;
    } catch (error) {
      if (player) player.style.backgroundColor = previousBackground;
      state.restoreError = String(error && error.message || error);
      return false;
    }
  }
  function keySnapshot(event) {
    const copy = {};
    for (const field of ['key', 'code', 'keyCode', 'which', 'charCode', 'location',
      'altKey', 'ctrlKey', 'shiftKey', 'metaKey']) copy[field] = event[field];
    return copy;
  }
  function replayShortOk(press, release) {
    replayingOk = true;
    try {
      const target = press.target && press.target.isConnected !== false ? press.target : document.activeElement || document;
      const events = [['keydown', press.down], ...press.keypress.map(value => ['keypress', value]), ['keyup', release]];
      for (const [type, init] of events) {
        const event = new KeyboardEvent(type, { ...init, bubbles: true, cancelable: true, composed: true });
        for (const field of ['keyCode', 'which', 'charCode']) {
          if (init[field] !== undefined && event[field] !== init[field]) Object.defineProperty(event, field, { value: init[field] });
        }
        target.dispatchEvent(event);
      }
    } finally { replayingOk = false; }
  }
  function ok(event) {
    if (replayingOk) return;
    const isOk = [13, 23, 32].includes(event.keyCode) || ['Enter', 'Select', 'Accept'].includes(event.key);
    if (!isOk) {
      if (event.type === 'keydown' && okPress) {
        clearTimeout(okPress.timer);
        okPress.cancelled = true;
      }
      return;
    }
    const stop = () => { event.preventDefault(); event.stopImmediatePropagation(); };
    if (okPress) {
      stop();
      if (event.type === 'keypress' && !event.repeat && !okPress.keypress.length) okPress.keypress.push(keySnapshot(event));
      if (event.type === 'keyup') {
        const press = okPress;
        okPress = null;
        clearTimeout(press.timer);
        if (press.long || press.cancelled) suppressKeyboardClickUntil = Date.now() + 300;
        else replayShortOk(press, keySnapshot(event));
      }
      return;
    }
    if (event.type !== 'keydown' || event.repeat) return;
    suppressKeyboardClickUntil = 0;
    const info = restoreInfo();
    if (!info || document.querySelector('input:focus,textarea:focus,[contenteditable="true"]:focus')
      || visibleDialog(document.querySelector('ytlr-player'))) return;
    stop();
    const press = okPress = { down: keySnapshot(event), target: event.target || document.activeElement || document,
      keypress: [], video: info.video, long: false, cancelled: false };
    press.timer = setTimeout(() => {
      if (okPress !== press || press.cancelled) return;
      if (document.querySelector('input:focus,textarea:focus,[contenteditable="true"]:focus')
        || visibleDialog(document.querySelector('ytlr-player'))) {
        press.cancelled = true;
        return;
      }
      press.long = true;
      if (!restoreVideo(press.video)) notice('Não foi possível voltar ao vídeo.');
    }, holdMilliseconds);
  }
  addEventListener('contextmenu', event => {
    if (!replayingOk && (okPress || Date.now() < suppressKeyboardClickUntil)) {
      event.preventDefault(); event.stopImmediatePropagation();
    }
  }, true);
  addEventListener('click', event => {
    if (!replayingOk && event.detail === 0 && (okPress || Date.now() < suppressKeyboardClickUntil)) {
      event.preventDefault(); event.stopImmediatePropagation();
    }
  }, true);
  addEventListener('blur', event => {
    if (event.target !== window) return;
    if (okPress) clearTimeout(okPress.timer);
    okPress = null;
    suppressKeyboardClickUntil = 0;
  }, true);
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
  for (const type of ['keydown', 'keypress', 'keyup']) {
    addEventListener(type, ok, true);
    addEventListener(type, back, true);
  }
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
      notice('Cobalt pessoal v4 carregado — segure OK para tela cheia.');
    };
    script.onerror = () => { state.upstreamError = true; notice('Falha ao carregar o TizenTube.', 15000); };
    (document.head || document.documentElement).appendChild(script);
  } catch (error) {
    state.upstreamError = String(error && error.message || error);
    notice('Falha ao iniciar o TizenTube.', 15000);
  }
})();
