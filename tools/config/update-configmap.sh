envValue=$1
APP_NAME=$2
OPENSHIFT_NAMESPACE=$3
COMMON_NAMESPACE=$4
APP_NAME_UPPER=${APP_NAME^^}
DB_JDBC_CONNECT_STRING=$5
DB_PWD=$6
DB_USER=$7
SPLUNK_TOKEN=$8
CHES_CLIENT_ID=$9
CHES_CLIENT_SECRET="${10}"
CHES_TOKEN_URL="${11}"
CHES_ENDPOINT_URL="${12}"
S3_ACCESS_KEY_ID="${13}"
S3_ACCESS_SECRET_KEY="${14}"
S3_BUCKET_NAME="${15}"
S3_ENDPOINT_URL="${16}"
SDC_NAMESPACE="${17}"
COMS_ENDPOINT_URL="${18}"
TZVALUE="America/Vancouver"
SOAM_KC_REALM_ID="master"
SOAM_KC=soam-$envValue.apps.silver.devops.gov.bc.ca

SOAM_KC_LOAD_USER_ADMIN=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"username": "\(.*\)"/\1/p' | base64 --decode)
SOAM_KC_LOAD_USER_PASS=$(oc -n $COMMON_NAMESPACE-$envValue -o json get secret sso-admin-${envValue} | sed -n 's/.*"password": "\(.*\)",/\1/p' | base64 --decode)
NATS_URL="nats://nats.${COMMON_NAMESPACE}-${envValue}.svc.cluster.local:4222"

echo Fetching SOAM token
TKN=$(curl -s \
  -d "client_id=admin-cli" \
  -d "username=$SOAM_KC_LOAD_USER_ADMIN" \
  -d "password=$SOAM_KC_LOAD_USER_PASS" \
  -d "grant_type=password" \
  "https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" | jq -r '.access_token')

echo
echo Retrieving client ID for assessment-api-service
ASSESSMENT_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="assessment-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for assessment-api-service
ASSESSMENT_APIServiceClientSecret=$([ -n "$ASSESSMENT_APIServiceClientID" ] && curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$ASSESSMENT_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Removing Assessment API client if exists
curl -sX DELETE "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$ASSESSMENT_APIServiceClientID" \
  -H "Authorization: Bearer $TKN"

echo Writing scope READ_ASSESSMENT_REPORT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Assessment report\",\"id\": \"READ_ASSESSMENT_REPORT\",\"name\": \"READ_ASSESSMENT_REPORT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo Writing scope WRITE_ASSESSMENT_FILES
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Assessment files\",\"id\": \"WRITE_ASSESSMENT_FILES\",\"name\": \"WRITE_ASSESSMENT_FILES\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo Writing scope WRITE_ASSESSMENT_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Assessment Students\",\"id\": \"WRITE_ASSESSMENT_STUDENT\",\"name\": \"WRITE_ASSESSMENT_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo Writing scope READ_ASSESSMENT_STUDENT
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Assessment Students\",\"id\": \"READ_ASSESSMENT_STUDENT\",\"name\": \"READ_ASSESSMENT_STUDENT\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

if [[ -n "$ASSESSMENT_APIServiceClientID" && -n "$ASSESSMENT_APIServiceClientSecret" && ("$envValue" = "dev" || "$envValue" = "test") ]]; then
  echo
  echo Creating client assessment-api-service with secret
  curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TKN" \
    -d "{\"clientId\" : \"assessment-api-service\",\"secret\" : \"$ASSESSMENT_APIServiceClientSecret\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\", \"READ_SDC_COLLECTION\", \"READ_SDC_SCHOOL_COLLECTION_STUDENT\", \"READ_INDEPENDENT_AUTHORITY\", \"READ_ASSESSMENT_USERS\",\"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
else
  echo
    echo Creating client assessment-api-service without secret
    curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
      -H "Content-Type: application/json" \
      -H "Authorization: Bearer $TKN" \
      -d "{\"clientId\" : \"assessment-api-service\",\"surrogateAuthRequired\" : false,\"enabled\" : true,\"clientAuthenticatorType\" : \"client-secret\",\"redirectUris\" : [ ],\"webOrigins\" : [ ],\"notBefore\" : 0,\"bearerOnly\" : false,\"consentRequired\" : false,\"standardFlowEnabled\" : false,\"implicitFlowEnabled\" : false,\"directAccessGrantsEnabled\" : false,\"serviceAccountsEnabled\" : true,\"publicClient\" : false,\"frontchannelLogout\" : false,\"protocol\" : \"openid-connect\",\"attributes\" : {\"saml.assertion.signature\" : \"false\",\"saml.multivalued.roles\" : \"false\",\"saml.force.post.binding\" : \"false\",\"saml.encrypt\" : \"false\",\"saml.server.signature\" : \"false\",\"saml.server.signature.keyinfo.ext\" : \"false\",\"exclude.session.state.from.auth.response\" : \"false\",\"saml_force_name_id_format\" : \"false\",\"saml.client.signature\" : \"false\",\"tls.client.certificate.bound.access.tokens\" : \"false\",\"saml.authnstatement\" : \"false\",\"display.on.consent.screen\" : \"false\",\"saml.onetimeuse.condition\" : \"false\"},\"authenticationFlowBindingOverrides\" : { },\"fullScopeAllowed\" : true,\"nodeReRegistrationTimeout\" : -1,\"protocolMappers\" : [ {\"name\" : \"Client ID\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientId\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientId\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client Host\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientHost\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientHost\",\"jsonType.label\" : \"String\"}}, {\"name\" : \"Client IP Address\",\"protocol\" : \"openid-connect\",\"protocolMapper\" : \"oidc-usersessionmodel-note-mapper\",\"consentRequired\" : false,\"config\" : {\"user.session.note\" : \"clientAddress\",\"id.token.claim\" : \"true\",\"access.token.claim\" : \"true\",\"claim.name\" : \"clientAddress\",\"jsonType.label\" : \"String\"}} ],\"defaultClientScopes\" : [ \"READ_INSTITUTE_CODES\", \"READ_SCHOOL\", \"READ_DISTRICT\", \"READ_SDC_COLLECTION\", \"READ_SDC_SCHOOL_COLLECTION_STUDENT\", \"READ_INDEPENDENT_AUTHORITY\", \"READ_ASSESSMENT_USERS\", \"web-origins\", \"role_list\", \"profile\", \"roles\", \"email\"],\"optionalClientScopes\" : [ \"address\", \"phone\", \"offline_access\" ],\"access\" : {\"view\" : true,\"configure\" : true,\"manage\" : true}}"
fi

echo
echo Retrieving client ID for assessment-api-service
ASSESSMENT_APIServiceClientID=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq '.[] | select(.clientId=="assessment-api-service")' | jq -r '.id')

echo
echo Retrieving client secret for assessment-api-service
ASSESSMENT_APIServiceClientSecret=$(curl -sX GET "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/clients/$ASSESSMENT_APIServiceClientID/client-secret" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" |
  jq -r '.value')

echo
echo Writing scope READ_ASSESSMENT_SESSIONS
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Read Assessment Sessions Data\",\"id\": \"READ_ASSESSMENT_SESSIONS\",\"name\": \"READ_ASSESSMENT_SESSIONS\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"

echo
echo Writing scope WRITE_ASSESSMENT_SESSIONS
curl -sX POST "https://$SOAM_KC/auth/admin/realms/$SOAM_KC_REALM_ID/client-scopes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TKN" \
  -d "{\"description\": \"Write Assessment Sessions Data\",\"id\": \"WRITE_ASSESSMENT_SESSIONS\",\"name\": \"WRITE_ASSESSMENT_SESSIONS\",\"protocol\": \"openid-connect\",\"attributes\" : {\"include.in.token.scope\" : \"true\",\"display.on.consent.screen\" : \"false\"}}"


###########################################################
#Setup for config-map
###########################################################
SPLUNK_URL="gww.splunk.educ.gov.bc.ca"
FLB_CONFIG="[SERVICE]
   Flush        1
   Daemon       Off
   Log_Level    debug
   HTTP_Server   On
   HTTP_Listen   0.0.0.0
   Parsers_File parsers.conf
[INPUT]
   Name   tail
   Path   /mnt/log/*
   Exclude_Path *.gz,*.zip
   Parser docker
   Mem_Buf_Limit 20MB
[FILTER]
   Name record_modifier
   Match *
   Record hostname \${HOSTNAME}
[OUTPUT]
   Name   stdout
   Match  *
[OUTPUT]
   Name  splunk
   Match *
   Host  $SPLUNK_URL
   Port  443
   TLS         On
   TLS.Verify  Off
   Message_Key $APP_NAME
   Splunk_Token $SPLUNK_TOKEN
"
PARSER_CONFIG="
[PARSER]
    Name        docker
    Format      json
"

THREADS_MIN_SUBSCRIBER=8
THREADS_MAX_SUBSCRIBER=12
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="0 0/2 * * * *"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="PT4M"
SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="PT4M"

SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON="0/2 * * * * *"
SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_MOST_FOR="1900ms"

SCHEDULED_JOBS_SETUP_SESSIONS_CRON="0 0 0 * AUG ?"
SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_LEAST_FOR="1700ms"
SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_MOST_FOR="1900ms"

SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON="0 */30 * * * *"
SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_LEAST_FOR="55s"
SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_MOST_FOR="57s"

ASSESSMENT_NOTIFICATION_EMAIL_FROM="educationdataexchange@gov.bc.ca"

MAXIMUM_DB_POOL_SIZE=25
MINIMUM_IDLE_DB_POOL_SIZE=15
NUMBER_OF_STUDENTS_TO_PROCESS_SAGA=500

EMAIL_SUBJECT_MYED_APPROVAL_NOTIFICATION="Session Assessment Results Available"
EMAIL_MYED_APPROVAL_NOTIFICATION_FROM="trax.support@gov.bc.ca"
EMAIL_TEMPLATE_MYED_APPROVAL_NOTIFICATION="<!DOCTYPE html><html xmlns:th=\"http://www.thymeleaf.org\"><head><meta charset=\"ISO-8859-1\"><title>Session Assessment Results are available</title></head><body>Hello,<br/><br/><span th:text=\"\${$}{currentSession}\"></span> Session Assessment Results are available.<br/><br/>Please let us know if we can be of further assistance.<br><br><b>Student Certification</b><br>Ministry of Education and Child Care</body></html>"
EMAIL_MYED_APPROVAL_NOTIFICATION_TO="support@isw-bc.ca"

if [ "$envValue" = "dev" ]
then
  ASSESSMENT_NOTIFICATION_EMAIL_FROM="dev.educationdataexchange@gov.bc.ca"
  EMAIL_MYED_APPROVAL_NOTIFICATION_TO="marco.1.villeneuve@gov.bc.ca"
elif [ "$envValue" = "test" ]
then
  ASSESSMENT_NOTIFICATION_EMAIL_FROM="test.educationdataexchange@gov.bc.ca"
  EMAIL_MYED_APPROVAL_NOTIFICATION_TO="avisha.1.sodhi@gov.bc.ca"
fi

echo
echo Creating config map "$APP_NAME"-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-config-map --from-literal=TZ=$TZVALUE  --from-literal=EMAIL_MYED_APPROVAL_NOTIFICATION_TO="$EMAIL_MYED_APPROVAL_NOTIFICATION_TO" --from-literal=EMAIL_TEMPLATE_MYED_APPROVAL_NOTIFICATION="$EMAIL_TEMPLATE_MYED_APPROVAL_NOTIFICATION" --from-literal=EMAIL_MYED_APPROVAL_NOTIFICATION_FROM="$EMAIL_MYED_APPROVAL_NOTIFICATION_FROM" --from-literal=EMAIL_SUBJECT_MYED_APPROVAL_NOTIFICATION="$EMAIL_SUBJECT_MYED_APPROVAL_NOTIFICATION" --from-literal=CHES_CLIENT_ID="$CHES_CLIENT_ID" --from-literal=CHES_CLIENT_SECRET="$CHES_CLIENT_SECRET" --from-literal=CHES_TOKEN_URL="$CHES_TOKEN_URL" --from-literal=CHES_ENDPOINT_URL="$CHES_ENDPOINT_URL" --from-literal=S3_ACCESS_KEY_ID="$S3_ACCESS_KEY_ID" --from-literal=S3_ACCESS_SECRET_KEY="$S3_ACCESS_SECRET_KEY" --from-literal=S3_BUCKET_NAME="$S3_BUCKET_NAME" --from-literal=S3_ENDPOINT_URL="$S3_ENDPOINT_URL" --from-literal=COMS_ENDPOINT_URL="$COMS_ENDPOINT_URL" --from-literal=STUDENT_API_URL="http://student-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student" --from-literal=ASSESSMENT_NOTIFICATION_EMAIL_FROM=$ASSESSMENT_NOTIFICATION_EMAIL_FROM --from-literal=JDBC_URL="$DB_JDBC_CONNECT_STRING" --from-literal=PURGE_RECORDS_SAGA_AFTER_DAYS=400 --from-literal=SCHEDULED_JOBS_PURGE_OLD_SAGA_RECORDS_CRON="@midnight" --from-literal=DB_USERNAME="$DB_USER" --from-literal=DB_PASSWORD="$DB_PWD" --from-literal=SPRING_SECURITY_LOG_LEVEL=INFO --from-literal=SPRING_WEB_LOG_LEVEL=INFO --from-literal=APP_LOG_LEVEL=INFO --from-literal=SPRING_BOOT_AUTOCONFIG_LOG_LEVEL=INFO --from-literal=SPRING_SHOW_REQUEST_DETAILS=false --from-literal=SPRING_JPA_SHOW_SQL="false" --from-literal=TOKEN_ISSUER_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID" --from-literal=TOKEN_URL="https://$SOAM_KC/auth/realms/$SOAM_KC_REALM_ID/protocol/openid-connect/token" --from-literal=NATS_MAX_RECONNECT=60 --from-literal=NATS_URL=$NATS_URL --from-literal=CLIENT_ID="assessment-api-service" --from-literal=CLIENT_SECRET="$ASSESSMENT_APIServiceClientSecret" --from-literal=THREADS_MIN_SUBSCRIBER="$THREADS_MIN_SUBSCRIBER" --from-literal=THREADS_MAX_SUBSCRIBER="$THREADS_MAX_SUBSCRIBER" --from-literal=INSTITUTE_API_URL="http://institute-api-master.$COMMON_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/institute" --from-literal=SDC_API_URL="http://student-data-collection-api-master.$SDC_NAMESPACE-$envValue.svc.cluster.local:8080/api/v1/student-data-collection" --from-literal=MAXIMUM_DB_POOL_SIZE="$MAXIMUM_DB_POOL_SIZE" --from-literal=MINIMUM_IDLE_DB_POOL_SIZE="$MINIMUM_IDLE_DB_POOL_SIZE" --from-literal=SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON="$SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_LEAST_FOR" --from-literal=NUMBER_OF_STUDENTS_TO_PROCESS_SAGA="$NUMBER_OF_STUDENTS_TO_PROCESS_SAGA" --from-literal=SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PUBLISH_LOADED_ASSESSMENT_STUDENTS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_SETUP_SESSIONS_CRON="$SCHEDULED_JOBS_SETUP_SESSIONS_CRON" --from-literal=SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_EXTRACT_UNCOMPLETED_SAGAS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_SETUP_SESSIONS_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON="$SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON" --from-literal=SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_PURGE_COMPLETED_RESULTS_FROM_STAGING_CRON_LOCK_AT_MOST_FOR" --from-literal=SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON="$SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON" --from-literal=SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON_LOCK_AT_LEAST_FOR="$SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON_LOCK_AT_LEAST_FOR" --from-literal=SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON_LOCK_AT_MOST_FOR="$SCHEDULED_JOBS_TRANSFER_STAGED_STUDENTS_CRON_LOCK_AT_MOST_FOR"  --dry-run -o yaml | oc apply -f -

echo
echo Setting environment variables for $APP_NAME-$SOAM_KC_REALM_ID application
oc -n "$OPENSHIFT_NAMESPACE"-"$envValue" set env --from=configmap/$APP_NAME-config-map deployment/$APP_NAME-$SOAM_KC_REALM_ID

echo Creating config map "$APP_NAME"-flb-sc-config-map
oc create -n "$OPENSHIFT_NAMESPACE"-"$envValue" configmap "$APP_NAME"-flb-sc-config-map --from-literal=fluent-bit.conf="$FLB_CONFIG" --from-literal=parsers.conf="$PARSER_CONFIG" --dry-run -o yaml | oc apply -f -
