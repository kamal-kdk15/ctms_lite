<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Admin Dashboard"/>
<c:set var="nav" value="dashboard"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Admin Dashboard <span class="tag">Role-Based Access</span></h1>
        <p>Live overview of trials, enrolled patients, adverse events and risk alerts.</p>
    </div>
    <div class="d-flex gap-2 flex-wrap">
        <a href="${ctx}/export?type=trials&amp;format=csv" class="btn btn-outline-secondary"><i class="bi bi-download"></i> Trial CSV</a>
        <a href="${ctx}/export?type=trials&amp;format=pdf" class="btn btn-outline-secondary"><i class="bi bi-file-pdf"></i> Trial PDF</a>
        <a href="${ctx}/trials" class="btn btn-teal"><i class="bi bi-plus-circle"></i> Register Trial</a>
    </div>
</div>

<div class="row g-3 mb-3">
    <div class="col-6 col-lg-3">
        <div class="stat-card teal">
            <div class="stat-label">Active Trials</div>
            <div class="stat-value">${activeTrials}</div>
            <div class="stat-sub">of ${totalTrials} registered</div>
        </div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card">
            <div class="stat-label">Enrolled Patients</div>
            <div class="stat-value">${enrolledPatients}</div>
            <div class="stat-sub">across all trials</div>
        </div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card danger">
            <div class="stat-label">Open Adverse Events</div>
            <div class="stat-value">${openAEs}</div>
            <div class="stat-sub"><a href="${ctx}/aeReview">Review now &rarr;</a></div>
        </div>
    </div>
    <div class="col-6 col-lg-3">
        <div class="stat-card ${empty alerts ? '' : 'danger'}">
            <div class="stat-label">Risk Alerts</div>
            <div class="stat-value">${fn:length(alerts)}</div>
            <div class="stat-sub">automated validation rules</div>
        </div>
    </div>
</div>
<div class="row g-3 mb-3">
    <div class="col-md-4"><div class="stat-card danger"><div class="stat-label">Open SAE Reviews</div><div class="stat-value">${openSae}</div><div class="stat-sub">24-hour review clock</div></div></div>
    <div class="col-md-4"><div class="stat-card danger"><div class="stat-label">Overdue Visits</div><div class="stat-value">${overdueVisits}</div><div class="stat-sub"><a href="${ctx}/visits?filter=overdue">Open visit schedule &rarr;</a></div></div></div>
    <div class="col-md-4"><div class="stat-card danger"><div class="stat-label">Missed Visits</div><div class="stat-value">${missedVisits}</div><div class="stat-sub"><a href="${ctx}/visits?filter=missed">Protocol deviations &rarr;</a></div></div></div>
</div>

<div class="panel">
    <h2><i class="bi bi-shield-exclamation text-danger"></i> Risk Alerts</h2>
    <c:choose>
        <c:when test="${empty alerts}">
            <div class="alert alert-success mb-0"><i class="bi bi-check-circle"></i> No active risk alerts. All validation rules are passing.</div>
        </c:when>
        <c:otherwise>
            <c:forEach var="a" items="${alerts}">
                <div class="alert ${a.css} risk-alert">
                    <span class="alert-title"><c:out value="${a.title}"/>:</span>
                    <c:out value="${a.text}"/>
                </div>
            </c:forEach>
        </c:otherwise>
    </c:choose>
</div>

<div class="row g-3">
    <div class="col-lg-8">
        <div class="panel">
            <h2>Trials Overview</h2>
            <div class="table-wrap">
                <table class="ctms-table">
                    <thead><tr><th>ID</th><th>Trial</th><th>Phase</th><th>Enrollment</th><th>Ends</th><th>Status</th></tr></thead>
                    <tbody>
                    <c:forEach var="t" items="${trials}">
                        <tr>
                            <td class="mono">${t.trial_id}</td>
                            <td><strong><c:out value="${t.trial_name}"/></strong><div class="sub"><c:out value="${t.drug_name}"/></div></td>
                            <td>Phase ${t.phase}</td>
                            <td><div class="small">${t.enrolled} / ${t.target_enrollment}</div>
                                <div class="progress"><div class="progress-bar" style="width:${t.pct}%"></div></div></td>
                            <td>${t.end_date}</td>
                            <td><span class="pill pill-${t.status}">${t.status}</span></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
    <div class="col-lg-4">
        <div class="panel">
            <h2>Adverse Events by Severity</h2>
            <canvas id="aeChart" height="190"></canvas>
        </div>
    </div>
</div>

<div class="panel">
    <h2>Recent Activity <a class="small fw-normal ms-2" href="${ctx}/auditLog">View full audit log &rarr;</a></h2>
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>When</th><th>User</th><th>Action</th><th>Details</th></tr></thead>
            <tbody>
            <c:forEach var="l" items="${recentLogs}">
                <tr>
                    <td class="sub">${l.log_time}</td>
                    <td><c:out value="${l.username}"/></td>
                    <td class="mono"><c:out value="${l.action_type}"/></td>
                    <td><c:out value="${l.description}"/></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js"></script>
<script>
    new Chart(document.getElementById('aeChart'), {
        type: 'doughnut',
        data: {
            labels: ['Mild', 'Moderate', 'Severe'],
            datasets: [{ data: [${sevMild}, ${sevModerate}, ${sevSevere}],
                         backgroundColor: ['#10b981', '#f59e0b', '#ef4444'] }]
        },
        options: { plugins: { legend: { position: 'bottom' } } }
    });
</script>
<%@ include file="_footer.jsp" %>
