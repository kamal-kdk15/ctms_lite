# Things to update in the project report so it matches the final code

1. Code snippets (Chapter 7): the classes now live in packages `com.ctmslite.util / filter / servlet`.
   Use the real code from the project, not the older draft snippets.
2. Database name is `ctms_lite` (same as the report). Table `adverse_events` has an extra column
   `occurrence_date` (Date of Occurrence from the AE form) - add it to the table in 5.2.
3. Login: passwords are SHA-256 hashed (`PasswordUtil.sha256`) - the draft snippet compared plain text.
4. Enrollment check 2: the code blocks every trial that is not *Active* (Suspended, Completed, Pending).
5. Consent rule: if consent of an enrolled patient is set to *Withdrawn*, the patient is removed from the trial.
6. Blocked enrollment attempts are saved in `audit_logs` (CONSENT_VIOLATION, SUSPENDED_TRIAL_ACCESS,
   DUPLICATE_ENROLLMENT) and drive the orange / blue risk alerts on the Admin dashboard.
7. Dashboard uses Chart.js (doughnut chart of AEs by severity) - already mentioned in 7.1.
8. Hardware/software table: Tomcat 9.0, JDK 8+, MySQL 8.0, Bootstrap 5 (CDN) - unchanged.
9. Sun Pharma location: use ONE city (Gurgaon or Mumbai) everywhere in the report.
10. Replace "Pass" test results with screenshots of the real application.
