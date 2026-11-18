<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Visit Schedule"/><c:set var="nav" value="visits"/>
<%@ include file="_header.jsp" %>
<div class="page-head"><div><h1>Patient Visit Schedule <span class="tag">Protocol Follow-up</span></h1>
<p>Baseline, Week 4, Week 12 and Week 24 visits are generated automatically at enrollment.</p></div></div>
<div class="row g-3 mb-3">
 <div class="col-6 col-lg-3"><div class="stat-card danger"><div class="stat-label">Overdue</div><div class="stat-value">${overdueCount}</div></div></div>
 <div class="col-6 col-lg-3"><div class="stat-card teal"><div class="stat-label">Due in 30 Days</div><div class="stat-value">${upcomingCount}</div></div></div>
 <div class="col-6 col-lg-3"><div class="stat-card"><div class="stat-label">Completed</div><div class="stat-value">${completedCount}</div></div></div>
 <div class="col-6 col-lg-3"><div class="stat-card danger"><div class="stat-label">Missed</div><div class="stat-value">${missedCount}</div></div></div>
</div>
<div class="panel">
 <div class="d-flex flex-wrap gap-2 mb-3">
  <a class="btn btn-sm ${filter == 'all' ? 'btn-teal' : 'btn-outline-secondary'}" href="${ctx}/visits">All</a>
  <a class="btn btn-sm ${filter == 'overdue' ? 'btn-teal' : 'btn-outline-secondary'}" href="${ctx}/visits?filter=overdue">Overdue</a>
  <a class="btn btn-sm ${filter == 'upcoming' ? 'btn-teal' : 'btn-outline-secondary'}" href="${ctx}/visits?filter=upcoming">Upcoming</a>
  <a class="btn btn-sm ${filter == 'completed' ? 'btn-teal' : 'btn-outline-secondary'}" href="${ctx}/visits?filter=completed">Completed</a>
  <a class="btn btn-sm ${filter == 'missed' ? 'btn-teal' : 'btn-outline-secondary'}" href="${ctx}/visits?filter=missed">Missed</a>
 </div>
 <div class="table-wrap"><table class="ctms-table"><thead><tr><th>Subject</th><th>Trial / Site</th><th>Visit</th><th>Due Date</th><th>Status</th><th>Record Outcome</th></tr></thead><tbody>
 <c:forEach var="v" items="${visits}"><tr>
  <td><strong><c:out value="${v.patient_name}"/></strong><div class="sub">SUBJ-${v.patient_id}</div></td>
  <td><c:out value="${v.trial_name}"/><div class="sub">${v.site_code}</div></td>
  <td><c:out value="${v.visit_name}"/></td><td>${v.due_date}<c:if test="${v.status == 'Scheduled' && v.days_late > 0}"><div class="text-danger small">${v.days_late} days late</div></c:if></td>
  <td><span class="pill pill-${v.status}">${v.status}</span></td>
  <td>
   <c:if test="${v.status == 'Scheduled' && sessionScope.role == 'Staff'}">
    <form method="post" action="${ctx}/visits" class="visit-action">
     <input type="hidden" name="visit_id" value="${v.visit_id}"/>
     <input type="date" class="form-control form-control-sm" name="actual_date"/>
     <input type="text" class="form-control form-control-sm" name="reason" placeholder="Reason / source note" maxlength="255" required/>
     <button class="btn btn-sm btn-teal" name="action" value="complete">Complete</button>
     <button class="btn btn-sm btn-outline-danger" name="action" value="missed">Mark Missed</button>
    </form>
   </c:if>
   <c:if test="${not empty v.note}"><small><c:out value="${v.note}"/></small></c:if>
  </td>
 </tr></c:forEach>
 <c:if test="${empty visits}"><tr><td colspan="6" class="text-center text-muted">No visits in this filter.</td></tr></c:if>
 </tbody></table></div>
</div>
<%@ include file="_footer.jsp" %>
