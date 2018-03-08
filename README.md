riot-core-services
=============


Prerequisites
--------------
1. java 1.8
2. gradle 1.12 <- 2.x is known not to work
3. tomcat 7
4. mysql 5
5. cassandra 2.1.2
6. mosquitto 3.1
7. riot-core-commons.jar (from riot-core-bridges)

Get the Source code
-------------------

git clone https://github.com/tierconnect/riot-core-services.git

StartUp
-------
1. start up mysql and cassandra


Build
-----
1. copy gradle-default.properties to gradle-local.properties, if needed. 
   It is probably best to not do this, and just use the defaults
2. edit the file, and make sure things looks correct, especially database username and password
3. run 'gradle clean assemble cassandraInit popdbmojixretail'
4. if you want to run the scripts, also do 'gradle fatJar'

If you want to commit a specific configuration, then copy
gradle-default.properties to gradle-NEWNAME.properties.

Then you should be able to build with:

  gradle clean assemble install cassandraInit popdb -PenvironmentName=NEWNAME


Deploy the application
----------------------
1. copy build/libs/riot-core-services.war to your tomcat webaspp's dir.


------------------------------------------------
If you want thing times series data, then:


Start the Mqtt-Cassandra-Bridge
-------------------------------------------------
You have serveral options:
1. run 'gradle mcb'                <= useful for developers if you did not build fatJar
2. run (as root) './mcb.sh run'    <= this starts the process in the foreground
3. run (as root) './mcb.sh start'  <= this starts the process as a daemon

NOTE: the bridge is a daemon process. The gradle task will never exit.
The bridge reads from the mosquitto mqtt broker, writes to the cassandra database, but also reads from the mysql database,
and calls a few REST endpoints from the tomcat application war. 

Generate some data 
------------------
1. 'gradle dgmojixretail' or './dgmojixretail.sh'

NOTE: this currently runs for 60 seconds then exits
The script offers some command line options, see './dgmojixretail.sh -h' for details.



How to check the swagger ui:
----------------------------
1. browse to "http://localhost:8080/riot-core-services/swagger-ui"


How to Check that data is flowing into Cassandra:
---------------------------------------------
1. cqlsh
2. select * from system.schema_keyspaces;
3. use riot_main;
4. select * from field_value_history;

How to Run and test the ALE Bridge
-----------------------------------------------

1. Generate the fatJar
2. java -DMCOM=69.163.51.20 -cp build/libs/riot-core-services-all-XXXXX.jar com/tierconnect/riot/iot/ale_bridge/AleBridge
    2.1. At this point the ALE Bridge should be listening at port 9090 for HTTP messages


3. Logging in to the web interface
    3.1. Open in a browser window the address 69.163.51.20   username/password = edison/<edison_password>
    3.2. Click on configure->reports
    3.3. Click on submit
        3.3.1.  At this you should be seeing the ip addresses of the machines that the MCON will be sending HTTP messages to
4. ssh into the MCON
    4.1. I use putty for this particular task, given that we have the .ppk key.
         or use 'ssh 69.163.51.20 -i open-ssh-mcon-key -l edison' from a linux system.
    4.2. Run the following command inside the MCON machine rtls_sim <NUMBER_OF_TAGS> <TIME_INTERVAL>
        4.2.2. e.g. rtls_sim 100 3 (This command would send 100 tags every three seconds)
        4.2.3. At this point the ALE bridge should be receiving data and inserting records into cassandra

/usr/local/apache-cassandra-2.0.11/conf/cassandra.yaml
