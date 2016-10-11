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

 def computeAverage() {
  def body4 = "{\"size\": 0, \"aggs\": { \"avg_grade\": { \"avg\": { \"field\": \"seconds\"}}}}"

  String url4 = "http://52.37.229.115:9200/seconds-2016.10.08/_search"
  println "url $url4"
  def myprocess4 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body4}' $url4"].execute()
  myprocess4.waitFor()
  String myprocess4AsText = myprocess4.text

  def result4AsJson = new JsonSlurper().parseText(myprocess4AsText)
  result4AsJson.aggregations.avg_grade.value
 }

/*
 Read from logstash-2016.10.08 and
 write to seconds-2016.10.08
*/
 def readThenWrite(logstashname) { //ixname logstash-2016.10.08
  def SIZE = 10
  def body2 = '{"query":{"match_all":{}}}'

  def fetchMoreData = true
  for (def from = 0; fetchMoreData; from += SIZE) {
   String url = "http://52.37.229.115:9200/$logstashname/_search?from=$from\\&size=$SIZE"
   println "url $url"
   def myprocess2 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body2}' $url"].execute()
   myprocess2.waitFor()
   String myprocess2AsText = myprocess2.text

   def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)
   def id = result2AsJson.hits.hits._id
   def attimestamp = result2AsJson.hits.hits._source."@timestamp"
   def time = result2AsJson.hits.hits._source.time

   for (def i = 0; i < id.size(); i++) {
    def t = timetimestamp(attimestamp[i], time[i])
    println "${id[i]} $t"
    def body = "{\"time\":${time[i][0..18]+"Z"}, \"seconds\": $t}"
    isrt(id[i], time[i][0..18]+"Z", t)
   }
   fetchMoreData = (id.size() == SIZE)
  }
 }

 def isrt(id, timexx, secs) {
   println id
   String url2 = "http://52.37.229.115:9200/seconds-2016.10.08/mytype/$id "
   println "url2 $url2"

   def myprocessa = [ 'bash', '-c', "curl -v -k -X POST  -d '{\"seconds\":$secs, \"lime\":33, \"motali\":\"$timexx\"}' $url2"]
   def myprocess3 = myprocessa.execute()
   myprocess3.waitFor()
   String myprocess3AsText = myprocess3.text
   println myprocess3AsText
  }

 def timetimestamp(attimestamp, time) {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  def xtimestamp = attimestamp 
  def xtimestamp2 = xtimestamp[0..18]+"Z"
  def xtimestamp3 = sdf.parse(xtimestamp2)

  def xtime = time 
  def xtime2 = xtime[0..18]+"Z"
  def xtime3 = sdf.parse(xtime2)

  def totalSeconds
  use(groovy.time.TimeCategory) {
    def duration = xtimestamp3 - xtime3
    totalSeconds = duration.minutes*60 + duration.seconds
  }
  totalSeconds
 }

 readThenWrite("logstash-2016.10.08")
 println computeAverage() 
