</main>
<footer class="app-footer">
    CTMS Lite v2.0 &middot; IGNOU BCA Project (BCSP-064) &middot;
    Electronic records workflow demonstrator &middot; Not a validated regulated system
</footer>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
(function () {
  var warning = document.createElement('div');
  warning.className = 'alert alert-warning session-warning';
  warning.textContent = 'Your session expires after 15 minutes without activity. Save your work before leaving this page.';
  document.body.appendChild(warning);
  var lastActivity = Date.now();
  function activity() { lastActivity = Date.now(); warning.style.display = 'none'; }
  ['mousemove','keydown','click','scroll','touchstart'].forEach(function(e){
    document.addEventListener(e, activity, {passive:true});
  });
  window.setInterval(function () {
    var idleFor = Date.now() - lastActivity;
    if (idleFor >= 14 * 60 * 1000) {
      warning.style.display = 'block';
    } else if (idleFor < 2 * 60 * 1000) {
      fetch('${ctx}/session-ping', {credentials:'same-origin', cache:'no-store'}).catch(function(){});
    }
  }, 60 * 1000);
})();
</script>
</body>
</html>
