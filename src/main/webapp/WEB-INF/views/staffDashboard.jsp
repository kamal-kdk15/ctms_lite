<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Staff Dashboard"/>
<c:set var="nav" value="dashboard"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Staff Dashboard <span class="tag">Site Operations</span></h1>
        <p>Register patients, track informed consent, enroll into trials and report adverse events.</p>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-6 col-lg-3">
        <div class="stat-card"><div class="stat-label">Registered Patients</div>
            <div class="stat-value">${totalPatients}</div><div class="stat-sub">in the system</div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card teal"><div class="stat-label">Enrolled in Trials</div>
            <div class="stat-value">${enrolledPatients}</div><div class="stat-sub">active participants</div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card"><div class="stat-label">Consent Pending</div>
            <div class="stat-value">${pendingConsent}</div><div class="stat-sub"><a href="${ctx}/patients?consent=Pending">View patients &rarr;</a></div></div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card"><div class="stat-label">AEs Reported by Me</div>
            <div class="stat-value">${myAEs}</div><div class="stat-sub"><a href="${ctx}/aeReport">Report new &rarr;</a></div></div>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-md-4"><a class="btn btn-teal w-100 py-3" href="${ctx}/patients"><i class="bi bi-people"></i> Patients &amp; Consent</a></div>
    <div class="col-md-4"><a class="btn btn-outline-secondary w-100 py-3" href="${ctx}/enrollPatient"><i class="bi bi-person-plus"></i> Enroll a Patient</a></div>
    <div class="col-md-4"><a class="btn btn-outline-danger w-100 py-3" href="${ctx}/aeReport"><i class="bi bi-heart-pulse"></i> Report Adverse Event</a></div>
</div>

<div class="panel">
    <h2>Recently Registered Patients</h2>
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>ID</th><th>Name</th><th>Age / Gender</th><th>Trial</th><th>Consent</th></tr></thead>
            <tbody>
            <c:forEach var="p" items="${recentPatients}">
                <tr>
                    <td class="mono">${p.patient_id}</td>
                    <td><strong><c:out value="${p.name}"/></strong></td>
                    <td>${p.age} / ${p.gender}</td>
                    <td><c:out value="${p.trial_name}" default="Not enrolled"/></td>
                    <td><span class="pill pill-${p.consent_status}">${p.consent_status}</span></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>
<%@ include file="_footer.jsp" %>
