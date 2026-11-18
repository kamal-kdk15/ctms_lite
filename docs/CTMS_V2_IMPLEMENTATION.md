# CTMS Lite v2 - Implementation Notes

## Added modules

| Module | What changed |
|---|---|
| Electronic signatures | Trial status changes and AE review transitions require password re-entry, signature meaning (`Reviewed` / `Approved`), and an attestation. Signature metadata and a SHA-256 record hash are stored in `e_signatures` and mirrored to the audit log. |
| Patient allocation | Enrollment assigns a system-generated arm from the least-populated arms, with a cryptographically secure random tie-break. Allocation history is kept in `randomization_allocations` so a withdrawn subject's allocation is never reused. |
| Visit management | Enrollment creates Baseline, Week 4, Week 12 and Week 24 visits. Staff can mark a visit completed or missed with a reason. |
| Visit risk alerts | Scheduled visits more than three days overdue and explicitly missed visits appear on the Admin dashboard. |
| SAE review clock | Staff explicitly classify an event as an SAE. The Admin review page displays the 24-hour clock and the dashboard flags open SAEs once the 24-hour review deadline passes. |
| Site operations | Admin can register sites, attach sites to trials and set site-level targets. Trial and site actual-versus-target progress is shown on the dashboard. |
| Exports | Audit trail and trial summary can be exported as CSV or PDF. |
| Security | Three failed logins lock the account for 15 minutes. Session inactivity timeout is 15 minutes; the browser displays a warning shortly before expiry. Admin can unlock accounts. |

## Upgrade procedure

This package is based on the uploaded v1 project structure and its MySQL schema.

1. Export/backup the current `ctms_lite` database before changing anything.
2. Run `database/upgrade_v2.sql` once against the existing v1 `ctms_lite` database.
3. Configure the database connection in `src/main/java/com/ctmslite/util/DBConnection.java`.
4. Build using Maven and Java 8+:
   ```bash
   mvn clean package
   ```
5. Deploy `target/ctms-lite.war` to Tomcat 9.
6. Sign in as Admin and open **Sites**. Confirm or create sites and attach at least one site to every active trial.
7. Staff can then enroll patients using the trial/site selector. The system creates the allocation and visit schedule.

For a fresh database, run the original `database/schema.sql` first, then `database/upgrade_v2.sql`.

## Important implementation boundaries

- The migration is intentionally a one-time upgrade, not a repeatable migration runner. Do not execute it twice.
- Existing enrolled subjects are not assigned invented treatment arms or investigative sites. Their historical site/allocation must be confirmed from source records. The migration creates their visit schedule where an enrollment timestamp exists.
- New allocations use balanced least-populated-arm selection with a secure random tie-break. This is a demonstrator allocation workflow, not a validated block-randomization schedule.
- The electronic-signature hash is a demonstrator integrity marker. It is not a digital signature backed by a protected key and does not by itself satisfy 21 CFR Part 11.
- This project remains an educational prototype. Production use would require validated requirements, protocol-specific workflows, validated randomization, stronger password hashing, immutable audit storage, key management, role/access reviews, SOPs, validation evidence, backup/restore testing, and quality/regulatory approval.
