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
