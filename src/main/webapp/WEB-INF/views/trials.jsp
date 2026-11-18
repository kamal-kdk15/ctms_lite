<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<c:set var="pageTitle" value="Trials"/>
<c:set var="nav" value="trials"/>
<%@ include file="_header.jsp" %>

<div class="page-head">
    <div>
        <h1>Trial Registration &amp; Management <span class="tag">Admin</span></h1>
        <p>Register clinical trials and update their lifecycle status. Suspended trials automatically block new enrollments.</p>
    </div>
    <button class="btn btn-teal" data-bs-toggle="modal" data-bs-target="#trialModal"><i class="bi bi-plus-circle"></i> Register New Trial</button>
</div>

<div class="panel">
    <div class="table-wrap">
        <table class="ctms-table">
            <thead><tr><th>ID</th><th>Trial &amp; Drug</th><th>Phase</th><th>Duration</th><th>Enrollment Progress</th><th>Status</th><th>Update Status</th></tr></thead>
            <tbody>
            <c:forEach var="t" items="${trials}">
                <tr>
                    <td class="mono">${t.trial_id}</td>
                    <td><strong><c:out value="${t.trial_name}"/></strong><div class="sub"><c:out value="${t.drug_name}"/></div></td>
                    <td>Phase ${t.phase}</td>
                    <td class="sub">${t.start_date}<br>to ${t.end_date}</td>
                    <td>
                        <div class="small fw-semibold">${t.enrolled} / ${t.target_enrollment}</div>
                        <div class="progress"><div class="progress-bar" style="width:${t.target_enrollment > 0 ? t.enrolled * 100 / t.target_enrollment : 0}%"></div></div>
                    </td>
                    <td><span class="pill pill-${t.status}">${t.status}</span></td>
                    <td>
                        <form method="post" action="${ctx}/trials" class="inline-form">
                            <input type="hidden" name="action" value="updateStatus">
                            <input type="hidden" name="trial_id" value="${t.trial_id}">
                            <select name="status" class="form-select form-select-sm">
                                <option ${t.status == 'Active' ? 'selected' : ''}>Active</option>
                                <option ${t.status == 'Pending' ? 'selected' : ''}>Pending</option>
                                <option ${t.status == 'Suspended' ? 'selected' : ''}>Suspended</option>
                                <option ${t.status == 'Completed' ? 'selected' : ''}>Completed</option>
                            </select>
                            <div class="esign-inline">
                                <select name="sig_meaning" class="form-select form-select-sm" required>
                                    <option value="">Signature meaning</option><option>Reviewed</option><option>Approved</option>
                                </select>
                                <input type="password" name="sig_password" class="form-control form-control-sm" placeholder="Re-enter password" required>
                                <label class="small"><input type="checkbox" name="sig_attest" required> I certify this electronic signature is my binding approval for this action.</label>
                            </div>
                            <button class="btn btn-sm btn-outline-secondary" type="submit">Sign &amp; Update</button>
                        </form>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</div>

<!-- Register trial modal -->
<div class="modal fade" id="trialModal" tabindex="-1">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <form class="modal-content" method="post" action="${ctx}/trials">
            <input type="hidden" name="action" value="create">
            <div class="modal-header"><h5 class="modal-title">Register New Clinical Trial</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
            <div class="modal-body">
                <div class="row g-3">
                    <div class="col-12"><label class="form-label">Trial Name *</label>
                        <input type="text" name="trial_name" class="form-control" maxlength="100" required></div>
                    <div class="col-md-6"><label class="form-label">Drug Name *</label>
                        <input type="text" name="drug_name" class="form-control" maxlength="100" required></div>
                    <div class="col-md-3"><label class="form-label">Phase *</label>
                        <select name="phase" class="form-select"><option>I</option><option>II</option><option>III</option><option>IV</option></select></div>
                    <div class="col-md-3"><label class="form-label">Initial Status *</label>
                        <select name="status" class="form-select"><option>Pending</option><option>Active</option><option>Suspended</option><option>Completed</option></select></div>
                    <div class="col-md-6"><label class="form-label">Start Date *</label>
                        <input type="date" name="start_date" class="form-control" required></div>
                    <div class="col-md-6"><label class="form-label">End Date *</label>
                        <input type="date" name="end_date" class="form-control" required></div>
                    <div class="col-md-6"><label class="form-label">Total Enrollment Target *</label>
                        <input type="number" name="target_enrollment" class="form-control" min="1" max="100000" value="100" required></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Cancel</button>
                <button type="submit" class="btn btn-teal">Save Trial</button>
            </div>
        </form>
    </div>
</div>
<%@ include file="_footer.jsp" %>
