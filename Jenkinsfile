@Library('deploy')
import deploy

def deployLib = new deploy()

node {
   def commitHash, commitHashShort, commitUrl
   def repo = "navikt"
   def application = "syfomottak"
   def committer, committerEmail, changelog, releaseVersion
   def mvnHome = tool "maven-3.5.2"
   def mvn = "${mvnHome}/bin/mvn"
   def appConfig = "nais.yaml"
   def dockerRepo = "repo.adeo.no:5443"
   def groupId = "nais"
   def environment = 't1'
   def zone = 'fss'
   def namespace = 't1'

   stage("Checkout") {
      cleanWs()
      withCredentials([string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
         withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
            sh(script: "git clone https://${token}:x-oauth-basic@github.com/${repo}/${application}.git .")
         }
      }

      commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
      commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
      commitUrl = "https://github.com/${repo}/${application}/commit/${commitHash}"
      committer = sh(script: 'git log -1 --pretty=format:"%an"', returnStdout: true).trim()
      committerEmail = sh(script: 'git log -1 --pretty=format:"%ae"', returnStdout: true).trim()
      changelog = sh(script: 'git log `git describe --tags --abbrev=0`..HEAD --oneline', returnStdout: true)
      notifyGithub(repo, application, 'continuous-integration/jenkins', commitHash, 'pending', "Build #${env.BUILD_NUMBER} has started")

      releaseVersion = "${env.major_version}.${env.BUILD_NUMBER}-${commitHashShort}"
   }

   stage("OWASP CVE check") {
      sh "${mvn} -Powasp dependency-check:check"
   }

   stage("Build & publish") {
      try {
         sh "${mvn} versions:set -B -DnewVersion=${releaseVersion}"
         sh "mkdir -p /tmp/${application}"
         sh "${mvn} -Palltests clean install -Djava.io.tmpdir=/tmp/${application} -B -e"
         slackSend([
            color: 'good',
            message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> (<${commitUrl}|${commitHashShort}>) of ${repo}/${application}@master by ${committer} passed  (${changelog})"
         ])
      } catch (Exception ex) {
         slackSend([
            color: 'danger',
            message: "Build <${env.BUILD_URL}|#${env.BUILD_NUMBER}> (<${commitUrl}|${commitHashShort}>) of ${repo}/${application}@master by ${committer} failed (${changelog})"
         ])
         echo '[FAILURE] Failed to build: ${ex}'
         currentBuild.result = 'FAILURE'
         return
      } finally {
         junit '**/target/surefire-reports/*.xml'
      }

      sh "docker build --build-arg version=${releaseVersion} --build-arg app_name=${application} -t ${dockerRepo}/${application}:${releaseVersion} ."
      withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusUser', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
         sh "curl --fail -v -u ${env.USERNAME}:${env.PASSWORD} --upload-file ${appConfig} https://repo.adeo.no/repository/raw/${groupId}/${application}/${releaseVersion}/nais.yaml"
         sh "docker login -u ${env.USERNAME} -p ${env.PASSWORD} ${dockerRepo} && docker push ${dockerRepo}/${application}:${releaseVersion}"
      }

      sh "${mvn} versions:revert"

      notifyGithub(repo, application, 'continuous-integration/jenkins', commitHash, 'success', "Build #${env.BUILD_NUMBER} has finished")
   }

   stage("Deploy to preprod") {
      withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
               'NO_PROXY=localhost,127.0.0.1,.local,.adeo.no,.nav.no,.aetat.no,.devillo.no,.oera.no',
               'no_proxy=localhost,127.0.0.1,.local,.adeo.no,.nav.no,.aetat.no,.devillo.no,.oera.no'
              ]) {
         System.setProperty("java.net.useSystemProxies", "true")
         System.setProperty("http.nonProxyHosts", "*.adeo.no")
         callback = "${env.BUILD_URL}input/Deploy/"
         def deploy = deployLib.deployNaisApp(application, releaseVersion, environment, zone, namespace, callback, committer).key
         echo "Check status here:  https://jira.adeo.no/browse/${deploy}"
         try {
            timeout(time: 15, unit: 'MINUTES') {
               input id: 'deploy', message: "Check status here:  https://jira.adeo.no/browse/${deploy}"
            }
         } catch (Exception ex) {
            throw new Exception("Deploy feilet :( \n Se https://jira.adeo.no/browse/" + deploy + " for detaljer", ex)
         }
      }
   }

   stage("Tag") {
      // TODO: Tag only releases that go to production
      withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
         withCredentials([string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            sh ("git tag -a ${releaseVersion} -m ${releaseVersion}")
            sh ("git push https://${token}:x-oauth-basic@github.com/${repo}/${application}.git --tags")
         }
      }
   }
}

def notifyGithub(owner, repo, context, sha, state, description) {
   def postBody = [
      state: "${state}",
      context: "${context}",
      description: "${description}",
      target_url: "${env.BUILD_URL}"
   ]
   def postBodyString = groovy.json.JsonOutput.toJson(postBody)

   withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
      withCredentials([string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
         sh """
                curl -H 'Authorization: token ${token}' \
                    -H 'Content-Type: application/json' \
                    -X POST \
                    -d '${postBodyString}' \
                    'https://api.github.com/repos/${owner}/${repo}/statuses/${sha}'
            """
      }
   }
}

