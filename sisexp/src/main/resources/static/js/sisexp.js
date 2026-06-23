/* ============================================================
   SISEXP-UPLA — Common JavaScript Utilities
   ============================================================ */

function toggleSidebar() {
  var s = document.getElementById('sidebar');
  var o = document.getElementById('overlay');
  s.classList.toggle('open');
  o.classList.toggle('show');
}

function setActiveSidebarLink() {
  var path = window.location.pathname;
  document.querySelectorAll('.sidebar a').forEach(function(a) {
    var linkPath = a.getAttribute('href');
    if (linkPath && path.startsWith(linkPath)) {
      a.classList.add('active');
    }
  });
}

function showToast(msg, type) {
  type = type || 'info';
  var colors = {success:'#10b981',error:'#ef4444',info:'#0ea5e9',warning:'#f59e0b'};
  var icons = {success:'check-circle',error:'x-circle',info:'info-circle',warning:'exclamation-triangle'};
  var el = document.createElement('div');
  el.style.cssText = 'position:fixed;top:1rem;right:1rem;z-index:9999;background:white;' +
    'border-left:4px solid ' + colors[type] + ';padding:1rem 1.5rem;border-radius:.5rem;' +
    'box-shadow:0 4px 20px rgba(0,0,0,.15);max-width:400px;animation:slideIn .3s ease;font-size:.9rem';
  el.innerHTML = '<i class="bi bi-' + icons[type] + '" style="color:' + colors[type] + ';margin-right:.5rem"></i> ' + msg;
  document.body.appendChild(el);
  setTimeout(function() {
    el.style.opacity = '0';
    el.style.transition = 'opacity .3s';
    setTimeout(function() { el.remove(); }, 300);
  }, 3500);
}

function confirmAction(msg, cb) {
  if (confirm(msg)) cb();
}

function filtrarTabla(inputId, tableId, colIndex) {
  var q = document.getElementById(inputId).value.toLowerCase();
  var rows = document.getElementById(tableId).querySelectorAll('tbody tr');
  rows.forEach(function(r) {
    r.style.display = r.cells[colIndex].textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}

function filtrarEstado(selectId, tableId, colIndex) {
  var val = document.getElementById(selectId).value;
  var rows = document.getElementById(tableId).querySelectorAll('tbody tr');
  rows.forEach(function(r) {
    r.style.display = !val || r.cells[colIndex].textContent.trim() === val ? '' : 'none';
  });
}

function filtrarEstadoDataset(selectId, tableId, dataAttr) {
  var val = document.getElementById(selectId).value;
  var rows = document.getElementById(tableId).querySelectorAll('tbody tr');
  rows.forEach(function(r) {
    if (r.getAttribute(dataAttr) === null) return;
    r.style.display = !val || r.getAttribute(dataAttr) === val ? '' : 'none';
  });
}

function filtrarTablaCodigo(inputId, tableId) {
  var q = document.getElementById(inputId).value.toUpperCase();
  document.querySelectorAll('#' + tableId + ' tbody tr').forEach(function(r) {
    if (r.getAttribute('data-estado') === null) return;
    var c = r.cells[0]?.textContent || '';
    r.style.display = c.toUpperCase().includes(q) ? '' : 'none';
  });
}

function autoDismissAlerts() {
  document.querySelectorAll('.alert-auto-close').forEach(function(el) {
    setTimeout(function() {
      el.style.opacity = '0';
      el.style.transition = 'opacity .3s';
      setTimeout(function() { el.remove(); }, 300);
    }, 4000);
  });
}

/* Notification badge polling */
async function loadNotifCount() {
  try {
    var resp = await fetch('/api/notificaciones/count');
    if (!resp.ok) return;
    var data = await resp.json();
    var badge = document.getElementById('notif-badge');
    if (badge) {
      badge.textContent = data.count || 0;
      badge.style.display = data.count > 0 ? '' : 'none';
    }
    var count = document.getElementById('notif-count');
    if (count) {
      count.textContent = data.count || 0;
      count.style.display = data.count > 0 ? '' : 'none';
    }
  } catch(e) {}
}

/* Modal form helpers */
function setupFormModal(modalId, formId, fields, actionPrefix) {
  var modal = document.getElementById(modalId);
  if (!modal) return;
  modal.addEventListener('show.bs.modal', function(e) {
    var btn = e.relatedTarget;
    if (!btn) return;
    var id = btn.getAttribute('data-id');
    if (!id) return;
    document.getElementById(formId).action = actionPrefix + '/' + id + '/editar';
    fields.forEach(function(f) {
      var el = document.getElementById(formId + '_' + f.name);
      if (el) el.value = btn.getAttribute('data-' + f.attr);
    });
  });
  modal.addEventListener('hidden.bs.modal', function() {
    document.getElementById(formId).reset();
  });
}

/* Init on DOM ready */
document.addEventListener('DOMContentLoaded', function() {
  setActiveSidebarLink();
  autoDismissAlerts();
  loadNotifCount();
  setInterval(loadNotifCount, 60000);
});

/* Overlay click to close sidebar */
document.addEventListener('click', function(e) {
  if (e.target.classList.contains('overlay')) {
    toggleSidebar();
  }
});
