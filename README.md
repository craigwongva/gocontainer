## How to run
`aws cloudformation create-stack --stack-name gocontainer --template-body file://cf-nexus-java.yaml --region us-west-2`--parameter ParameterKey=IP8080,ParameterValue=REDACTED ParameterKey=IP22,ParameterValue=REDACTED

Wait 12 minutes for everything to install, then 4 more minutes before green dots are enabled (I'm not sure why this delay exists).

From ec2w, `http://x.y.z.w:8080/green/timer/dots`

## About Piazza service builds ##
This blurb describes how Piazza services are built. (Someday there will be a separate description for how backing services are built.)

On a single EC2 instance (created via a Cloud Formation script), for each Piazza service:
* build it via maven,
* upload its Java .war to Nexus (this is not yet implemented for Go),
* download its .war from Nexus,
* build its Docker image,
* upload its Docker image to Dockerhub,
* download its Docker image from Dockerhub,
* create Docker container

## About dynamic ports ##
This blurb discusses two Go modules, pz-logger and pz-uuidgen. The blurb mentions pz-logger specifically, but the same logic applies to pz-uuidgen.

Like all Piazza Prime application services, pz-logger runs in its own container. As a Go module, pz-logger runs on a dynamic port. When its container starts pz-logger, pz-logger writes one line to stdout describing the port. That one stdout line is captured in a container file (/tmp/pz-logger-port-number-line), because the container's Dockerfile (Dockerfile-nexus-logger) CMD instruction specifically mandates that capture.

The script (userdata-nexus-logger) that invoked the Dockerfile subsequently performs a "docker copy" to copy the container's /tmp/pz-logger-port-number-line file up out of the container into the host. The script then parses the file's one line message and extracts the container's pz-logger port number and writes it to local file pz-logger-port-number. See the Appendix A below for the specific code.

The pz-logger-port-number file is used by all subsequent container launches. Therefore all subsequent containers know which port to use to access pz-logger. For example, userdata-nexus-gateway contains an "echo" statement that appends a CMD statement with the pz-logger port number into the pz-gateway Dockerfile (Dockerfile-nexus-gateway). See Appendix B below for the specific echo code.


### Appendix A ###
Here is the specific code from userdata-nexus-logger that extracts the pz-logger port number:
```
sudo docker cp `sudo docker ps -a | grep logger | cut -c1-6`:/tmp/pz-logger-port-number-line pz-logger-port-number-line

#Extract just the process number between : and )
cat pz-logger-port-number-line | cut -d':' -f3 | cut -d')' -f1 > pz-logger-port-number

##Sometimes there is a line in the pz-logger output that isn't
## related to the process number. After the cuts, that line is
## now empty, so remove it.
sed -i -e '/^$/d' pz-logger-port-number
```

### Appendix B ###
Here is the specific code from userdata-nexus-gateway that uses the extracted pz-logger port number:
```
sed -i -e '/CMD/d' Dockerfile-nexus-gateway
echo CMD [\"/usr/lib/jvm/java-1.8.0-openjdk/bin/java\", \"-Duuid.protocol=http\", \"-Duuid.port=`cat pz-uuidgen-port-number`\", \"-Dlogger.protocol=http\", \"-Dlogger.port=`cat pz-logger-port-number`\", \"-jar\", \"piazza-gateway-0.1.0-NEXUSSED.jar\"] >> Dockerfile-nexus-gateway
```

## About networking ##

pz-logger is not accessible externally even if the AWS firewall for pz-logger's port is open.

Therefore the networking strategy from the master branch won't work for pz-logger and pz-logger's consumers (i.e. all other Piazza Prime application services).

Therefore the go branch uses `--net="host"` networking:
* All containers are run with the `--net="host"` option. Therefore each container uses the host's /etc/hosts definitions. The most important definition is the localhost definition. Each container finds each other container on localhost, e.g. localhost:8081 for pz-gateway, localhost:8083 for pz-jobmanager, ..., localhost:8088 for pz-servicecontroller.
* At the customer meeting we demoed the use of external backing services, i.e. backing services like Mongo which lived on an EC2 instance separate from the Piazza Prime application services. In the master branch, this was accomplished automatically via `--add-host` parameters in the `docker build` command (e.g. `--add-host="jobdb.dev:$1"` in userdata-nexus-manager in the master branch). In the go branch, external backing service locations must be defined in the host /etc/hosts like this: `52.43.236.86 jobdb.dev"`. (This isn't automated yet.)

Here are the new service registration pointers using "docker run --net" networking:
* gateway:           --net="host" 
* jobmanager:        --net="host" 
* ingest:            --net="host" 
* access:            --net="host" 
* servicecontroller: --net="host" 
* logger:            --net="host" 
* uuidgen:           --net="host" 

For historical reference, here are the old service registration pointers using "docker run --add-host" networking:
* gateway:          --add-host="pz-jobmanager.localdomain:$1" --add-host="pz-servicecontroller.localdomain:$1" --add-host="pz-access.localdomain:$1"
* jobmanager:       --add-host="jobdb.dev:$1" --add-host="kafka.dev:$1"
* ingest:           --add-host="jobdb.dev:$1" --add-host="kafka.dev:$1" 
* access:           --add-host="jobdb.dev:$1" --add-host="kafka.dev:$1"
* servicecontroller:--add-host="jobdb.dev:$1" --add-host="kafka.dev:$1"

## About overridden environment variables ##
Here are the overridden environment variables for all the Java component Dockerfiles' CMD lines:

```"-Duuid.protocol=http", "-Duuid.port=REPLACEME1", "-Dlogger.protocol=http", "-Dlogger.port=REPLACEME2"```

That's for gateway, jobmanager, ingest, access, servicecontroller.


Note that at Docker container creation time:
* the text REPLACEME1 is dynamically replaced with the actual dynamic pz-uuidgen port.
* the text REPLACEME2 is dynamically replaced with the actual dynamic pz-logger port.

Here are the overridden environment variables for the Go component Dockerfiles' CMD lines:
* logger:  none (someday soon: port forwarding for :9200 will be implemented)
* uuidgen: none

## Using external backing services from Piazza Java services ##
If your backing services are on the same host as your Piazza Prime containerized services, then you don't need to change your host /etc/hosts.
However if your external backing service locations are external (e.g. 52.43.236.86), then you must change your host /etc/hosts like this: 
```
52.43.236.86 jobdb.dev
52.43.236.86 kafka.dev
52.43.236.86 pz-jobmanager.localdomain
52.43.236.86 pz-access.localdomain
52.43.236.86 pz-servicecontroller.localdomain
```

## Using Consul with Go but not Grails  ##
Add ?consul=52.53.54.55 and code will query consul :8500 to find the
external user services participating through Swarm. 
The readiness/up status of the external user services is not actually 
tracked by Consul, so we are actually taking a bit of a shortcut now.

find-eus.go makes this call:
```
52.39.26.15:8500/v1/kv/docker/swarm/nodes?keys
```
and might get this back:
```
["docker/swarm/nodes/52.88.204.91:2375",
 "docker/swarm/nodes/54.68.69.232:2375",
 "docker/swarm/nodes"]

