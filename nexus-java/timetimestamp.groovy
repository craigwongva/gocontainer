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

def timetimestamp(attimestamp, time) {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  def xtimestamp = attimestamp //result2AsJson.hits.hits[0]._source."@timestamp"
  def xtimestamp2 = xtimestamp[0..18]+"Z"
  def xtimestamp3 = sdf.parse(xtimestamp2)
  println xtimestamp3
  def xtime = time //result2AsJson.hits.hits[0]._source.time
  def xtime2 = xtime[0..18]+"Z"
  def xtime3 = sdf.parse(xtime2)
  println xtime3
  def totalSeconds
  use(groovy.time.TimeCategory) {
    def duration = xtimestamp3 - xtime3
    //println "Hours: ${duration.hours}"
    //println "Minutes: ${duration.minutes}"
    //println "Seconds: ${duration.seconds}"
    totalSeconds = duration.minutes*60 + duration.seconds
  }
  totalSeconds
}
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
  def id = result2AsJson.hits.hits._id
  def attimestamp = result2AsJson.hits.hits._source."@timestamp"
  def time = result2AsJson.hits.hits._source.time
  //println "_id is $id"
  //println "attimestamp is $attimestamp"
  //println "time is $time"
//  def fetchMoreData = true
//  for (def from = 0; fetchMoreData; from += SIZE) {
  for (def i = 0; i < id.size(); i++) {
   def t = timetimestamp(result2AsJson.hits.hits[i]._source."@timestamp", result2AsJson.hits.hits[i]._source.time) 
   println t
  }
  println "--end of test 2--"
/*
 def SIZE = 2
 def CSV = 'c:/grails-3.0.9/test1/elasticsearch.txt' 
   
 def csvWriter = new File(CSV).newWriter() 
 csvWriter << 'animal, animalReversed, animalLength\n'
 
 def fetchMoreData = true
 for (def from = 0; fetchMoreData; from += SIZE) {
  def resultAsText = [
   CURL, "-XGET", 
   "$ES/$ES_IX/_search?from=$from&size=$SIZE",
  ].execute().text

  def resultAsJson = new JsonSlurper().parseText(resultAsText)

  def animals = resultAsJson.hits.hits._source.title

  def transformations = 
   animals.collect {[
     animal:it, 
     animalReversed:it.reverse(), 
     animalLength:it.length()
   ]}
   
  transformations.each {
   println "${it.animal},${it.animalReversed},${it.animalLength}"
   csvWriter <<
    "${it.animal},${it.animalReversed},${it.animalLength}\n"
  } 
  fetchMoreData = (transformations.size() == SIZE)
 }
*/
