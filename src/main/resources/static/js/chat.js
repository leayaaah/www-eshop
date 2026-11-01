(function () {
  var widget = document.querySelector('.chat-widget');
  if (!widget) return;
  var panel = widget.querySelector('#chat-panel');
  var messages = widget.querySelector('.chat-messages');
  var form = widget.querySelector('.chat-form');
  var sendButton = form.querySelector('button');
  var sending = false;
  widget.querySelector('.chat-toggle').addEventListener('click', function () {
    panel.hidden = !panel.hidden;
  });
  form.addEventListener('submit', async function (event) {
    event.preventDefault();
    if (sending) return;
    var input = event.target.message;
    var message = input.value.trim();
    if (!message) return;
    sending = true;
    input.disabled = true;
    sendButton.disabled = true;
    var user = document.createElement('p');
    user.textContent = 'Bạn: ' + message;
    messages.appendChild(user);
    input.value = '';
    var status = document.createElement('p');
    status.className = 'chat-status';
    status.textContent = 'IUH eShop đang trả lời...';
    messages.appendChild(status);
    var controller = new AbortController();
    var timeout = setTimeout(function () { controller.abort(); }, 45000);
    try {
      var conversationId = sessionStorage.getItem('eshop-chat-conversation');
      if (!conversationId) {
        conversationId = crypto.randomUUID ? crypto.randomUUID() : String(Date.now());
        sessionStorage.setItem('eshop-chat-conversation', conversationId);
      }
      var response = await fetch('/api/chat', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message, conversationId: conversationId }),
        signal: controller.signal
      });
      var data = await response.json();
      if (!response.ok && !data.error) {
        data.error = 'Chatbot không thể xử lý yêu cầu lúc này.';
      }
      status.remove();
      var reply = document.createElement('p');
      reply.textContent = 'IUH eShop: ' + (data.answer || data.error || 'Xin lỗi, có lỗi xảy ra.');
      messages.appendChild(reply);
    } catch (error) {
      status.remove();
      var failure = document.createElement('p');
      failure.textContent = 'IUH eShop: ' +
        (error.name === 'AbortError'
          ? 'Chatbot phản hồi quá lâu, vui lòng thử lại.'
          : 'Không thể kết nối chatbot.');
      messages.appendChild(failure);
    } finally {
      clearTimeout(timeout);
      sending = false;
      input.disabled = false;
      sendButton.disabled = false;
      input.focus();
    }
  });
})();
