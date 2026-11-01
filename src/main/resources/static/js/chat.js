(function () {
  var widget = document.querySelector('.chat-widget');
  if (!widget) return;
  var panel = widget.querySelector('#chat-panel');
  var messages = widget.querySelector('.chat-messages');
  var toggle = widget.querySelector('.chat-toggle');
  var close = widget.querySelector('.chat-close');
  function setPanelOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    toggle.hidden = open;
  }
  toggle.addEventListener('click', function () {
    setPanelOpen(true);
  });
  close.addEventListener('click', function () {
    setPanelOpen(false);
    toggle.focus();
  });
  widget.querySelector('.chat-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    var input = event.target.message;
    var message = input.value.trim();
    if (!message) return;
    appendMessage('user', message);
    input.value = '';
    try {
      var response = await fetch('/api/chat', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message })
      });
      var data = await response.json();
      appendMessage('bot', data.answer || data.error || 'Xin lỗi, có lỗi xảy ra.');
    } catch (error) {
      appendMessage('bot', 'Không thể kết nối chatbot lúc này.');
    }
    messages.scrollTop = messages.scrollHeight;
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
  }
})();
