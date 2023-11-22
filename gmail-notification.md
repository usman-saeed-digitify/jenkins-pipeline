# Generate a new app password in Gmail:


1. Go to your Gmail account settings. Click on your profile picture at the top-right, then click on "Manage your Google Account".
2. On the left side, choose "Security".
3. Scroll down until you find "2-Step Verification" and set it up if not done already. Then, under "2-Step Verification", find "App Passwords" and click it.
4. It might ask for your password to proceed. Then you will see "App Passwords" screen where you can generate a 16 character password for mail access on Jenkins (or any other). You can select "Mail" and "this computer" (or any relevant name), and generate a new 16 character password.


# Configure the Email Notification in Jenkins:


1. Open Jenkins and click on "Manage Jenkins".
2. Click on "Configure System".
3. Scroll down until you see the "Extended E-mail Notification" section and "E-mail Notification" section.
4. In both sections, do the following configuration:
   - SMTP Server: smtp.gmail.com
   - Default user E-mail suffix: @gmail.com (This field can be left blank)
   - Use SMTP Authentication: checked
   - User Name: your Gmail account email address
   - Password: the 16 character app password generated in Step 1
   - SSL: checked
   - SMTP Port: 465
   - Charset: UTF-8
5. Click on "Test configuration by sending test e-mail", enter your e-mail, and click "Test configuration". This should send a test email to verify if your configuration works.
6. Scroll down and hit "Save".


# Add a credential in Jenkins:

1. In your Jenkins dashboard, go to Credentials > System > Global credentials.
2. Click on the "Add Credentials" on the left side.
3. In the Kind dropdown, select "Username with password".
4. Enter your Gmail username in the "Username" field and your Gmail password in the "Password" field.
5. Give the credential an ID (you will use this ID in your Pipeline script to reference the credential).
6. Click OK to save the credential.

# pipeline script for sending gmali notification

```groovy
pipeline {
//    ...
//    ...
//    stages
//    ...
//    ...
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
```