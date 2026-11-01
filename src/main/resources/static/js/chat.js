(function () {
  var widget = document.querySelector('.chat-widget');
  if (!widget) return;
  var panel = widget.querySelector('#chat-panel');
  var messages = widget.querySelector('.chat-messages');
  widget.querySelector('.chat-toggle').addEventListener('click', function () {
    panel.hidden = !panel.hidden;
  });
  widget.querySelector('.chat-form').addEventListener('submit', async function (event) {
    event.preventDefault();
    var input = event.target.message;
    var message = input.value.trim();
    if (!message) return;
    var user = document.createElement('p');
    user.textContent = 'Bạn: ' + message;
    messages.appendChild(user);
    input.value = '';
    try {
      var response = await fetch('/api/chat', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: message })
      });
      var data = await response.json();
      var reply = document.createElement('p');
      reply.textContent = 'IUH eShop: ' + (data.answer || data.error || 'Xin lỗi, có lỗi xảy ra.');
      messages.appendChild(reply);
    } catch (error) {
      var failure = document.createElement('p');
      failure.textContent = 'IUH eShop: Không thể kết nối chatbot.';
      messages.appendChild(failure);
    }
  });
})();
