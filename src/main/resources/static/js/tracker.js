(function () {
  if (localStorage.getItem('eshop_consent') !== 'yes') return;

  function send(type, productId, valueMs) {
    if (!productId) return;
    var body = JSON.stringify({ productId: Number(productId), type: type, valueMs: valueMs || null });
    navigator.sendBeacon('/api/track', new Blob([body], { type: 'application/json' }));
  }

  document.addEventListener('click', function (event) {
    var link = event.target.closest('[data-product-id]');
    if (link) send('CLICK', link.dataset.productId);
  });

  var detail = location.pathname.match(/^\/products\/(\d+)/);
  if (!detail) return;
  var started = document.visibilityState === 'visible' ? Date.now() : null;
  var elapsed = 0;
  var sent = false;

  document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'hidden') {
      if (started !== null) elapsed += Date.now() - started;
      started = null;
      return;
    }
    if (started === null) started = Date.now();
  });

  function finish() {
    if (sent) return;
    if (started !== null) elapsed += Date.now() - started;
    sent = true;
    if (elapsed >= 3000) send('DWELL', detail[1], Math.min(elapsed, 1800000));
  }
  addEventListener('pagehide', finish, { once: true });
})();
