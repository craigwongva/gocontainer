sudo service tomcat7 stop

sudo rm -rf /usr/share/tomcat7/webapps/green
sudo rm /usr/share/tomcat7/webapps/green.war

vi

mvn -DskipTests install
sudo cp target/green-1.0-SNAPSHOT.war /usr/share/tomcat7/webapps/green.war

sudo service tomcat7 start

sudo vi /var/log/tomcat7/catalina.out
