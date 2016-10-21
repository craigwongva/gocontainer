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
import groovy.time.*

 def computeAverage(ES2, yyyydotmmdotdd) {
  def body4 = "{\"size\": 0, \"aggs\": { \"avg_grade\": { \"avg\": { \"field\": \"seconds\"}}}}"

  String url4 = "http://$ES2:9200/seconds-$yyyydotmmdotdd/_search"
  //println "url $url4"
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
 def readThenWrite(ES2, yyyydotmmdotdd) { //ixname logstash-2016.10.08
  def HEADER = "\"xtime\", \"xtimestamp\", \"relative\", \"xseconds\""
  def SIZE = 60
  def body2 = '{"query":{"match_all":{}},"sort":[{"time":{"order":"asc"}}]}'

  println HEADER
  def fetchMoreData = true
  for (def from = 0; fetchMoreData; from += SIZE) {
   String url = "http://$ES2:9200/logstash-$yyyydotmmdotdd/_search?from=$from\\&size=$SIZE"
   def myprocess2 = [ 'bash', '-c', "curl -v -k -X POST -H \"Content-Type: application/json\" -d '${body2}' $url"].execute()
   myprocess2.waitFor()
   String myprocess2AsText = myprocess2.text

   def result2AsJson = new JsonSlurper().parseText(myprocess2AsText)
   def id = result2AsJson.hits.hits._id
   def attimestamp = result2AsJson.hits.hits._source."@timestamp"
   def time = result2AsJson.hits.hits._source.time

   for (def i = 0; i < id.size(); i++) {
    def t = timetimestamp(attimestamp[i], time[i])
    def firstTime = "2016-10-20T23:58:10.658192516Z" //s/m: automate this
    def relative = timetimestamp(time[i], firstTime)
    println "\"${time[i]}\", \"${attimestamp[i]}\", $relative, $t"
    //println "${id[i]} $t"
    def body = "{\"time\":${time[i][0..18]+"Z"}, \"seconds\": $t}"
    isrt(ES2, yyyydotmmdotdd, id[i], time[i][0..18]+"Z", t)
   }
   fetchMoreData = (id.size() == SIZE)
  }
 }

 def isrt(ES2, yyyydotmmdotdd, id, timexx, secs) {
   String url2 = "http://$ES2:9200/seconds-$yyyydotmmdotdd/mytype/$id "
/*
   def myprocessa = [ 'bash', '-c', "curl -v -k -X POST  -d '{\"seconds\":$secs, \"lime\":33, \"motali\":\"$timexx\"}' $url2"]
   def myprocess3 = myprocessa.execute()
   myprocess3.waitFor()
   String myprocess3AsText = myprocess3.text
   //println myprocess3AsText
*/
  }

 def timetimestamp(attimestamp, time) {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
  def xtimestamp = attimestamp 
  def xtimestamp2 = xtimestamp[0..18]+"Z"
  def xtimestamp3 = sdf.parse(xtimestamp2)
  def xtimestamp4 = xtimestamp[0..22]+"+0000"

  def xtime = time 
  def xtime2 = xtime[0..18]+"Z"
  def xtime3 = sdf.parse(xtime2)
  def xtime4 = xtime[0..22]+"+0000"

  def start = Date.parse("yyy-MM-dd'T'HH:mm:ss.SSSZ",xtime4)
  def end   = Date.parse("yyy-MM-dd'T'HH:mm:ss.SSSZ",xtimestamp4)
  TimeDuration durationx = TimeCategory.minus(end, start)
  60*1000*durationx.minutes+1000*durationx.seconds+durationx.millis
 }
 def ES = '52.37.169.141'
 readThenWrite(ES, "2016.10.20")
 //println computeAverage("52.37.169.141", "2016.10.13") 
