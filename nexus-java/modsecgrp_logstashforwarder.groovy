import groovy.json.JsonSlurper

/************
usage:
   vi modsecgrp.groovy
   ~/.sdkman/candidates/groovy/2.4.6/bin/groovyc modsecgrp.groovy
   java -cp .:./groovy-2.4.7.jar:./groovy-json-2.4.7.jar modsecgrp
************/

//def getSecurityGroupID(keyvalue) {
def getSecurityGroupID(keyvalue, region) {
 def awscmd = 'aws ec2 describe-security-groups --region ' + region //us-west-2'
 def awscmdtext = awscmd.execute().text
 def slurper = new JsonSlurper()
 def result = slurper.parseText(awscmdtext)
 def keynames = result.SecurityGroups.GroupName
 def keyclosure = { it =~ keyvalue }
 def count = keynames.grep(keyclosure).size()
 if (count == 0) {
  println "GroupName '$keyvalue' not found. exiting with rc=-1"
  System.exit(-1)
 }
 if (count > 1) {
  println "GroupName '$keyvalue' too common. exiting with rc=-1"
  println keynames.grep(keyclosure)
  System.exit(-1)
 }
 def ixkeyname = keynames.findIndexOf(keyclosure)
 result.SecurityGroups.GroupId[ixkeyname]
}

def getInstanceIPAddresses(keyvalue, region) {
 def awscmd = 'aws ec2 describe-instances --region ' + region + //'us-west-2' +
              ' --filters Name=instance-state-name,Values=running'
 def awscmdtext = awscmd.execute().text
 def returnResult = [:]

 def slurper = new JsonSlurper()
 def result = slurper.parseText(awscmdtext)
 result.Reservations.Instances.each { i ->
  def found = i.Tags[0].grep {
//   (it.Key == 'Name') && (it.Value == keyvalue)
//   ((region == 'us-west-2') && (it.Key == 'craig-go-component') && (it.Value == keyvalue)) ||
   ((region == 'us-west-2') && (it.Key == 'Name')               && (it.Value == keyvalue))
  }  
  if (found.size() > 0) {
   returnResult.privateIpAddress = i[0].PrivateIpAddress
   returnResult.publicIpAddress =  i[0].PublicIpAddress
  }
 }
 returnResult
}

//def authorizeSecurityGroupIngress(comment, groupID, cidr, port) {
def authorizeSecurityGroupIngress(comment, groupID, cidr, port, region) {
 def proc
 def sb

 def cmd = 
       "aws ec2 authorize-security-group-ingress " +
       " --group-id $groupID" +
       " --cidr $cidr/32" +
       " --port $port " +
       " --protocol tcp " +
//       " --region us-west-2 " 
       " --region $region " 
 proc = cmd.execute()
 sb = new StringBuffer()
 proc.consumeProcessErrorStream(sb)
 println "comment: \n$comment"
 println "generated AWS CLI command: \n$cmd"
 println "stdout: ${proc.text}"
 println "stderr: ${sb.toString()}"
 println ""
}

def updateLogstashForwarderConfFileLocalhost(confFile, ip) {
 def f = new File(confFile)
 f.write(f.text.replaceAll('localhost', ip))
}

/**
* Update AWS security groups
**/

def logstashForwarderIPs             = getInstanceIPAddresses('craigLF','us-west-2')
def logstashIPs                      = getInstanceIPAddresses('craigLg','us-west-2')
def elasticsearchIPs                 = getInstanceIPAddresses('craigES','us-west-2')

def logstashForwarderSecurityGroupID = getSecurityGroupID('craigLF-SecurityGroup', 'us-west-2')
def logstashSecurityGroupID          = getSecurityGroupID('SecurityGroupLg','us-west-2')
def elasticsearchSecurityGroupID     = getSecurityGroupID('SecurityGroupES','us-west-2')
def consulSecurityGroupID            = getSecurityGroupID('cwconsul-SecurityGroup', 'us-west-2')

println "87: logstashForwarderIPs=$logstashForwarderIPs"
println "92: logstashIPs=$logstashIPs"
println "95: ES IPs=$elasticsearchIPs"

authorizeSecurityGroupIngress(
 'allow LF to receive from Lg on 5043', 
 logstashForwarderSecurityGroupID, 
 logstashIPs.privateIpAddress, 
 '5043', 'us-west-2')

authorizeSecurityGroupIngress(
 'allow Lg to receive from LF on 5043', 
 logstashSecurityGroupID, 
 logstashForwarderIPs.privateIpAddress, 
 '5043', 'us-west-2')

authorizeSecurityGroupIngress(
 'ES: 9200 LF',
 elasticsearchSecurityGroupID,
 logstashForwarderIPs.publicIpAddress,
 '9200', 'us-west-2')

authorizeSecurityGroupIngress(
 'consul: 8500 LF',
 consulSecurityGroupID,
 logstashForwarderIPs.publicIpAddress,
 '8500', 'us-west-2')

authorizeSecurityGroupIngress(
 'LF: 8077 LF',
 logstashForwarderSecurityGroupID,
 logstashForwarderIPs.publicIpAddress,
 '8077', 'us-west-2')

/**
* Update /etc/logstash-forwarder.conf
**/

def confFile = args[0]
assert confFile == '/etc/logstash-forwarder.conf'
updateLogstashForwarderConfFileLocalhost(confFile, logstashIPs.privateIpAddress)
