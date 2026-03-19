--Create table temp_assmnt_load via CSV import in DBeaver

select "SESSION_DATE", count(*) from temp_assmnt_load tal group by "SESSION_DATE" ;

--Data Cleanup
update temp_assmnt_load tal
set "SESSION_DATE" = '202601'
where tal."SESSION_DATE" = '202701';

update temp_assmnt_load tal
set "SESSION_DATE" = '202601'
where tal."SESSION_DATE" = '202603';

update temp_assmnt_load tal
set "SESSION_DATE" = '202601'
where tal."SESSION_DATE" = '202602';

update temp_assmnt_load tal
set "SESSION_DATE" = '202601'
where tal."SESSION_DATE" = '202612';

--Create the index
CREATE INDEX temp_assmnt_load_pen_idx ON api_eas.temp_assmnt_load USING btree ("PEN", "SESSION_DATE", "ASSESSMENT_SHORT_NAME");

--Remove the dups
DELETE FROM temp_assmnt_load
WHERE ctid NOT IN (
    SELECT MIN(ctid)
    FROM temp_assmnt_load
    GROUP BY "SESSION_DATE", "PEN", "ASSESSMENT_SHORT_NAME"
);


--Update of our table
UPDATE assessment_student asm_stu
SET    local_assessment_id = tmp."STUDENT_ASSESSMENT_ID"
    FROM   assessment asm
JOIN   assessment_session sess ON sess.session_id           = asm.session_id
    JOIN   temp_assmnt_load   tmp  ON tmp."SESSION_DATE"        = sess.course_year || sess.course_month
    AND tmp."ASSESSMENT_SHORT_NAME" = asm.assessment_type_code
WHERE  asm_stu.assessment_id                                = asm.assessment_id
  AND    asm_stu.pen                                          = tmp."PEN"
  AND    asm_stu.local_assessment_id IS DISTINCT FROM tmp."STUDENT_ASSESSMENT_ID";