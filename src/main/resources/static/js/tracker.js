(function () {
  // 1) Chưa đồng ý cookie thì không theo dõi gì cả
  if (localStorage.getItem('eshop_consent') !== 'yes') return;

  var sid = sessionStorage.getItem('eshop_sid');
  if (!sid) { sid = crypto.randomUUID();
      sessionStorage.setItem('eshop_sid', sid); }
  var queue = [], start = Date.now(), sent = {};

  function track(type, data) {
    queue.push(Object.assign({ type: type, page: location.pathname }, data
        || {}));
    if (queue.length >= 5) flush();
  }
  // sendBeacon vẫn gửi được khi khách đóng tab / chuyển trang
  function flush() {
    if (!queue.length) return;
    var body = JSON.stringify({ sessionId: sid, events: queue.splice(0)
        });
    navigator.sendBeacon('/api/track', new Blob([body], { type:
        'application/json' }));
  }

  track('page_view');
  var m = location.pathname.match(/^\/products\/(\d+)/);
  if (m) track('view_product', { productId: Number(m[1]) });
  if (location.pathname === '/checkout') track('begin_checkout');

  // 2) Tìm kiếm trên shop/index.html (form.filters)
  var f = document.querySelector('form.filters');
  if (f) f.addEventListener('submit', function () {
    track('search', { keyword: f.keyword.value, categoryId:
        f.categoryId.value });
  });

  // 3) Thêm vào giỏ: form POST /cart/add/{id} (trang sẽ chuyển hướng
  // ngay)
  document.querySelectorAll('form[action*="/cart/add/"]').forEach(function
      (form) {
    form.addEventListener('submit', function () {
      var id = Number(form.action.match(/add\/(\d+)/)[1]);
      track('add_to_cart', { productId: id, quantity:
          Number(form.quantity.value) });
      flush();
    });
  });

  // 4) Cuộn trang: ghi mốc 50% và 100%
  window.addEventListener('scroll', function () {
    var p = (scrollY + innerHeight) /
        document.documentElement.scrollHeight;
    [0.5, 1].forEach(function (t) {
      if (p >= t * 0.98 && !sent[t]) { sent[t] = 1; track('scroll', {
          depth: t * 100 }); }
    });
  }, { passive: true });

  // 5) Rời trang: ghi thời gian ở lại, rồi gửi nốt hàng đợi
  addEventListener('pagehide', function () {
    track('leave', { seconds: Math.round((Date.now() - start) / 1000) });
    flush();
  });
  setInterval(flush, 10000);
})();