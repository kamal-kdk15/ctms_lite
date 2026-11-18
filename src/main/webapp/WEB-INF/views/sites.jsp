<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Investigative Sites"/><c:set var="nav" value="sites"/>
<%@ include file="_header.jsp" %>
<div class="page-head">
 <div><h1>Site Management <span class="tag">Multi-site Operations</span></h1>
 <p>Register investigative sites and set site-level enrollment targets for each trial.</p></div>
</div>
<div class="row g-3">
 <div class="col-lg-5"><div class="panel"><h2>Register Site</h2>
  <form method="post" action="${ctx}/sites">
   <input type="hidden" name="action" value="create"/>
   <label class="form-label">Site Code</label><input class="form-control mb-2" name="site_code" maxlength="30" required placeholder="IND-DEL-01"/>
   <label class="form-label">Institution</label><input class="form-control mb-2" name="institution" maxlength="150" required/>
   <label class="form-label">City / Location</label><input class="form-control mb-2" name="location" maxlength="150" required/>
   <label class="form-label">Principal Investigator</label><input class="form-control mb-3" name="principal_investigator" maxlength="120" required/>
   <button class="btn btn-teal">Create Site</button>
  </form></div>
  <div class="panel"><h2>Assign Site to Trial</h2>
   <form method="post" action="${ctx}/sites">
    <input type="hidden" name="action" value="assign"/>
    <label class="form-label">Trial</label><select class="form-select mb-2" name="trial_id" required>
     <c:forEach var="t" items="${trials}"><option value="${t.trial_id}"><c:out value="${t.trial_name}"/> (target ${t.target_enrollment})</option></c:forEach>
    </select>
    <label class="form-label">Site</label><select class="form-select mb-2" name="site_id" required>
     <c:forEach var="s" items="${sites}"><option value="${s.site_id}"><c:out value="${s.site_code}"/> · <c:out value="${s.institution}"/></option></c:forEach>
    </select>
    <label class="form-label">Site Enrollment Target</label><input class="form-control mb-3" type="number" min="1" max="100000" name="site_target" value="30" required/>
    <button class="btn btn-teal">Save Trial-Site Assignment</button>
   </form>
  </div>
 </div>
 <div class="col-lg-7"><div class="panel"><h2>Registered Sites</h2><div class="table-wrap"><table class="ctms-table">
  <thead><tr><th>Code</th><th>Institution / City</th><th>PI</th><th>Trials</th><th>Status</th></tr></thead><tbody>
  <c:forEach var="s" items="${sites}"><tr><td class="mono"><c:out value="${s.site_code}"/></td><td><strong><c:out value="${s.institution}"/></strong><div class="sub"><c:out value="${s.location}"/></div></td><td><c:out value="${s.principal_investigator}"/></td><td>${s.trial_count}</td><td><span class="pill pill-${s.status}">${s.status}</span></td></tr></c:forEach>
  </tbody></table></div></div>
  <div class="panel"><h2>Trial-Site Enrollment</h2><div class="table-wrap"><table class="ctms-table">
   <thead><tr><th>Trial</th><th>Site</th><th>Enrolled / Target</th><th>Progress</th></tr></thead><tbody>
   <c:forEach var="a" items="${assignments}"><tr><td><c:out value="${a.trial_name}"/></td><td class="mono">${a.site_code}</td><td>${a.enrolled} / ${a.site_target}</td><td><div class="progress"><div class="progress-bar" style="width:${a.pct}%"></div></div></td></tr></c:forEach>
   </tbody></table></div></div>
 </div>
</div>
<%@ include file="_footer.jsp" %>
