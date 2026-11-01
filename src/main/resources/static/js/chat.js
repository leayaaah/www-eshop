(function () {
  var widget = document.querySelector('.chat-widget');
  if (!widget) return;
  var panel = widget.querySelector('.chat-panel');
  var messages = widget.querySelector('.chat-messages');
  var form = widget.querySelector('.chat-form');
  var toggle = widget.querySelector('.chat-toggle');
  var close = widget.querySelector('.chat-close');
  var input = form.querySelector('input[name="message"]');
  var sendButton = form.querySelector('button');
  var sending = false;

  function setPanelOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    toggle.hidden = open;
    if (open) input.focus();
  }

  toggle.addEventListener('click', function () {
    setPanelOpen(true);
  });

  close.addEventListener('click', function () {
    setPanelOpen(false);
    toggle.focus();
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && !panel.hidden) {
      setPanelOpen(false);
      toggle.focus();
    }
  });

  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    if (sending) return;
    var message = input.value.trim();
    if (!message) return;

    sending = true;
    input.disabled = true;
    sendButton.disabled = true;
    appendMessage('user', message);
    input.value = '';
    var status = appendMessage('bot', 'IUH eShop đang trả lời...');
    status.classList.add('chat-message-loading');

    var controller = new AbortController();
    var timeout = setTimeout(function () { controller.abort(); }, 45000);

    try {
      var conversationId = sessionStorage.getItem('eshop-chat-conversation');
      if (!conversationId) {
        conversationId = crypto.randomUUID ? crypto.randomUUID() : String(Date.now());
        sessionStorage.setItem('eshop-chat-conversation', conversationId);
      }

      var response = await fetch('/api/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message, conversationId: conversationId }),
        signal: controller.signal
      });
      var data = await response.json();
      if (!response.ok && !data.error) {
        data.error = 'Chatbot không thể xử lý yêu cầu lúc này.';
      }
      status.remove();
      appendMessage('bot', data.answer || data.error || 'Xin lỗi, có lỗi xảy ra.');
    } catch (error) {
      status.remove();
      appendMessage('bot', error.name === 'AbortError'
        ? 'Chatbot phản hồi quá lâu, vui lòng thử lại.'
        : 'Không thể kết nối chatbot lúc này.');
    } finally {
      clearTimeout(timeout);
      sending = false;
      input.disabled = false;
      sendButton.disabled = false;
      input.focus();
      messages.scrollTop = messages.scrollHeight;
    }
  });

  function appendMessage(type, text) {
    var row = document.createElement('div');
    row.className = 'chat-message chat-message-' + type;
    var avatar = document.createElement('div');
    avatar.className = 'chat-avatar';
    avatar.textContent = type === 'bot' ? '✦' : 'Bạn';
    var bubble = document.createElement('div');
    bubble.className = 'chat-bubble';
    bubble.textContent = text;
    row.appendChild(avatar);
    row.appendChild(bubble);
    messages.appendChild(row);
    messages.scrollTop = messages.scrollHeight;
    return row;
  }
})();
