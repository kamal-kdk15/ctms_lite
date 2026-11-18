-- CTMS Lite v2 upgrade
-- Run ONCE after backing up ctms_lite. This script does NOT drop or recreate the database.
USE ctms_lite;

ALTER TABLE users
    ADD COLUMN failed_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;

ALTER TABLE trials
    ADD COLUMN target_enrollment INT NOT NULL DEFAULT 100;

ALTER TABLE patients
    ADD COLUMN site_id INT NULL,
    ADD COLUMN arm VARCHAR(40) NULL,
    ADD COLUMN randomization_no VARCHAR(32) NULL;

ALTER TABLE adverse_events
    ADD COLUMN is_sae TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN sae_reviewed_at DATETIME NULL;

ALTER TABLE trials
    ADD CONSTRAINT chk_trial_target_v2 CHECK (target_enrollment > 0);

CREATE INDEX idx_patient_trial_site ON patients (enrolled_trial, site_id);
CREATE INDEX idx_sae_review_clock ON adverse_events (is_sae, status, reported_at);

CREATE TABLE IF NOT EXISTS sites (
    site_id INT AUTO_INCREMENT PRIMARY KEY,
    site_code VARCHAR(30) NOT NULL UNIQUE,
    institution VARCHAR(150) NOT NULL,
    location VARCHAR(150) NOT NULL,
    principal_investigator VARCHAR(120) NOT NULL,
    status ENUM('Active','Inactive') NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS trial_sites (
    trial_site_id INT AUTO_INCREMENT PRIMARY KEY,
    trial_id INT NOT NULL,
    site_id INT NOT NULL,
    site_target INT NOT NULL DEFAULT 10,
    UNIQUE KEY uq_trial_site (trial_id, site_id),
    CONSTRAINT fk_trial_sites_trial FOREIGN KEY (trial_id) REFERENCES trials(trial_id),
    CONSTRAINT fk_trial_sites_site FOREIGN KEY (site_id) REFERENCES sites(site_id),
    CONSTRAINT chk_site_target CHECK (site_target > 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS randomization_allocations (
    allocation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    trial_id INT NOT NULL,
    patient_id INT NOT NULL,
    site_id INT NOT NULL,
    allocation_no VARCHAR(32) NOT NULL UNIQUE,
    arm VARCHAR(40) NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_randomized_patient (patient_id),
    KEY idx_randomization_trial_arm (trial_id, arm),
    CONSTRAINT fk_randomization_trial FOREIGN KEY (trial_id) REFERENCES trials(trial_id),
    CONSTRAINT fk_randomization_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    CONSTRAINT fk_randomization_site FOREIGN KEY (site_id) REFERENCES sites(site_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS visits (
    visit_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT NOT NULL,
    visit_no TINYINT NOT NULL,
    visit_name VARCHAR(60) NOT NULL,
    due_date DATE NOT NULL,
    actual_date DATE NULL,
    status ENUM('Scheduled','Completed','Missed','Cancelled') NOT NULL DEFAULT 'Scheduled',
    note VARCHAR(255) NULL,
    UNIQUE KEY uq_patient_visit (patient_id, visit_no),
    KEY idx_visit_due (status, due_date),
    CONSTRAINT fk_visits_patient FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS e_signatures (
    sign_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    record_id INT NOT NULL,
    meaning ENUM('Reviewed','Approved','Verified','Authorized') NOT NULL,
    record_state VARCHAR(255) NOT NULL,
    signed_at DATETIME NOT NULL,
    signature_hash CHAR(64) NOT NULL,
    KEY idx_esign_record (record_type, record_id),
    CONSTRAINT fk_esign_user FOREIGN KEY (user_id) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Seed three demo sites. Existing site codes are left unchanged.
INSERT IGNORE INTO sites (site_code, institution, location, principal_investigator) VALUES
('IND-DEL-01', 'Clinical Research Centre Delhi', 'New Delhi', 'Dr. Arjun Mehta'),
('IND-MUM-02', 'Western Medical Research Centre', 'Mumbai', 'Dr. Meera Iyer'),
('IND-CHD-04', 'North Clinical Research Institute', 'Chandigarh', 'Dr. Sunita Rao');

ALTER TABLE patients
    ADD CONSTRAINT fk_patients_site_v2 FOREIGN KEY (site_id) REFERENCES sites(site_id);

-- Associate each existing trial with one site and preserve its current enrollment target.
INSERT IGNORE INTO trial_sites (trial_id, site_id, site_target)
SELECT t.trial_id, s.site_id, GREATEST(t.target_enrollment, 10)
FROM trials t
CROSS JOIN (SELECT site_id FROM sites ORDER BY site_id LIMIT 1) s;

-- Add a second investigative site to selected demonstration trials.
UPDATE trial_sites ts
JOIN trials t ON t.trial_id = ts.trial_id
SET ts.site_target = GREATEST(10, CEIL(t.target_enrollment / 2));

INSERT IGNORE INTO trial_sites (trial_id, site_id, site_target)
SELECT t.trial_id, s.site_id, GREATEST(10, CEIL(t.target_enrollment / 2))
FROM trials t
JOIN sites s ON s.site_code = CASE
    WHEN t.trial_id IN (1, 2) THEN 'IND-MUM-02'
    ELSE 'IND-CHD-04'
END;

-- Existing subjects are not assigned to an invented site. Their site_id remains NULL
-- until a real site association is confirmed in the study records.

-- Generate a baseline and follow-up schedule for already-enrolled subjects.
INSERT IGNORE INTO visits (patient_id, visit_no, visit_name, due_date, status)
SELECT p.patient_id, v.visit_no, v.visit_name,
       DATE_ADD(DATE(p.enrolled_at), INTERVAL v.offset_days DAY),
       'Scheduled' 
FROM patients p
JOIN (
    SELECT 1 visit_no, 'Visit 1 - Baseline' visit_name, 0 offset_days
    UNION ALL SELECT 2, 'Visit 2 - Week 4', 28
    UNION ALL SELECT 3, 'Visit 3 - Week 12', 84
    UNION ALL SELECT 4, 'Visit 4 - Week 24', 168
) v
WHERE p.enrolled_trial IS NOT NULL AND p.enrolled_at IS NOT NULL;

-- NOTE: existing subjects are intentionally NOT assigned randomization arms retroactively.
-- Do not fabricate treatment allocation for historical subjects.
