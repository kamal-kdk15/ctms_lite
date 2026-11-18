<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="AE Review"/>
<c:set var="nav" value="aeReview"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Adverse Event Review <span class="tag">21 CFR 312.32</span></h1>
        <p>Review submitted adverse events and move them through Open &rarr; Under Review &rarr; Resolved. SAEs have a 24-hour review clock. Status changes require password re-authentication and an electronic signature.</p>
    </div>
</div>

<div class="panel">
    <form method="get" action="${ctx}/aeReview" class="row g-2 align-items-end">
        <div class="col-md-4"><label class="form-label">Severity</label>
            <select name="severity" class="form-select">
                <option value="">All severities</option>
                <option ${selSeverity == 'Mild' ? 'selected' : ''}>Mild</option>
                <option ${selSeverity == 'Moderate' ? 'selected' : ''}>Moderate</option>
                <option ${selSeverity == 'Severe' ? 'selected' : ''}>Severe</option>
            </select></div>
        <div class="col-md-4"><label class="form-label">Status</label>
            <select name="status" class="form-select">
                <option value="">All statuses</option>
                <option ${selStatus == 'Open' ? 'selected' : ''}>Open</option>
                <option ${selStatus == 'Under Review' ? 'selected' : ''}>Under Review</option>
                <option ${selStatus == 'Resolved' ? 'selected' : ''}>Resolved</option>
            </select></div>
        <div class="col-md-4 d-flex gap-2">
            <button class="btn btn-teal" type="submit"><i class="bi bi-funnel"></i> Filter</button>
            <a class="btn btn-light" href="${ctx}/aeReview">Reset</a>
        </div>
    </form>
</div>

<div class="panel">
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>AE</th><th>Patient / Trial</th><th>Severity / SAE</th><th>Description</th><th>Reported / Clock</th><th>Status</th><th>Review &amp; Sign</th></tr></thead>
            <tbody>
            <c:forEach var="a" items="${aes}">
                <tr>
                    <td class="mono">#${a.ae_id}</td>
                    <td><strong><c:out value="${a.patient_name}"/></strong><div class="sub"><c:out value="${a.trial_name}"/></div></td>
                    <td><span class="pill pill-${a.severity}">${a.severity}</span>
                        <c:if test="${a.is_sae == 1}"><div><span class="pill pill-Open">SAE</span></div></c:if></td>
                    <td class="desc-cell"><c:out value="${a.description}"/>
                        <div class="sub">Occurred ${a.occurrence_date}</div></td>
                    <td class="sub">${a.reported_at}<br>by <c:out value="${a.reporter}"/>
                        <c:if test="${a.is_sae == 1 && a.status == 'Open'}">
                            <br><c:choose>
                                <c:when test="${a.sae_seconds_left <= 0}"><span class="pill pill-Open">24h review overdue</span></c:when>
                                <c:otherwise><span class="pill pill-Pending">SAE review due · ${a.sae_seconds_left / 3600}h left</span></c:otherwise>
                            </c:choose>
                        </c:if></td>
                    <td><span class="pill pill-${fn:replace(a.status, ' ', '')}">${a.status}</span></td>
                    <td>
                        <form method="post" action="${ctx}/aeReview" class="inline-form">
                            <input type="hidden" name="ae_id" value="${a.ae_id}">
                            <select name="status" class="form-select form-select-sm">
                                <option ${a.status == 'Open' ? 'selected' : ''}>Open</option>
                                <option ${a.status == 'Under Review' ? 'selected' : ''}>Under Review</option>
                                <option ${a.status == 'Resolved' ? 'selected' : ''}>Resolved</option>
                            </select>
                            <div class="esign-inline">
                                <select name="sig_meaning" class="form-select form-select-sm" required>
                                    <option value="">Signature meaning</option><option>Reviewed</option><option>Approved</option>
                                </select>
                                <input type="password" name="sig_password" class="form-control form-control-sm" placeholder="Password" required>
                                <label class="small"><input type="checkbox" name="sig_attest" required> I confirm</label>
                            </div>
                            <button class="btn btn-sm btn-outline-secondary" type="submit">Sign &amp; Save</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty aes}"><tr><td colspan="7" class="text-center text-muted py-4">No adverse events match the selected filters.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="_footer.jsp" %>
