# DaDaPush plugin for Jenkins

Based on Mattermost plugin:

https://github.com/jenkinsci/mattermost-plugin/


Includes [Jenkins Pipeline](https://github.com/jenkinsci/workflow-plugin) support as of version 2.0:

```
dadapushSend title:"test title", content:"test content"

dadapushSend title:"test title", content:"test content", failOnError:true, channelToken:"ctXXXXXX", basePath:"https://www.dadapush.com"
```

# Jenkins Instructions

1. Set up a DaDaPush server
2. Configure an incoming webhook
3. Install this plugin on your Jenkins server
4. **Add it as a Post-build action** in your Jenkins job.

# Developer instructions

Install Maven and JDK.

Run unit tests

    mvn test

Run findbugs:

    mvn findbugs:check

Create an HPI file to install in Jenkins (HPI file will be in `target/dadapush.hpi`).

    mvn package
