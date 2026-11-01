(function () {
  'use strict';
  var POLICY_VERSION = '2026-11-01';
  var KEY = 'eshop_consent';

  function read() {
    try { return JSON.parse(localStorage.getItem(KEY) || 'null'); } catch (_) { return null; }
  }
  function write(analytics) {
    localStorage.setItem(KEY, JSON.stringify({
      policyVersion: POLICY_VERSION,
      analytics: analytics === true,
      updatedAt: new Date().toISOString()
    }));
    document.querySelector('[data-consent-modal]')?.remove();
    window.dispatchEvent(new CustomEvent('eshop:consent-changed'));
    window.location.reload();
  }
  function modal(settings) {
    if (!settings && read()?.policyVersion === POLICY_VERSION) return;
    var root = document.createElement('div');
    root.className = 'consent-backdrop';
    root.dataset.consentModal = '';
    root.innerHTML = '<section class="consent-modal" role="dialog" aria-modal="true" aria-labelledby="consent-title">' +
      '<h2 id="consent-title">Quyền riêng tư của bạn</h2>' +
      '<p>eShop dùng cookie cần thiết để vận hành và chỉ dùng tracking CLICK/DWELL khi bạn cho phép. ' +
      '<a href="/privacy">Xem chính sách</a>.</p>' +
      '<div class="consent-options" data-consent-options hidden>' +
      '<label><input type="checkbox" data-analytics checked> Cho phép phân tích hành vi mua sắm (CLICK/DWELL)</label>' +
      '</div><div class="consent-actions">' +
      '<button class="button" data-consent-all>Đồng ý tất cả</button>' +
      '<button class="button secondary" data-consent-reject>Từ chối</button>' +
      '<button class="button secondary" data-consent-custom>Tùy chỉnh</button></div></section>';
    document.body.appendChild(root);
    root.querySelector('[data-consent-all]').onclick = function () { write(true); };
    root.querySelector('[data-consent-reject]').onclick = function () { write(false); };
    root.querySelector('[data-consent-custom]').onclick = function () {
      root.querySelector('[data-consent-options]').hidden = false;
      root.querySelector('[data-consent-custom]').textContent = 'Lưu lựa chọn';
      root.querySelector('[data-consent-custom]').onclick = function () {
        write(root.querySelector('[data-analytics]').checked);
      };
    };
  }
  window.eshopConsent = { open: function () { modal(true); } };
  document.addEventListener('DOMContentLoaded', function () { modal(false); });
})();
