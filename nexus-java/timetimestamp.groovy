/************
usage:
   cp /.sdkman/candidates/groovy/2.4.7/lib/groovy-2.4.7.jar .
   cp /.sdkman/candidates/groovy/2.4.7/lib/groovy-json-2.4.7.jar .
   vi timetimestamp.groovy
   /.sdkman/candidates/groovy/2.4.7/bin/groovyc timetimestamp.groovy
   java -cp .:./groovy-2.4.7.jar:./groovy-json-2.4.7.jar timetimestamp /etc/logstash-forwarder.conf
************/

import groovy.json.*
import java.sql.*
import java.text.*
/*
  println "--start of test 1--"
  def body2 = '{"url":"http://pzsvc-hello.int.geointservices.io","method":"GET","contractUrl":"http://pzsvc-hello.int.geointservices.io/","resourceMetadata":{"name":"Hello World Test","description":"Hello world test","classType":{"classification":"UNCLASSIFIED"}}}'

  def myprocess2 = [ 'bash', '-c', "curl -v -k -X GET http://52.37.229.115:9200/logstash-2016.10.08" ].execute()
  myprocess2.waitFor()
  String myprocess2AsText =  myprocess2.text
  println myprocess2AsText

  def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)

  println result2AsJson
  println "--end of test 1--"
*/

  println "--start of test 2--"
  def body2 = '{"size":4,"query":{"match_all":{}}}'

  def myprocess2 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body2}' http://52.37.229.115:9200/logstash-2016.10.08/_search" ].execute()
  myprocess2.waitFor()
  String myprocess2AsText =  myprocess2.text
  println myprocess2AsText

  def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)

  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  def xtimestamp = result2AsJson.hits.hits[0]._source."@timestamp"
  def xtimestamp2 = xtimestamp[0..18]+"Z"
  def xtimestamp3 = sdf.parse(xtimestamp2)
  println xtimestamp3
  def xtime = result2AsJson.hits.hits[0]._source.time
  def xtime2 = xtime[0..18]+"Z"
  def xtime3 = sdf.parse(xtime2)
  println xtime3
  use(groovy.time.TimeCategory) {
    def duration = xtimestamp3 - xtime3
    println "Hours: ${duration.hours}"
    println "Minutes: ${duration.minutes}"
    println "Seconds: ${duration.seconds}"
  }

  println "--end of test 2--"

