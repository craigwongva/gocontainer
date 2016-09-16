import groovy.json.JsonSlurper

/************
usage:
   vi modsecgrp.groovy
   ~/.sdkman/candidates/groovy/2.4.6/bin/groovyc modsecgrp.groovy
   java -cp .:./groovy-2.4.7.jar:./groovy-json-2.4.7.jar modsecgrp
************/

def getSecurityGroupID(keyvalue) {
 def awscmd = 'aws ec2 describe-security-groups --region us-west-2'
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

def getInstanceIPAddresses(keyvalue) {
 def awscmd = 'aws ec2 describe-instances --region us-west-2' +
              ' --filters Name=instance-state-name,Values=running'
 def awscmdtext = awscmd.execute().text
 def returnResult = [:]

 def slurper = new JsonSlurper()
 def result = slurper.parseText(awscmdtext)
 result.Reservations.Instances.each { i ->
  def found = i.Tags[0].grep {
   (it.Key == 'Name') && (it.Value == keyvalue)
  }  
  if (found.size() > 0) {
   returnResult.privateIpAddress = i[0].PrivateIpAddress
   returnResult.publicIpAddress =  i[0].PublicIpAddress
  }
 }
 returnResult
}

def authorizeSecurityGroupIngress(comment, groupID, cidr, port) {
 def proc
 def sb

 def cmd = 
       "aws ec2 authorize-security-group-ingress " +
       " --group-id $groupID" +
       " --cidr $cidr/32" +
       " --port $port " +
       " --protocol tcp " +
       " --region us-west-2 " 
 proc = cmd.execute()
 sb = new StringBuffer()
 proc.consumeProcessErrorStream(sb)
 println "comment: \n$comment"
 println "generated AWS CLI command: \n$cmd"
 println "stdout: ${proc.text}"
 println "stderr: ${sb.toString()}"
 println ""
}

/**
* Update AWS security groups
**/

def worker1IPs                      = getInstanceIPAddresses('worker1')
def worker1SecurityGroupID          = getSecurityGroupID('worker1-SecurityGroup')

def consulIPs                        = getInstanceIPAddresses('consul')
def consulSecurityGroupID            = getSecurityGroupID('consul-SecurityGroup')

if (args[0] == 'option1') {
 authorizeSecurityGroupIngress(
  'allow worker1 to receive from consul on 2375', 
  worker1SecurityGroupID, 
  consulIPs.publicIpAddress, 
  '2375')

 authorizeSecurityGroupIngress(
  'allow consul to receive from worker1 on 2375', 
  consulSecurityGroupID, 
  worker1IPs.publicIpAddress, 
  '2375')

 authorizeSecurityGroupIngress(
  'allow worker1 to receive from consul on 8500', 
  worker1SecurityGroupID, 
  consulIPs.publicIpAddress, 
  '8500')

 authorizeSecurityGroupIngress(
  'allow consul to receive from worker1 on 8500', 
  consulSecurityGroupID, 
  worker1IPs.publicIpAddress, 
  '8500')
}

if (args[0] == 'option2') {
 println consulIPs.publicIpAddress
}
