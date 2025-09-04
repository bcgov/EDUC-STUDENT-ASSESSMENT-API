UPDATE PROVINCIAL_SPECIAL_CASE_CODE
SET DESCRIPTION = 'Aegrotat Standing – the student has been excused from writing a Provincial Graduation Assessment due to unpredictable circumstances that render the student unable to write it, even at a future session.',
    LABEL = 'AEG'
WHERE PROVINCIAL_SPECIAL_CASE_CODE = 'A';

UPDATE PROVINCIAL_SPECIAL_CASE_CODE
SET DESCRIPTION = 'Non-Complete – the student did not complete enough of the assessment to generate a proficiency score.',
    LABEL = 'NC'
WHERE PROVINCIAL_SPECIAL_CASE_CODE = 'X';

UPDATE PROVINCIAL_SPECIAL_CASE_CODE
SET DESCRIPTION = 'Assessment requirement exempt.',
    LABEL = 'XMT'
WHERE PROVINCIAL_SPECIAL_CASE_CODE = 'E';

UPDATE PROVINCIAL_SPECIAL_CASE_CODE
SET DESCRIPTION = 'Disqualification – an assessment is disqualified when the assessment rules are breached. No mark is given for the assessment.',
    LABEL = 'DSQ'
WHERE PROVINCIAL_SPECIAL_CASE_CODE = 'Q';