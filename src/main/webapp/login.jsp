<%@ page contentType="text/html;charset=UTF-8" language="java" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Login | CTMS Lite</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.2/font/bootstrap-icons.css" rel="stylesheet">
    <link href="${ctx}/css/style.css" rel="stylesheet">
</head>
<body>
<div class="login-wrap">
    <div class="login-card">
        <div class="brand-mark"><i class="bi bi-activity"></i></div>
        <h1 class="h4 text-center fw-bold mb-0">CTMS Lite</h1>
        <p class="text-center text-muted small mb-4">Clinical Trial Monitoring System</p>

        <c:if test="${param.error == 'locked'}">
        <div class="alert alert-danger">Account temporarily locked after three failed attempts. Try again after 15 minutes.</div>
    </c:if>
    <c:if test="${param.error == '1'}"><div class="alert alert-danger py-2">Invalid username or password.</div></c:if>
        <c:if test="${param.error == 'empty'}"><div class="alert alert-warning py-2">Please enter both username and password.</div></c:if>
        <c:if test="${param.error == 'session'}"><div class="alert alert-warning py-2">Please log in to continue.</div></c:if>
        <c:if test="${param.error == 'denied'}"><div class="alert alert-danger py-2">You do not have permission to open that page.</div></c:if>
        <c:if test="${param.error == 'db'}"><div class="alert alert-danger py-2">Cannot connect to the database. Check MySQL and DBConnection.java.</div></c:if>
        <c:if test="${param.msg == 'loggedout'}"><div class="alert alert-success py-2">You have been logged out.</div></c:if>

        <form method="post" action="${ctx}/login" autocomplete="off">
            <div class="mb-3">
                <label class="form-label" for="username">Username</label>
                <input type="text" class="form-control" id="username" name="username" required autofocus>
            </div>
            <div class="mb-3">
                <label class="form-label" for="password">Password</label>
                <input type="password" class="form-control" id="password" name="password" required>
            </div>
            <button type="submit" class="btn btn-teal w-100"><i class="bi bi-box-arrow-in-right"></i> Sign in</button>
        </form>

        <div class="demo-box mt-4">
            <strong>Demo accounts</strong><br>
            Admin: <span class="mono">admin / admin123</span><br>
            Staff: <span class="mono">staff1 / staff123</span>
        </div>
    </div>
</div>
</body>
</html>
