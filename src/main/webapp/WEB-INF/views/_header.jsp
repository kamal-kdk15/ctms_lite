<%-- Common head + navbar. Included by every page AFTER its own page/taglib directives. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><c:out value="${pageTitle}"/> | CTMS Lite</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${ctx}/css/style.css" rel="stylesheet">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark app-nav">
    <div class="container-xl">
        <a class="navbar-brand d-flex align-items-center gap-2"
           href="${ctx}/${sessionScope.role == 'Admin' ? 'adminDashboard' : 'staffDashboard'}">
            <span class="brand-mark"><i class="bi bi-activity"></i></span>
            <span>
                <span class="brand-title d-block">CTMS Lite</span>
                <span class="brand-sub d-block">Clinical Trial Monitoring System</span>
            </span>
        </a>
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#mainNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="mainNav">
            <ul class="navbar-nav me-auto ms-lg-4 gap-lg-1">
                <c:choose>
                    <c:when test="${sessionScope.role == 'Admin'}">
                        <li class="nav-item"><a class="nav-link ${nav == 'dashboard' ? 'active' : ''}" href="${ctx}/adminDashboard"><i class="bi bi-speedometer2"></i> Dashboard</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'trials' ? 'active' : ''}" href="${ctx}/trials"><i class="bi bi-file-earmark-medical"></i> Trials</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'sites' ? 'active' : ''}" href="${ctx}/sites"><i class="bi bi-hospital"></i> Sites</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'visits' ? 'active' : ''}" href="${ctx}/visits"><i class="bi bi-calendar-check"></i> Visits</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'aeReview' ? 'active' : ''}" href="${ctx}/aeReview"><i class="bi bi-exclamation-circle"></i> AE Review</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'audit' ? 'active' : ''}" href="${ctx}/auditLog"><i class="bi bi-clock-history"></i> Audit Log</a></li>
                        <li class="nav-item"><a class="nav-link" href="${ctx}/users"><i class="bi bi-person-lock"></i> Users</a></li>
                    </c:when>
                    <c:otherwise>
                        <li class="nav-item"><a class="nav-link ${nav == 'dashboard' ? 'active' : ''}" href="${ctx}/staffDashboard"><i class="bi bi-speedometer2"></i> Dashboard</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'patients' ? 'active' : ''}" href="${ctx}/patients"><i class="bi bi-people"></i> Patients &amp; Consent</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'enroll' ? 'active' : ''}" href="${ctx}/enrollPatient"><i class="bi bi-person-plus"></i> Enroll Patient</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'aeReport' ? 'active' : ''}" href="${ctx}/aeReport"><i class="bi bi-heart-pulse"></i> Report AE</a></li>
                        <li class="nav-item"><a class="nav-link ${nav == 'visits' ? 'active' : ''}" href="${ctx}/visits"><i class="bi bi-calendar-check"></i> Visit Schedule</a></li>
                    </c:otherwise>
                </c:choose>
            </ul>
            <div class="d-flex align-items-center gap-2 text-white">
                <span class="fw-semibold"><c:out value="${sessionScope.username}"/></span>
                <span class="role-badge"><c:out value="${sessionScope.role}"/></span>
                <a class="btn btn-sm btn-outline-light ms-2" href="${ctx}/logout"><i class="bi bi-box-arrow-right"></i> Logout</a>
            </div>
        </div>
    </div>
</nav>

<main class="container-xl py-4">
<c:if test="${not empty sessionScope.flash}">
    <div class="alert alert-${sessionScope.flashType} alert-dismissible fade show" role="alert">
        <c:out value="${sessionScope.flash}"/>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <c:remove var="flash" scope="session"/>
    <c:remove var="flashType" scope="session"/>
</c:if>
