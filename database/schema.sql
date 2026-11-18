-- =====================================================================
-- CTMS Lite : A Web-Based Clinical Trial Monitoring System
-- IGNOU BCA Project (BCSP-064)
-- Database : ctms_lite   |   RDBMS : MySQL 8.0+
-- Tables   : users, trials, patients, consent_records,
--            adverse_events, audit_logs
-- =====================================================================

DROP DATABASE IF EXISTS ctms_lite;
CREATE DATABASE ctms_lite
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE ctms_lite;

-- ---------------------------------------------------------------------
-- 1. users  (Role-Based Access: Admin / Staff)
--    Passwords are stored as SHA-256 hashes, never as plain text.
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id     INT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('Admin','Staff') NOT NULL DEFAULT 'Staff',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. trials
-- ---------------------------------------------------------------------
CREATE TABLE trials (
    trial_id    INT AUTO_INCREMENT PRIMARY KEY,
    trial_name  VARCHAR(100) NOT NULL,
    drug_name   VARCHAR(100) NOT NULL,
    phase       ENUM('I','II','III','IV') NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    status      ENUM('Active','Pending','Suspended','Completed')
                NOT NULL DEFAULT 'Pending',
    created_by  INT NOT NULL,
    CONSTRAINT chk_trial_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_trials_user  FOREIGN KEY (created_by)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. patients
--    enrolled_trial = NULL  -> patient registered but not yet enrolled
-- ---------------------------------------------------------------------
CREATE TABLE patients (
    patient_id      INT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    age             INT NOT NULL,
    gender          ENUM('Male','Female','Other') NOT NULL,
    enrolled_trial  INT NULL,
    consent_status  ENUM('Obtained','Pending','Withdrawn')
                    NOT NULL DEFAULT 'Pending',
    enrolled_by     INT NULL,
    enrolled_at     TIMESTAMP NULL,
    CONSTRAINT chk_patient_age CHECK (age BETWEEN 0 AND 120),
    CONSTRAINT fk_patients_trial FOREIGN KEY (enrolled_trial)
        REFERENCES trials(trial_id),
    CONSTRAINT fk_patients_user  FOREIGN KEY (enrolled_by)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 4. consent_records  (one record per patient per trial)
-- ---------------------------------------------------------------------
CREATE TABLE consent_records (
    consent_id      INT AUTO_INCREMENT PRIMARY KEY,
    patient_id      INT NOT NULL,
    trial_id        INT NOT NULL,
    consent_status  ENUM('Obtained','Pending','Withdrawn') NOT NULL,
    recorded_by     INT NOT NULL,
    recorded_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_consent UNIQUE (patient_id, trial_id),
    CONSTRAINT fk_consent_patient FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id),
    CONSTRAINT fk_consent_trial   FOREIGN KEY (trial_id)
        REFERENCES trials(trial_id),
    CONSTRAINT fk_consent_user    FOREIGN KEY (recorded_by)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 5. adverse_events
-- ---------------------------------------------------------------------
CREATE TABLE adverse_events (
    ae_id            INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT NOT NULL,
    trial_id         INT NOT NULL,
    description      TEXT NOT NULL,
    severity         ENUM('Mild','Moderate','Severe') NOT NULL,
    status           ENUM('Open','Under Review','Resolved')
                     NOT NULL DEFAULT 'Open',
    occurrence_date  DATE NOT NULL,
    reported_by      INT NOT NULL,
    reported_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ae_patient FOREIGN KEY (patient_id)
        REFERENCES patients(patient_id),
    CONSTRAINT fk_ae_trial   FOREIGN KEY (trial_id)
        REFERENCES trials(trial_id),
    CONSTRAINT fk_ae_user    FOREIGN KEY (reported_by)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. audit_logs  (21 CFR Part 11 style audit trail)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    action_type     VARCHAR(100) NOT NULL,
    affected_table  VARCHAR(50),
    description     TEXT,
    `timestamp`     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Helpful indexes for dashboard / alert queries
CREATE INDEX idx_ae_severity_status ON adverse_events (severity, status);
CREATE INDEX idx_trials_status_end  ON trials (status, end_date);
CREATE INDEX idx_audit_time         ON audit_logs (`timestamp`);

-- =====================================================================
-- SAMPLE DATA
-- Login credentials for testing:
--   admin  / admin123
--   staff1 / staff123
--   staff2 / staff123
-- =====================================================================

INSERT INTO users (username, password, role) VALUES
('admin',  SHA2('admin123', 256), 'Admin'),
('staff1', SHA2('staff123', 256), 'Staff'),
('staff2', SHA2('staff123', 256), 'Staff');

INSERT INTO trials
    (trial_name, drug_name, phase, start_date, end_date, status, created_by)
VALUES
('Efficacy of Metformin XR in Type 2 Diabetes',      'Metformin XR',  'III', '2026-03-01', '2027-02-28', 'Active',    1),
('Telmisartan in Stage 1 Hypertension',              'Telmisartan',   'II',  '2026-05-15', DATE_ADD(CURDATE(), INTERVAL 3 DAY), 'Active', 1),
('Long-term Safety of Atorvastatin in Dyslipidemia', 'Atorvastatin',  'IV',  '2026-01-10', '2026-12-31', 'Active',    1),
('Safety Study of Novel Anti-inflammatory SP-204',   'SP-204',        'I',   '2026-12-01', '2027-06-30', 'Pending',   1),
('Dapagliflozin in Heart Failure Patients',          'Dapagliflozin', 'II',  '2026-02-01', '2027-01-31', 'Suspended', 1);

INSERT INTO patients
    (name, age, gender, enrolled_trial, consent_status, enrolled_by, enrolled_at)
VALUES
('Rajesh Kumar',    58, 'Male',   1,    'Obtained',  2, '2026-03-20 10:15:00'),
('Sunita Sharma',   52, 'Female', 1,    'Obtained',  2, '2026-03-22 11:30:00'),
('Amit Verma',      47, 'Male',   2,    'Obtained',  3, '2026-06-05 09:45:00'),
('Priya Nair',      39, 'Female', 2,    'Obtained',  3, '2026-06-08 14:20:00'),
('Mohammed Irfan',  61, 'Male',   3,    'Obtained',  2, '2026-02-12 10:00:00'),
('Anita Desai',     44, 'Female', NULL, 'Pending',   NULL, NULL),
('Vikram Singh',    55, 'Male',   NULL, 'Withdrawn', NULL, NULL),
('Neha Gupta',      36, 'Female', NULL, 'Obtained',  NULL, NULL);

INSERT INTO consent_records
    (patient_id, trial_id, consent_status, recorded_by, recorded_at)
VALUES
(1, 1, 'Obtained', 2, '2026-03-20 10:10:00'),
(2, 1, 'Obtained', 2, '2026-03-22 11:25:00'),
(3, 2, 'Obtained', 3, '2026-06-05 09:40:00'),
(4, 2, 'Obtained', 3, '2026-06-08 14:15:00'),
(5, 3, 'Obtained', 2, '2026-02-12 09:55:00'),
(6, 1, 'Pending',  2, '2026-10-28 12:00:00'),
(7, 3, 'Withdrawn',3, '2026-09-15 16:30:00');

INSERT INTO adverse_events
    (patient_id, trial_id, description, severity, status,
     occurrence_date, reported_by, reported_at)
VALUES
(1, 1, 'Mild nausea after morning dose, resolved without intervention.',
       'Mild',     'Resolved',     '2026-04-02', 2, '2026-04-02 15:00:00'),
(2, 1, 'Dizziness and light-headedness reported for two consecutive days.',
       'Moderate', 'Under Review', '2026-08-18', 2, '2026-08-19 09:30:00'),
(3, 2, 'Sudden drop in blood pressure with fainting episode; hospital visit advised.',
       'Severe',   'Open',         DATE_SUB(CURDATE(), INTERVAL 3 DAY), 3, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 2, 'Mild headache lasting less than one day.',
       'Mild',     'Resolved',     '2026-07-10', 3, '2026-07-10 17:10:00'),
(5, 3, 'Persistent muscle pain in lower limbs, dose under observation.',
       'Moderate', 'Open',         '2026-10-22', 2, '2026-10-23 11:00:00');

INSERT INTO audit_logs (user_id, action_type, affected_table, description, `timestamp`) VALUES
(1, 'LOGIN',               'users',           'User admin (Admin) logged in successfully.',                              DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'TRIAL_CREATED',       'trials',          'Trial "Telmisartan in Stage 1 Hypertension" (Phase II) registered.',       '2026-05-14 10:20:00'),
(2, 'LOGIN',               'users',           'User staff1 (Staff) logged in successfully.',                              DATE_SUB(NOW(), INTERVAL 4 DAY)),
(2, 'PATIENT_ENROLLED',    'patients',        'Patient ID 1 (Rajesh Kumar) enrolled in Trial ID 1.',                      '2026-03-20 10:15:00'),
(3, 'AE_SUBMITTED',        'adverse_events',  'Severe adverse event reported for Patient ID 3 in Trial ID 2.',            DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 'CONSENT_UPDATED',     'consent_records', 'Consent for Patient ID 6 set to Pending.',                                 DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 'AE_STATUS_UPDATE',    'adverse_events',  'AE ID 2 status changed from Open to Under Review.',                        DATE_SUB(NOW(), INTERVAL 20 DAY)),
(3, 'CONSENT_VIOLATION',   'patients',        'Enrollment blocked for Patient ID 7 (Vikram Singh): consent is Withdrawn.', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'SUSPENDED_TRIAL_ACCESS','patients',      'Enrollment blocked for Patient ID 8 (Neha Gupta): Trial ID 5 is suspended.', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'DUPLICATE_ENROLLMENT','patients',        'Enrollment blocked for Patient ID 1 (Rajesh Kumar): already enrolled in Trial ID 1.', DATE_SUB(NOW(), INTERVAL 1 DAY));

-- =====================================================================
-- VERIFICATION QUERIES  (run these one by one and take screenshots)
-- =====================================================================
-- SHOW TABLES;
-- DESCRIBE users;
-- DESCRIBE trials;
-- DESCRIBE patients;
-- DESCRIBE consent_records;
-- DESCRIBE adverse_events;
-- DESCRIBE audit_logs;
-- SELECT user_id, username, role, created_at FROM users;
-- SELECT * FROM trials;
-- SELECT * FROM patients;
-- SELECT * FROM consent_records;
-- SELECT * FROM adverse_events;
-- SELECT * FROM audit_logs;
--
-- Dashboard / risk alert style queries:
-- SELECT COUNT(*) AS active_trials FROM trials WHERE status='Active';
-- SELECT COUNT(*) AS enrolled_patients FROM patients WHERE enrolled_trial IS NOT NULL;
-- SELECT COUNT(*) AS open_aes FROM adverse_events WHERE status='Open';
-- SELECT * FROM adverse_events
--   WHERE severity='Severe' AND status='Open'
--   AND reported_at < NOW() - INTERVAL 48 HOUR;
-- SELECT trial_name, end_date FROM trials
--   WHERE status='Active'
--   AND end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY);
