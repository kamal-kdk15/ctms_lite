<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="pageTitle" value="Users & Access"/><c:set var="nav" value="users"/>
<%@ include file="_header.jsp" %>
<div class="page-head"><div><h1>Users &amp; Access Control</h1><p>Review failed login attempts and unlock accounts after verifying the request.</p></div></div>
<div class="panel"><div class="table-wrap"><table class="ctms-table">
<thead><tr><th>User</th><th>Role</th><th>Failed Attempts</th><th>Lock Until</th><th>Account</th><th>Admin Action</th></tr></thead><tbody>
<c:forEach var="u" items="${users}"><tr>
<td><strong><c:out value="${u.username}"/></strong><div class="sub">ID ${u.user_id}</div></td><td>${u.role}</td><td>${u.failed_attempts}</td><td>${u.locked_until}</td>
<td><c:choose><c:when test="${u.is_locked == 1}"><span class="pill pill-Open">Locked</span></c:when><c:otherwise><span class="pill pill-Active">Active</span></c:otherwise></c:choose></td>
<td><form method="post" action="${ctx}/users" class="inline-form"><input type="hidden" name="user_id" value="${u.user_id}"/>
<input class="form-control form-control-sm" name="reason" maxlength="255" placeholder="Unlock reason" required/>
<button class="btn btn-sm btn-outline-secondary">Unlock</button></form></td>
</tr></c:forEach></tbody></table></div></div>
<%@ include file="_footer.jsp" %>
