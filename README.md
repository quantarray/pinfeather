<!--
  Title: PinFeather
  Description: Display TeamCity build status via Raspberry Pi.
  Author: Araik Grigoryan
  Copyright: 2016 Quantarray, LLC
-->
  
<meta name='keywords' content='scala, akka, raspberry pi, team city, pinfeather'>

# PinFeather

Display TeamCity build status via Raspberry Pi using Scala and Akka.

Obtain TeamCity build status using [JetBrains's REST client](https://github.com/JetBrains/teamcity-rest-client).

Control Raspberry Pi using [Pi4J](http://pi4j.com/).

# Deployment

Run ```pinfeather-monitor-package-deploy.sh pi@mypi.local:/path/to/remote/directory``` to package a single JAR and deploy to your Raspberry Pi (using SCP).

# Configuration

## .httpAuth

Use ```.httpAuth.template``` file to copy and create ```.httpAuth``` file to supply parameters below

```
method=httpAuth
hostUrl=http://host:port // TeamCity host and port
userName=                // TeamCity user
password=                // TeamCity password 
```

As of the time of this writing, httpAuth is the only secure way to authenticate against a TeamCity server using JetBrains's REST client.

## xyz.application.conf

Use ```application.conf.template``` file to copy and create ```xyz.application.conf``` file (replace ```xyz``` with a proper application name) to supply 

```
pinFeather {

  teamCity {

    hostUrl: "http://host:port" // TeamCity host and port

    authFilePath: ".httpAuth"

    buildConfigIds: [
      "Project_SubProject1_Build", // Your sub-project 1
      "Project_SubProject2_Build"  // Your sub-project 2, etc.
    ]
  }

}
```

# Running on Raspberry Pi

```sudo ./pinfeather-monitor.sh xyz``` (replace ```xyz``` with a proper application name)

# Raspberry Pi Wiring

Depending on your version of Raspberry Pi, your pysical wiring will differ. 

I used the [Pi4J's pinout for Raspberry Pi 3 Model B](http://pi4j.com/pins/model-3b-rev1.html).
 
[![Connected, successful](/doc/PinFeatherConnectedSuccessful.JPG)]