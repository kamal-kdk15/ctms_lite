-- CTMS Lite v2 : demo-data cleanup
-- The sample patients were enrolled months ago, so their early visits were still 'Scheduled'
-- and showed up as "Missed Visit Risk" on the Admin dashboard.
-- This marks past visits as Completed on their due date, exactly what the app's own
-- "Mark complete" action does. Upcoming visits stay Scheduled.
-- This is sample/demo data only - do NOT run on real study data.
USE ctms_lite;

UPDATE visits
SET status = 'Completed',
    actual_date = due_date,
    note = NULL
WHERE status = 'Scheduled'
  AND due_date < DATE_SUB(CURDATE(), INTERVAL 3 DAY);
