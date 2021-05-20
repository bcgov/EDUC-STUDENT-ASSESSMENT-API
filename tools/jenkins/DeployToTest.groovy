pipeline{
    agent {
        kubernetes {
            label 'maven'
            cloud 'openshift'
            defaultContainer 'jnlp'
            serviceAccount 'jenkins'
            yaml """
        kind: Pod
        metadata:
          name: jenkins-slave
        spec:
          containers:
          - name: jnlp
            image: registry.access.redhat.com/openshift3/jenkins-agent-maven-35-rhel7
            privileged: false
            alwaysPullImage: false
            workingDir: /tmp
            ttyEnabled: false
            volumeMounts:
            - mountPath: '/home/jenkins/.m2'
              name: pvc
          volumes:
          - name: pvc
            persistentVolumeClaim:
              claimName: 'maven-slave-pvc'
      """
        }
    }

    environment{
        OCP_PROJECT = '77c02f-dev'
        IMAGE_PROJECT = '77c02f-tools'
        IMAGE_TAG = 'latest'
        APP_SUBDOMAIN_SUFFIX = '77c02f-test'
        REPO_NAME = 'educ-student-assessment-api'
        JOB_NAME = 'main'
        APP_NAME = 'educ-student-assessment-api'
        APP_DOMAIN = 'apps.silver.devops.gov.bc.ca'
    }

    stages{
        stage('Promote to TEST') {
            steps{
                script {
                    openshift.withCluster() {
                        openshift.withProject(OCP_PROJECT) {
                            def dcTemplate = openshift.process('-f', 'tools/openshift/api.dc.yaml',
                                    "REPO_NAME=${REPO_NAME}",
                                    "JOB_NAME=${JOB_NAME}",
                                    "NAMESPACE=${IMAGE_PROJECT}",
                                    "APP_NAME=${APP_NAME}",
                                    "HOST_ROUTE=${REPO_NAME}-${APP_SUBDOMAIN_SUFFIX}.${APP_DOMAIN}",
                                    "TAG=${IMAGE_TAG}"
                            )

                            echo "Applying Deployment ${APP_NAME}"
                            def dc = openshift.apply(dcTemplate).narrow('dc')

                            echo "Waiting for deployment to roll out"
                            // Wait for deployments to roll out
                            timeout(10) {
                                dc.rollout().status('--watch=true')
                            }
                        }
                    }
                }
            }
            post{
                success{
                    echo 'Deployment to Dev was successful'
                }
                failure{
                    echo 'Deployment to Dev failed'
                }
            }
        }
    }
}
