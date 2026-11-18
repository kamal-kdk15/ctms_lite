<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Patients and Consent"/>
<c:set var="nav" value="patients"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Patients &amp; Informed Consent <span class="tag">ICH-GCP 4.8</span></h1>
        <p>Register patients, track consent (Obtained / Pending / Withdrawn) and see who is enrolled in which trial.</p>
    </div>
    <div class="d-flex gap-2">
        <a class="btn btn-outline-secondary" href="${ctx}/enrollPatient"><i class="bi bi-person-plus"></i> Enroll Patient</a>
        <button class="btn btn-teal" data-bs-toggle="modal" data-bs-target="#patientModal"><i class="bi bi-plus-circle"></i> Register Patient</button>
    </div>
</div>

<div class="panel">
    <form method="get" action="${ctx}/patients" class="row g-2 align-items-end">
        <div class="col-md-4">
            <label class="form-label">Filter by trial</label>
            <select name="trial" class="form-select">
                <option value="">All trials</option>
                <c:forEach var="t" items="${trials}">
                    <option value="${t.trial_id}" ${selTrial == t.trial_id ? 'selected' : ''}><c:out value="${t.trial_name}"/></option>
                </c:forEach>
            </select>
        </div>
        <div class="col-md-4">
            <label class="form-label">Filter by consent</label>
            <select name="consent" class="form-select">
                <option value="">All consent states</option>
                <option ${selConsent == 'Obtained' ? 'selected' : ''}>Obtained</option>
                <option ${selConsent == 'Pending' ? 'selected' : ''}>Pending</option>
                <option ${selConsent == 'Withdrawn' ? 'selected' : ''}>Withdrawn</option>
            </select>
        </div>
        <div class="col-md-4 d-flex gap-2">
            <button class="btn btn-teal" type="submit"><i class="bi bi-funnel"></i> Filter</button>
            <a class="btn btn-light" href="${ctx}/patients">Reset</a>
        </div>
    </form>
</div>

<div class="panel">
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>ID</th><th>Patient</th><th>Age / Gender</th><th>Trial / Site / Allocation</th><th>Consent</th><th>Update Consent</th></tr></thead>
            <tbody>
            <c:forEach var="p" items="${patients}">
                <tr>
                    <td class="mono">${p.patient_id}</td>
                    <td><strong><c:out value="${p.name}"/></strong></td>
                    <td>${p.age} / ${p.gender}</td>
                    <td>
                        <c:choose>
                            <c:when test="${not empty p.trial_name}">
                                <c:out value="${p.trial_name}"/>
                                <div class="sub">since ${p.enrolled_at} <c:if test="${not empty p.site_code}"> · <c:out value="${p.site_code}"/></c:if></div>
                                <c:if test="${not empty p.arm}">
                                    <div class="sub"><strong><c:out value="${p.arm}"/></strong> · <span class="mono"><c:out value="${p.randomization_no}"/></span></div>
                                </c:if>
                            </c:when>
                            <c:otherwise><span class="pill pill-none">Not enrolled</span></c:otherwise>
                        </c:choose>
                    </td>
                    <td><span class="pill pill-${p.consent_status}">${p.consent_status}</span></td>
                    <td>
                        <form method="post" action="${ctx}/patients" class="inline-form">
                            <input type="hidden" name="action" value="updateConsent">
                            <input type="hidden" name="patient_id" value="${p.patient_id}">
                            <select name="consent_status" class="form-select form-select-sm">
                                <option ${p.consent_status == 'Obtained' ? 'selected' : ''}>Obtained</option>
                                <option ${p.consent_status == 'Pending' ? 'selected' : ''}>Pending</option>
                                <option ${p.consent_status == 'Withdrawn' ? 'selected' : ''}>Withdrawn</option>
                            </select>
                            <button class="btn btn-sm btn-outline-secondary" type="submit">Save</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty patients}"><tr><td colspan="6" class="text-center text-muted py-4">No patients match the selected filters.</td></tr></c:if>
            </tbody>
        </table>
    </div>
</div>

<!-- Register patient modal -->
<div class="modal fade" id="patientModal" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
        <form class="modal-content" method="post" action="${ctx}/patients">
            <input type="hidden" name="action" value="register">
            <div class="modal-header"><h5 class="modal-title">Register New Patient</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body">
                <div class="row g-3">
                    <div class="col-12"><label class="form-label">Full Name *</label>
                        <input type="text" name="name" class="form-control" maxlength="100" required></div>
                    <div class="col-6"><label class="form-label">Age *</label>
                        <input type="number" name="age" class="form-control" min="1" max="120" required></div>
                    <div class="col-6"><label class="form-label">Gender *</label>
                        <select name="gender" class="form-select"><option>Male</option><option>Female</option><option>Other</option></select></div>
                    <div class="col-12"><label class="form-label">Informed Consent Status *</label>
                        <select name="consent_status" class="form-select"><option>Pending</option><option>Obtained</option><option>Withdrawn</option></select></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-teal">Register Patient</button>
            </div>
        </form>
    </div>
</div>
<%@ include file="_footer.jsp" %>
