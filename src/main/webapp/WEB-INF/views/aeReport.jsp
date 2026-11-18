<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Report Adverse Event"/>
<c:set var="nav" value="aeReport"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Adverse Event Reporting <span class="tag">ICH-GCP E2A</span></h1>
        <p>Log medical events experienced by enrolled patients. New reports start as <em>Open</em> and are reviewed by the Admin.</p>
    </div>
</div>

<div class="row g-3">
    <div class="col-lg-5">
        <div class="panel">
            <h2>New Adverse Event</h2>
            <c:choose>
                <c:when test="${empty enrolledPatients}">
                    <div class="alert alert-warning mb-0">No enrolled patients yet. Enroll a patient in a trial first.</div>
                </c:when>
                <c:otherwise>
                    <form method="post" action="${ctx}/aeReport">
                        <div class="mb-3"><label class="form-label">Patient *</label>
                            <select name="patient_id" class="form-select" required>
                                <option value="">-- Select enrolled patient --</option>
                                <c:forEach var="p" items="${enrolledPatients}">
                                    <option value="${p.patient_id}">#${p.patient_id} - <c:out value="${p.name}"/> (<c:out value="${p.trial_name}"/>)</option>
                                </c:forEach>
                            </select></div>
                        <div class="row g-3 mb-3">
                            <div class="col-6"><label class="form-label">Severity *</label>
                                <select name="severity" class="form-select"><option>Mild</option><option>Moderate</option><option>Severe</option></select></div>
                            <div class="col-6"><label class="form-label">Date of Occurrence *</label>
                                <input type="date" name="occurrence_date" class="form-control" max="${today}" value="${today}" required></div>
                        </div>
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" name="is_sae" id="isSae">
                            <label class="form-check-label" for="isSae">Classify as a Serious Adverse Event (SAE)</label>
                            <div class="form-text">SAE classification must follow the protocol and applicable safety criteria. The system starts a 24-hour review clock when selected.</div>
                        </div>
                        <div class="mb-3"><label class="form-label">Event Description *</label>
                            <textarea name="description" class="form-control" rows="4" maxlength="2000" required></textarea></div>
                        <button class="btn btn-danger" type="submit"><i class="bi bi-heart-pulse"></i> Submit Adverse Event</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <div class="col-lg-7">
        <div class="panel">
            <h2>My Recent Reports</h2>
            <div class="table-wrap">
                <table class="ctms-table">
                    <thead><tr><th>AE</th><th>Patient / Trial</th><th>Severity</th><th>Status</th><th>Occurred</th></tr></thead>
                    <tbody>
                    <c:forEach var="a" items="${myAEs}">
                        <tr>
                            <td class="mono">#${a.ae_id}</td>
                            <td><strong><c:out value="${a.patient_name}"/></strong><div class="sub"><c:out value="${a.trial_name}"/></div></td>
                            <td><span class="pill pill-${a.severity}">${a.severity}</span><c:if test="${a.is_sae == 1}"><div><span class="pill pill-Open">SAE</span></div></c:if></td>
                            <td><span class="pill pill-${fn:replace(a.status, ' ', '')}">${a.status}</span></td>
                            <td class="sub">${a.occurrence_date}</td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty myAEs}"><tr><td colspan="5" class="text-center text-muted py-4">You have not reported any adverse events yet.</td></tr></c:if>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</div>
<%@ include file="_footer.jsp" %>
