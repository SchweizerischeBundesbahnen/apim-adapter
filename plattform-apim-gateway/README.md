# SBB Gateway

A Gateway which manages the API of an Application.
Implemented with the [Light Proxy](https://doc.networknt.com/service/proxy/) from the Light4J framework.

## See also
See also:
* [APIM - Architecture](https://confluence.sbb.ch/pages/viewpage.action?pageId=1157204897) 
* [APIM - Adapter](https://code.sbb.ch/projects/KD_APIM/repos/apim-adapter/browse)

## How it works

![HowItWorks](HowItWorks.jpg)

## Performance
- The Performance of one POD is about **3000 request per second** with a response time on an echo api of around 5ms
- The PODs scale linear so you can scale the PODs up to the required amount of Request per second
- But keep in mind, if the backend has a higher latency the performance of the gateway will be reduced
- also make sure you scale your backend, that it can handle the amount of request
- The low resources Template can handle around **300 request per second**

## Rate Limits - INACTIVE per default
- The Gateway has a limit of 40 concurrent request per Pod and there is also a queue for 40 request.
    - The custom error code 513 is thrown if the queue is full and the gateway can't handle more request
    - In this case it is recommended to scale up the gateway
- Http-Error 513 ->  [undertow.io](http://undertow.io/javadoc/1.3.x/io/undertow/server/handlers/RequestLimit.html)

## LOGLEVEL
- To set the log level set the environment variable **APIM_GATEWAY_LOG_LEVEL** to one of the following values:
    - DEBUG
    - INFO
    - WARN (default if the variable is not set)
    - ERROR

## Monitoring
The Gateway and Adapter are both monitored over [Prometheus](https://prometheus.io/). The monitoring is accessible over
the admin port (3000) and the endpoint **/metrics** . The data is also pushed to the centralized APIM monitoring, for us to help you in case of an error. 
The data which is pushed/exposed can be limited over the **MONITORING_LEVEL** env variable.

### Monitoring Level
we have four different levels
- **NONE**
    - NO DATA WILL BE MONITORED
- **MINIMAL**
    - Here we only collect the minimal amount of data to analyse
        - Status of the SYNC
        - Cache Size
        - Configuration of the Gateway
- **STANDARD**
    - everything from MINIMAL and the following data
        - Cache statistics
        - Thread information
        - GC 
        - Memory
        - Statistics of the Proxy
        - XNIO information
- **ALL**
    - everything from STANDARD and the following data
          - JMX 
          - Runtime / Version

### PUSH
We push the data to `APIM_MONITORING_PUSH_HOST` (env variable).
 
PROD AWS: http://pushgateway-monitoring.prod.app.ose.sbb-aws.net
	
PROD VIAS: http://pushgateway-monitoring.prod.app.ose.sbb-cloud.net

INT AWS:  http://pushgateway-monitoring.int.app.ose.sbb-aws.net

INT VIAS: http://pushgateway-monitoring.int.app.ose.sbb-cloud.net

### Own Monitoring

The project can monitor the application by itself over the **/metrics** endpoint. You need to have your own Prometheus instance running to collect and store the Data. 

## Light4J resources
* [Getting Started](https://doc.networknt.com/getting-started/light-proxy/) to learn core concepts
* [Tutorial](https://doc.networknt.com/tutorial/proxy/) with step by step guide for RESTful proxy
* [Configuration](https://doc.networknt.com/service/proxy/configuration/) for different configurations based on your situations
* [Artifact](https://doc.networknt.com/service/proxy/artifact/) to guide customer to choose the right artifact to deploy light-proxy.

## Emergency Mode
The environment variable CACHE_LOCATION can be used to activate Emergency Mode functionality. If this value is set, the adapter persists configuration files from the api-management backend within this path.

The path of should point to a persistent volume (PV). This PV has to be added to the deployment of the gateway. 

When the app is starting and the api-management backend is not available, the persisted configuration is reused. And the app is in Emergency Mode.

The adapter tries to sync its configuration with the api-management. If this is successful the app is leaving Emergency Mode.

## Run local
* maven clean install exec:exec
* with the following environment variables
    * APIM_ADMIN_HOST
        - 3scale-admin.dev.app.ose.sbb-aws.net
    * APIM_BACKEND_HOST
        - backend-3scale.dev.app.ose.sbb-aws.net
    * APIM_BACKEND_TOKEN
        - **replace**
    * APIM_ADAPTER_PRODUCTION_MODE
        - false
    * APIM_TOKENISSUER_1_URL_PATTERN
        - https://sso-dev.sbb.ch/auth/realms/(SBB_Public)
    * APIM_ADAPTER_SERVICE_ID
        - **replace**
    * APIM_ADMIN_TOKEN
        - **replace**
    * APIM_MONITORING_PUSH_HOST
        - http://pushgateway-monitoring.dev.app.ose.sbb-aws.net
    * APIM_MONITORING_NAMESPACE
        - local-project
    * APIM_MONITORING_ID
        - pod-name
    * APIM_MONITORING_LEVEL
        - ALL
    * APIM_CACHE_LOCATION
        - target/apim_cache

`APIM_CACHE_LOCATION` can also be omitted, then it falls back to default value `DISABLED` which deactivates offline configuration caching.

## Release
When releasing the project the following steps have to be done:
1) Update version in pom. If you want to release Version ```x.y.z``` set version to ```x.y.z-SNAPSHOT```
2) Update ```/openshift/templates``` to match version ```x.y.z```
3) Commit changes on ```develop``` Branch
4) Merge ```develop``` into ```master```. This will trigger a release by jenkins
5) Update Openshift templates in vias and aws
6) Optional: Delete old oc templates


