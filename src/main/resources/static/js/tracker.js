(function () {
  'use strict';

  var POLICY_VERSION = '2026-11-01';
  var CONSENT_KEY = 'eshop_consent';
  var QUEUE_KEY = 'eshop_tracking_queue';
  var SESSION_KEY = 'eshop_tracking_session';
  var ANONYMOUS_KEY = 'eshop_anonymous_id';
  var DWELL_THRESHOLD_MS = 5000;
  var queue = [];
  var detail = location.pathname.match(/^\/products\/(\d+)/);
  if (window.eshopTrackingEnabled === false) return;

  function consent() {
    try { return JSON.parse(localStorage.getItem(CONSENT_KEY) || 'null'); } catch (_) { return null; }
  }
  function enabled() {
    var value = consent();
    return value && value.policyVersion === POLICY_VERSION && value.analytics === true;
  }
  function id(key, storage) {
    var value = storage.getItem(key);
    if (!value) {
      value = crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(16).slice(2);
      storage.setItem(key, value);
    }
    return value;
  }
  function loadQueue() {
    try { queue = JSON.parse(localStorage.getItem(QUEUE_KEY) || '[]'); } catch (_) { queue = []; }
  }
  function saveQueue() {
    localStorage.setItem(QUEUE_KEY, JSON.stringify(queue.slice(-100)));
  }
  function add(type, productId, valueMs, element) {
    if (!enabled() || !productId) return;
    queue.push({
      eventId: crypto.randomUUID ? crypto.randomUUID() : Date.now() + '-' + Math.random().toString(16).slice(2),
      sessionId: id(SESSION_KEY, sessionStorage),
      anonymousId: id(ANONYMOUS_KEY, localStorage),
      productId: Number(productId),
      type: type,
      valueMs: valueMs || null,
      consentState: 'analytics',
      schemaVersion: '1.0',
      contextJson: JSON.stringify({ page: location.pathname, target: element && element.tagName || null }),
      timestamp: new Date().toISOString()
    });
    saveQueue();
    flush();
  }
  function flush() {
    if (!enabled() || !queue.length || !navigator.onLine) return;
    var batch = queue.slice(0, 20);
    fetch('/api/track/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(batch),
      keepalive: true
    }).then(function (response) {
      if (!response.ok) throw new Error('tracking request failed');
      queue.splice(0, batch.length);
      saveQueue();
      if (queue.length) flush();
    }).catch(function () {});
  }
  function finishDwell() {
    if (!detail || !enabled() || dwellSent) return;
    if (dwellStarted !== null) dwellElapsed += Date.now() - dwellStarted;
    dwellStarted = null;
    dwellSent = true;
    if (dwellElapsed >= DWELL_THRESHOLD_MS) add('DWELL', detail[1], Math.min(dwellElapsed, 1800000));
  }

  var dwellStarted = detail && document.visibilityState === 'visible' ? Date.now() : null;
  var dwellElapsed = 0;
  var dwellSent = false;
  loadQueue();
  if (!enabled()) return;

  document.addEventListener('click', function (event) {
    var link = event.target.closest('[data-product-id]');
    if (link) add('CLICK', link.dataset.productId, null, link);
  });
  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'hidden') {
      if (dwellStarted !== null) dwellElapsed += Date.now() - dwellStarted;
      dwellStarted = null;
    } else if (dwellStarted === null) {
      dwellStarted = Date.now();
    }
  });
  window.addEventListener('online', flush);
  window.addEventListener('pagehide', function () {
    finishDwell();
    flush();
  });
  window.addEventListener('storage', function (event) {
    if (event.key === CONSENT_KEY && !enabled()) {
      queue = [];
      localStorage.removeItem(QUEUE_KEY);
      window.location.reload();
    }
  });
  window.addEventListener('eshop:consent-changed', function () {
    if (!enabled()) {
      queue = [];
      localStorage.removeItem(QUEUE_KEY);
      window.location.reload();
    } else {
      flush();
    }
  });
  flush();
})();
