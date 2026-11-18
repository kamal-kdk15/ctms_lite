<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Audit Log"/>
<c:set var="nav" value="audit"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Audit Log <span class="tag">21 CFR Part 11</span></h1>
        <p>Every critical action (login, enrollment, AE submission, status change, consent change) is recorded with user, action and timestamp.</p>
    </div>
</div>

<div class="d-flex gap-2 mb-3 no-print">
    <a class="btn btn-sm btn-outline-secondary" href="${ctx}/export?type=audit&amp;format=csv"><i class="bi bi-filetype-csv"></i> Export CSV</a>
    <a class="btn btn-sm btn-outline-secondary" href="${ctx}/export?type=audit&amp;format=pdf"><i class="bi bi-filetype-pdf"></i> Download PDF</a>
</div>
<div class="panel">
    <form method="get" action="${ctx}/auditLog" class="row g-2 align-items-end">
        <div class="col-md-3"><label class="form-label">From date</label>
            <input type="date" name="from" class="form-control" value="<c:out value='${selFrom}'/>"></div>
        <div class="col-md-3"><label class="form-label">To date</label>
            <input type="date" name="to" class="form-control" value="<c:out value='${selTo}'/>"></div>
        <div class="col-md-2"><label class="form-label">User</label>
            <select name="user" class="form-select">
                <option value="">All users</option>
                <c:forEach var="u" items="${users}">
                    <option value="${u.user_id}" ${selUser == u.user_id ? 'selected' : ''}><c:out value="${u.username}"/></option>
                </c:forEach>
            </select></div>
        <div class="col-md-2"><label class="form-label">Action</label>
            <select name="action" class="form-select">
                <option value="">All actions</option>
                <c:forEach var="a" items="${actions}">
                    <option value="${a.action_type}" ${selAction == a.action_type ? 'selected' : ''}><c:out value="${a.action_type}"/></option>
                </c:forEach>
            </select></div>
        <div class="col-md-2 d-flex gap-2">
            <button class="btn btn-teal" type="submit"><i class="bi bi-funnel"></i> Filter</button>
            <a class="btn btn-light" href="${ctx}/auditLog">Reset</a>
        </div>
    </form>
</div>

<div class="panel">
    <div class="sub mb-2">Showing the latest ${fn:length(logs)} entries (max 200).</div>
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>Log ID</th><th>Timestamp</th><th>User</th><th>Action</th><th>Table</th><th>Description</th></tr></thead>
            <tbody>
            <c:forEach var="l" items="${logs}">
                <tr>
                    <td class="mono">${l.log_id}</td>
                    <td class="sub">${l.log_time}</td>
                    <td><c:out value="${l.username}"/> <span class="sub">(${l.role})</span></td>
                    <td class="mono"><c:out value="${l.action_type}"/></td>
                    <td class="mono"><c:out value="${l.affected_table}"/></td>
                    <td><c:out value="${l.description}"/></td>
                </tr>
            </c:forEach>
            <c:if test="${empty logs}"><tr><td colspan="6" class="text-center text-muted py-4">No audit entries match the selected filters.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>
<div class="panel">
    <h2>Electronic Signature Register <span class="tag">Recent 20</span></h2>
    <div class="table-wrap"><table class="ctms-table">
        <thead><tr><th>When</th><th>User</th><th>Record</th><th>Meaning</th><th>Signed State</th><th>Hash</th></tr></thead>
        <tbody>
        <c:forEach var="g" items="${signatures}">
            <tr><td>${g.signed_at}</td><td><c:out value="${g.username}"/></td>
                <td>${g.record_type} #${g.record_id}</td><td><strong><c:out value="${g.meaning}"/></strong></td>
                <td><c:out value="${g.record_state}"/></td><td class="mono">${g.hash_short}…</td></tr>
        </c:forEach>
        <c:if test="${empty signatures}"><tr><td colspan="6" class="text-muted text-center">No electronic signatures recorded.</td></tr></c:if>
        </tbody>
    </table></div>
</div>
<%@ include file="_footer.jsp" %>
