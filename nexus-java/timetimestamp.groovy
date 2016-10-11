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
  //println xtimestamp3
  def xtime = time //result2AsJson.hits.hits[0]._source.time
  def xtime2 = xtime[0..18]+"Z"
  def xtime3 = sdf.parse(xtime2)
  //println xtime3
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
curl http://52.37.229.115:9200#/logstash-2016.10.08/_search?from=23 -d '{"size":2,"query":{"match_all":{}}}'
*/

  println "--start of test 2--"
  def SIZE = 10
  def body2 = '{"query":{"match_all":{}}}'

  def fetchMoreData = true
  for (def from = 0; fetchMoreData; from += SIZE) {
   String url = "http://52.37.229.115:9200/logstash-2016.10.08/_search?from=$from\\&size=$SIZE"
   println "url $url"
   def myprocess2 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body2}' $url"].execute()
   myprocess2.waitFor()
   String myprocess2AsText = myprocess2.text
   //println "xxx\n$myprocess2AsText\nyyy"
   def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)
   def id = result2AsJson.hits.hits._id
   def attimestamp = result2AsJson.hits.hits._source."@timestamp"
   def time = result2AsJson.hits.hits._source.time

   for (def i = 0; i < id.size(); i++) {
    def t = timetimestamp(attimestamp[i], time[i])
    println "${id[i]} $t"
    //def body = "{\"id\":\"${id[i]}\", \"time\":${time[i][0..18]+"Z"}, \"seconds\": $t}"
    def body = "{\"time\":${time[i][0..18]+"Z"}, \"seconds\": $t}"
    isrt(id[i], time[i][0..18]+"Z", t)
   }
   fetchMoreData = (id.size() == SIZE)
  }
  println "--end of test 2--"
/*
 curl -XPUT http://52.37.229.115:9200/seconds-2016.10.08/mytype/AVelsxTHYmEacSoOqezI -d '{"time":"2016-10-08T19:08:58Z", "seconds": 20}'
*/
 def isrt(id, timexx, secs) {
   println id
   String url2 = "http://52.37.229.115:9200/seconds-2016.10.08/mytype/$id "
   println "url2 $url2"

   //def myprocessa = [ 'bash', '-c', "curl -v -k -X POST  -d '{\"seconds\":44, \"lime\":33, \"motali\":\"2016-10-08T19:08:58Z\"}' $url2"]
   def myprocessa = [ 'bash', '-c', "curl -v -k -X POST  -d '{\"seconds\":$secs, \"lime\":33, \"motali\":\"$timexx\"}' $url2"]
   def myprocess3 = myprocessa.execute()
   myprocess3.waitFor()
   String myprocess3AsText = myprocess3.text
   println myprocess3AsText
  }
 
