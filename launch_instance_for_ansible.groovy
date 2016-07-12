import groovy.json.JsonSlurper

/////////////
// usage:
//   vi modsecgrp.groovy
//   ~/.sdkman/candidates/groovy/2.4.6/bin/groovyc modsecgrp.groovy
//   java -cp .:./groovy-2.4.7.jar:./groovy-json-2.4.7.jar modsecgrp
/////////////

def writeAnsibleInventoryFile(confFile, ip) {
 def f = new File(confFile)
 f.write("[greekserver]\n$ip\n")
}
def createTargetEC2Instance() {

 def awscmd2 = 'aws cloudformation create-stack --stack-name ansible3 --template-url https://s3.amazonaws.com/venicegeo-devops-dev-gocontainer-project/cf-ansible.json --region us-west-2'
 //s/m: here add logic to see error messages, not just .text
 def awscmdtext2 = awscmd2.execute().text
 def slurper2 = new JsonSlurper()
 def result2 = slurper2.parseText(awscmdtext2)
 println result2.StackId

 def justTheLaunchedStack
 def stackStatus = ''
 for (int i=0; i<24; i++) {
  if (stackStatus != 'CREATE_COMPLETE') {
   def awscmd = 'aws cloudformation list-stacks --region us-west-2'
   def awscmdtext = awscmd.execute().text
   def slurper = new JsonSlurper()
   def result = slurper.parseText(awscmdtext)
   justTheLaunchedStack = result.StackSummaries.grep{ it.StackId == result2.StackId }
   stackStatus = justTheLaunchedStack.StackStatus[0]
   println "$i $stackStatus"
   sleep(5000)
  }
 }
 if (stackStatus != 'CREATE_COMPLETE') {
  println "Stack not created within two minutes."
  System.exit(1)
 }
 return result2.StackId
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
   (it.Key == 'aws:cloudformation:stack-id') && (it.Value == keyvalue)
  }
  if (found.size() > 0) {
   returnResult.privateIpAddress = i[0].PrivateIpAddress
   returnResult.publicIpAddress =  i[0].PublicIpAddress
  }
 }
 println "getInstanceIPAddresses($keyvalue) ==> $returnResult"
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

def StackId = createTargetEC2Instance()
println "StackId: $StackId"
def publicIpAddress = getInstanceIPAddresses(StackId).publicIpAddress
println "publicIpAddress: $publicIpAddress"

def tracSecurityGroupID = 'sg-1a31957c'
authorizeSecurityGroupIngress(
 'allow trac to receive from Ansible target server on 3690',
 tracSecurityGroupID,
 publicIpAddress,
 '3690')

writeAnsibleInventoryFile('inventory', publicIpAddress)

for (int i=0; i<24; i++) {
 println "$i Waiting two minutes for ssh to become available."
 sleep(5000)
}

