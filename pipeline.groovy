pipeline {
    agent any

    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'test', description: 'Branch to build')
    }

    environment {
        APPLICATION_NAME = 'my-notes-app'
        APPLICATION_REPO = 'https://github.com/ChaimaeFalih9/TheTipTopDSP'
        REPO_BRANCH = 'test'
        GCR_PROJECT_ID = 'poetic-nova-403717'
        GCR_IMAGE_NAME = '${APPLICATION_NAME}'
        GCR_IMAGE_TAG = 'test'
        GCP_VM_INSTANCE_NAME = 'test'
        GCP_ZONE = 'us-west4-b'
        GAR_REPO_NAME = 'dispthetiptop'
        REGION = 'us-west4'
        GCLOUD_CREDS = credentials('gcloud-cred')
        GCP_USER_NAME = 'admin-mac'
        DOCKER_HUB_USER = 'chaimaefalih'
        DOCKER_HUB_PASS = 'ChaimaeFalih@321'
        HOST_PORT = '8000'
        CONTAINER_PORT = '8000'
    }

    stages {
        stage("Checkout") {
           when {
            anyOf {
                branch "${REPO_BRANCH}"
                changeRequest target: "${REPO_BRANCH}"
                }
            }
            steps {
                script {
                    def branch = params.BRANCH_NAME ?: env.GIT_BRANCH
                    echo "Cloning the code ${branch}"
                    git url: "${APPLICATION_REPO}", branch: branch
                }
            }
        }
        stage('Unit Tests') {
            steps {
                echo "Running Unit Tests"
                sh 'mvn test'  // Run unit tests using Maven and JUnit
            }
        }
        stage('Build') {
            steps {
                echo "Building the jar file"
                sh 'mvn clean package'  // Build the Spring Boot application using Maven
            }
        }
        stage("Docker Image Build") {
            steps {
                echo "Building the image"
                sh "docker build -t ${APPLICATION_NAME} ."
            }
        }
        stage("Push to GAR") {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gcloud-cred', variable: 'GCP_KEY_FILE')]) {
                        // Authenticate Docker to GAR with service account
                        sh "gcloud auth activate-service-account --key-file ${GCP_KEY_FILE}"
                        sh "gcloud auth configure-docker ${REGION}-docker.pkg.dev"

                        // Tag the Docker image
                        sh "docker tag ${APPLICATION_NAME} ${REGION}-docker.pkg.dev/${GCR_PROJECT_ID}/${GAR_REPO_NAME}/${APPLICATION_NAME}:${GCR_IMAGE_TAG}"

                        // Push the Docker image
                        sh "docker push ${REGION}-docker.pkg.dev/${GCR_PROJECT_ID}/${GAR_REPO_NAME}/${APPLICATION_NAME}:${GCR_IMAGE_TAG}"
                    }
                }
            }
        }
        stage('Deploy to GCP VM') {
            steps {
                script {
                    withCredentials([file(credentialsId: 'gcloud-cred', variable: 'GCP_KEY_FILE')]) {
                        // Activate the service account with the key file
                        sh "gcloud auth activate-service-account --key-file ${GCP_KEY_FILE}"

                        // Add the Artifact Registry Docker repository to the local Docker client
//                        sh "gcloud artifacts docker add-repo ${REGION}-docker.pkg.dev/${GCR_PROJECT_ID}/${GAR_REPO_NAME}"
                    }

                    // Set the GCP project
//                    sh "gcloud config set project ${GCR_PROJECT_ID}"

                    // SSH into the GCP VM and pull & run the Docker container
                    sh "gcloud compute ssh ${GCP_USER_NAME}@${GCP_VM_INSTANCE_NAME} --zone ${GCP_ZONE} --command='" +
                            // "gcloud auth configure-docker us-west4-docker.pkg.dev && "+
                            "docker pull ${REGION}-docker.pkg.dev/${GCR_PROJECT_ID}/${GAR_REPO_NAME}/${APPLICATION_NAME}:${GCR_IMAGE_TAG} && " +
                            "docker stop ${APPLICATION_NAME} && " +
                            "docker rm ${APPLICATION_NAME} && " +
                            "docker run -d --name ${APPLICATION_NAME} -p ${HOST_PORT}:${CONTAINER_PORT} ${REGION}-docker.pkg.dev/${GCR_PROJECT_ID}/${GAR_REPO_NAME}/${APPLICATION_NAME}:${GCR_IMAGE_TAG}'"
                }
            }
        }

    }
    post {
        failure {
            emailext (
                    to: 'your-email@gmail.com',
                    subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                    body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>""",
                    mimeType: 'text/html'
            )
        }
    }

    post {
        failure {
            script {
                withCredentials([usernamePassword(
                        credentialsId: 'emailCreds',
                        passwordVariable: 'password',
                        usernameVariable: 'username'
                )]) {
                    emailext (
                            to: 'your-email@gmail.com',
                            subject: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                            body: """<p>FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
                            <p>Check console output at '<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>'</p>""",
                            mimeType: 'text/html',
                            replyTo: '$username',
                            from: '$username',
                            smtpAuthUsername: '$username',
                            smtpAuthPassword: '$password',
                            smtpHost: 'smtp.gmail.com',
                            smtpPort: '465',
                            smtpStarttls: true
                    )
                }
            }
        }
        success {
            script {
                withCredentials([usernamePassword(
                        credentialsId: 'emailCreds',
                        passwordVariable: 'password',
                        usernameVariable: 'username'
                )]) {
                    emailext (
                            to: 'your-email@gmail.com',
                            subject: "SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                            body: """<p>SUCCESS: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'</p>
                            <p>Check console output at '<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>'</p>""",
                            mimeType: 'text/html',
                            replyTo: '$username',
                            from: '$username',
                            smtpAuthUsername: '$username',
                            smtpAuthPassword: '$password',
                            smtpHost: 'smtp.gmail.com',
                            smtpPort: '465',
                            smtpStarttls: true
                    )
                }
            }
        }
    }


}


