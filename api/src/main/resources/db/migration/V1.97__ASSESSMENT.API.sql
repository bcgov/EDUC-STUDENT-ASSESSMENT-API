UPDATE assessment_type_code
SET display_order = CASE assessment_type_code
    WHEN 'LTE10' THEN 10
    WHEN 'LTP10' THEN 20
    WHEN 'NME10' THEN 30
    WHEN 'NME' THEN 40
    WHEN 'NMF10' THEN 50
    WHEN 'NMF' THEN 60
    WHEN 'LTE12' THEN 70
    WHEN 'LTP12' THEN 80
    WHEN 'LTF12' THEN 90
END,
    update_user = 'ASSESSMENT-API',
    update_date = CURRENT_TIMESTAMP
WHERE assessment_type_code IN ('LTE10', 'LTP10', 'NME10', 'NME', 'NMF10', 'NMF', 'LTE12', 'LTP12', 'LTF12');
