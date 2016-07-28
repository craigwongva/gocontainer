package rudm

import groovy.json.*

class AppleController {

 def PIAZZA_PRIME_BOX = '52.43.11.233'

 def index() { }

 def ping() {

  println "--start of test 1--"
  def test1 = [
   "curl", "-X", "GET", 
   "-d 'Das Hotel Marriott'", 
   "http://$PIAZZA_PRIME_BOX:8080"
  ].execute().text
  println test1
  println "--end of test 1--"


  println "--start of test 2--"
  def body2 = '{"url":"http://pzsvc-hello.int.geointservices.io","method":"GET","contractUrl":"http://pzsvc-hello.int.geointservices.io/","resourceMetadata":{"name":"Hello World Test","description":"Hello world test","classType":{"classification":"UNCLASSIFIED"}}}'

  def myprocess2 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body2}' http://$PIAZZA_PRIME_BOX:8081/service" ].execute()
  myprocess2.waitFor()
  String myprocess2AsText =  myprocess2.text
  println myprocess2AsText

  def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)

  println result2AsJson
  println "--end of test 2--"


  println "--start of test 3--"
  def body3 = '{"type":"execute-service","data":{"serviceId":"REPLACEME","dataInputs":{},"dataOutput":[{"mimeType":"application/json","type":"text"}]}}'
  body3 = body3.replaceAll('REPLACEME', result2AsJson.data.serviceId)

  def myprocess3 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body3}' http://$PIAZZA_PRIME_BOX:8081/job" ].execute()
  String myprocess3AsText =  myprocess3.text
  println myprocess3AsText

  def result3AsJson = new JsonSlurper().parseText(myprocess3AsText)

  println result3AsJson
  println "--end of test 3--"

  sleep(5000)

  println "--start of test 4--"
  def myprocess4 = [ 'bash', '-c', "curl -v -k -X GET -H \"Content-Type: application/json\" http://$PIAZZA_PRIME_BOX:8081/job/${result3AsJson.data.jobId}" ].execute()
  String myprocess4AsText =  myprocess4.text
  println myprocess4AsText
  def result4AsJson = new JsonSlurper().parseText(myprocess4AsText)

  println result4AsJson
  println "--end of test 4--"

  println "--start of test 5--"
  def myprocess5 = [ 'bash', '-c', "curl -v -k -X GET -H \"Content-Type: application/json\" http://$PIAZZA_PRIME_BOX:8081/data/${result4AsJson.data.result.dataId}" ].execute()
  String myprocess5AsText = myprocess5.text
  println myprocess5AsText
  println "--end of test 5--"
//GOODBYE
  println "i'm exiting"
 }

}
