package scripts

import ammonite._

object ConfdSetup extends App {
  def createFiles: Unit = {
    val confdTomlDir = root/"etc"/"confd"/"conf.d"
    val confdTemplateDir = root/"etc"/"confd"/"templates"

    mkdir! confdTomlDir
    mkdir! confdTemplateDir

    write.over(confdTomlDir/"env.toml",
      """|[template]
        |src = "env.tmpl"
        |dest = "/etc/env"
        |keys = [
        |  "/environment"
        |]
        |""".stripMargin
    )
    write.over(confdTemplateDir/"env.tmpl",
      """{{getv "/environment"}}"""
    )

    write.over(confdTomlDir/"hosts.toml",
      """|[template]
        |src = "hosts.tmpl"
        |dest = "/etc/hosts"
        |keys = [
        |  "/hosts"
        |]
        |""".stripMargin
    )
    write.over(confdTemplateDir/"hosts.tmpl",
      """127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
        |::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
        |{{getv "/hosts"}}
        |""".stripMargin
    )

    write.over(confdTomlDir/"fail2banconfig.toml",
      """|[template]
        |src = "fail2banconfig.tmpl"
        |dest = "/etc/fail2ban/jail.local"
        |keys = [
        |  "/fail2banconfig/bantime"
        |]
        |reload_cmd = "service fail2ban restart"
      """.stripMargin
    )
    write.over(confdTemplateDir/"fail2banconfig.tmpl",
      """|# Fail2Ban jail specifications file
        |#
        |# Comments: use '#' for comment lines and ';' for inline comments
        |#
        |# Changes:  in most of the cases you should not modify this
        |#           file, but provide customizations in jail.local file, e.g.:
        |#
        |# [DEFAULT]
        |# bantime = {{getv "/fail2banconfig/bantime"}}
        |#
        |# [ssh-iptables]
        |# enabled = true
        |#
        |
        |# The DEFAULT allows a global definition of the options. They can be overridden
        |# in each jail afterwards.
        |
        |[DEFAULT]
        |
        |# "ignoreip" can be an IP address, a CIDR mask or a DNS host. Fail2ban will not
        |# ban a host which matches an address in this list. Several addresses can be
        |# defined using space separator.
        |ignoreip = 127.0.0.1/8
        |
        |# "bantime" is the number of seconds that a host is banned.
        |bantime  = 3600
        |
        |# A host is banned if it has generated "maxretry" during the last "findtime"
        |# seconds.
        |findtime  = 600
        |
        |# "maxretry" is the number of failures before a host get banned.
        |maxretry = 3
        |
        |# "backend" specifies the backend used to get files modification.
        |# Available options are "pyinotify", "gamin", "polling" and "auto".
        |# This option can be overridden in each jail as well.
        |#
        |# pyinotify: requires pyinotify (a file alteration monitor) to be installed.
        |#              If pyinotify is not installed, Fail2ban will use auto.
        |# gamin:     requires Gamin (a file alteration monitor) to be installed.
        |#              If Gamin is not installed, Fail2ban will use auto.
        |# polling:   uses a polling algorithm which does not require external libraries.
        |# auto:      will try to use the following backends, in order:
        |#              pyinotify, gamin, polling.
        |backend = auto
        |
        |# "usedns" specifies if jails should trust hostnames in logs,
        |#   warn when reverse DNS lookups are performed, or ignore all hostnames in logs
        |#
        |# yes:   if a hostname is encountered, a reverse DNS lookup will be performed.
        |# warn:  if a hostname is encountered, a reverse DNS lookup will be performed,
        |#        but it will be logged as a warning.
        |# no:    if a hostname is encountered, will not be used for banning,
        |#        but it will be logged as info.
        |usedns = warn
        |
        |
        |# This jail corresponds to the standard configuration in Fail2ban 0.6.
        |# The mail-whois action send a notification e-mail with a whois request
        |# in the body.
        |
        |[ssh-iptables]
        |
        |enabled  = true
        |filter   = sshd
        |action   = iptables[name=SSH, port=ssh, protocol=tcp]
        |           sendmail-whois[name=SSH, dest=you@example.com, sender=fail2ban@example.com]
        |logpath  = /var/log/secure
        |maxretry = 5
        |
        |[proftpd-iptables]
        |
        |enabled  = false
        |filter   = proftpd
        |action   = iptables[name=ProFTPD, port=ftp, protocol=tcp]
        |           sendmail-whois[name=ProFTPD, dest=you@example.com]
        |logpath  = /var/log/proftpd/proftpd.log
        |maxretry = 6
        |
        |# This jail forces the backend to "polling".
        |
        |[sasl-iptables]
        |
        |enabled  = false
        |filter   = sasl
        |backend  = polling
        |action   = iptables[name=sasl, port=smtp, protocol=tcp]
        |           sendmail-whois[name=sasl, dest=you@example.com]
        |logpath  = /var/log/mail.log
        |
        |# Here we use TCP-Wrappers instead of Netfilter/Iptables. "ignoreregex" is
        |# used to avoid banning the user "myuser".
        |
        |[ssh-tcpwrapper]
        |
        |enabled     = false
        |filter      = sshd
        |action      = hostsdeny
        |              sendmail-whois[name=SSH, dest=you@example.com]
        |ignoreregex = for myuser from
        |logpath     = /var/log/sshd.log
        |
        |# This jail demonstrates the use of wildcards in "logpath".
        |# Moreover, it is possible to give other files on a new line.
        |
        |[apache-tcpwrapper]
        |
        |enabled  = false
        |filter   = apache-auth
        |action   = hostsdeny
        |logpath  = /var/log/apache*/*error.log
        |           /home/www/myhomepage/error.log
        |maxretry = 6
        |
        |# The hosts.deny path can be defined with the "file" argument if it is
        |# not in /etc.
        |
        |[postfix-tcpwrapper]
        |
        |enabled  = false
        |filter   = postfix
        |action   = hostsdeny[file=/not/a/standard/path/hosts.deny]
        |           sendmail[name=Postfix, dest=you@example.com]
        |logpath  = /var/log/postfix.log
        |bantime  = 300
        |
        |# Do not ban anybody. Just report information about the remote host.
        |# A notification is sent at most every 600 seconds (bantime).
        |
        |[vsftpd-notification]
        |
        |enabled  = false
        |filter   = vsftpd
        |action   = sendmail-whois[name=VSFTPD, dest=you@example.com]
        |logpath  = /var/log/vsftpd.log
        |maxretry = 5
        |bantime  = 1800
        |
        |# Same as above but with banning the IP address.
        |
        |[vsftpd-iptables]
        |
        |enabled  = false
        |filter   = vsftpd
        |action   = iptables[name=VSFTPD, port=ftp, protocol=tcp]
        |           sendmail-whois[name=VSFTPD, dest=you@example.com]
        |logpath  = /var/log/vsftpd.log
        |maxretry = 5
        |bantime  = 1800
        |
        |# Ban hosts which agent identifies spammer robots crawling the web
        |# for email addresses. The mail outputs are buffered.
        |
        |[apache-badbots]
        |
        |enabled  = false
        |filter   = apache-badbots
        |action   = iptables-multiport[name=BadBots, port="http,https"]
        |           sendmail-buffered[name=BadBots, lines=5, dest=you@example.com]
        |logpath  = /var/www/*/logs/access_log
        |bantime  = 172800
        |maxretry = 1
        |
        |# Use shorewall instead of iptables.
        |
        |[apache-shorewall]
        |
        |enabled  = false
        |filter   = apache-noscript
        |action   = shorewall
        |           sendmail[name=Postfix, dest=you@example.com]
        |logpath  = /var/log/apache2/error_log
        |
        |# Ban attackers that try to use PHP's URL-fopen() functionality
        |# through GET/POST variables. - Experimental, with more than a year
        |# of usage in production environments.
        |
        |[php-url-fopen]
        |
        |enabled = false
        |port    = http,https
        |filter  = php-url-fopen
        |logpath = /var/www/*/logs/access_log
        |maxretry = 1
        |
        |# A simple PHP-fastcgi jail which works with lighttpd.
        |# If you run a lighttpd server, then you probably will
        |# find these kinds of messages in your error_log:
        |# ALERT – tried to register forbidden variable ‘GLOBALS’
        |# through GET variables (attacker '1.2.3.4', file '/var/www/default/htdocs/index.php')
        |# This jail would block the IP 1.2.3.4.
        |
        |[lighttpd-fastcgi]
        |
        |enabled = false
        |port    = http,https
        |filter  = lighttpd-fastcgi
        |# adapt the following two items as needed
        |logpath = /var/log/lighttpd/error.log
        |maxretry = 2
        |
        |# Same as above for mod_auth
        |# It catches wrong authentifications
        |
        |[lighttpd-auth]
        |
        |enabled = false
        |port    = http,https
        |filter  = lighttpd-auth
        |# adapt the following two items as needed
        |logpath = /var/log/lighttpd/error.log
        |maxretry = 2
        |
        |# This jail uses ipfw, the standard firewall on FreeBSD. The "ignoreip"
        |# option is overridden in this jail. Moreover, the action "mail-whois" defines
        |# the variable "name" which contains a comma using "". The characters '' are
        |# valid too.
        |
        |[ssh-ipfw]
        |
        |enabled  = false
        |filter   = sshd
        |action   = ipfw[localhost=192.168.0.1]
        |           sendmail-whois[name="SSH,IPFW", dest=you@example.com]
        |logpath  = /var/log/auth.log
        |ignoreip = 168.192.0.1
        |
        |# These jails block attacks against named (bind9). By default, logging is off
        |# with bind9 installation. You will need something like this:
        |#
        |# logging {
        |#     channel security_file {
        |#         file "/var/log/named/security.log" versions 3 size 30m;
        |#         severity dynamic;
        |#         print-time yes;
        |#     };
        |#     category security {
        |#         security_file;
        |#     };
        |# };
        |#
        |# in your named.conf to provide proper logging.
        |# This jail blocks UDP traffic for DNS requests.
        |
        |# !!! WARNING !!!
        |#   Since UDP is connection-less protocol, spoofing of IP and imitation
        |#   of illegal actions is way too simple.  Thus enabling of this filter
        |#   might provide an easy way for implementing a DoS against a chosen
        |#   victim. See
        |#    http://nion.modprobe.de/blog/archives/690-fail2ban-+-dns-fail.html
        |#   Please DO NOT USE this jail unless you know what you are doing.
        |#
        |# [named-refused-udp]
        |#
        |# enabled  = false
        |# filter   = named-refused
        |# action   = iptables-multiport[name=Named, port="domain,953", protocol=udp]
        |#            sendmail-whois[name=Named, dest=you@example.com]
        |# logpath  = /var/log/named/security.log
        |# ignoreip = 168.192.0.1
        |
        |# This jail blocks TCP traffic for DNS requests.
        |
        |[named-refused-tcp]
        |
        |enabled  = false
        |filter   = named-refused
        |action   = iptables-multiport[name=Named, port="domain,953", protocol=tcp]
        |           sendmail-whois[name=Named, dest=you@example.com]
        |logpath  = /var/log/named/security.log
        |ignoreip = 168.192.0.1
        |
        |# Multiple jails, 1 per protocol, are necessary ATM:
        |# see https://github.com/fail2ban/fail2ban/issues/37
        |[asterisk-tcp]
        |
        |enabled  = false
        |filter   = asterisk
        |action   = iptables-multiport[name=asterisk-tcp, port="5060,5061", protocol=tcp]
        |           sendmail-whois[name=Asterisk, dest=you@example.com, sender=fail2ban@example.com]
        |logpath  = /var/log/asterisk/messages
        |maxretry = 10
        |
        |[asterisk-udp]
        |
        |enabled  = false
        |filter   = asterisk
        |action   = iptables-multiport[name=asterisk-udp, port="5060,5061", protocol=udp]
        |           sendmail-whois[name=Asterisk, dest=you@example.com, sender=fail2ban@example.com]
        |logpath  = /var/log/asterisk/messages
        |maxretry = 10
        |
        |# Jail for more extended banning of persistent abusers
        |# !!! WARNING !!!
        |#   Make sure that your loglevel specified in fail2ban.conf/.local
        |#   is not at DEBUG level -- which might then cause fail2ban to fall into
        |#   an infinite loop constantly feeding itself with non-informative lines
        |[recidive]
        |
        |enabled  = false
        |filter   = recidive
        |logpath  = /var/log/fail2ban.log
        |action   = iptables-allports[name=recidive]
        |           sendmail-whois-lines[name=recidive, logpath=/var/log/fail2ban.log]
        |bantime  = 604800  ; 1 week
        |findtime = 86400   ; 1 day
        |maxretry = 5
      """.stripMargin
    )

    write.over(confdTomlDir/"spark_env.tmpl",
      """[template]
        |src = "spark_env.tmpl"
        |dest = "/etc/spark/conf/spark-env.sh"
        |keys = [
        |]
        |reload_cmd = "
        |  service spark-master condrestart
        |  service spark-worker restart
        |"
      """.stripMargin)
    write.over(confdTemplateDir/"spark_env.tmpl",
      """|#!/usr/bin/env bash
        |
        |# This file is sourced when running various Spark programs.
        |# Copy it as spark-env.sh and edit that to configure Spark for your site.
        |
        |# Options read when launching programs locally with
        |# ./bin/run-example or ./bin/spark-submit
        |# - HADOOP_CONF_DIR, to point Spark towards Hadoop configuration files
        |# - SPARK_LOCAL_IP, to set the IP address Spark binds to on this node
        |# - SPARK_PUBLIC_DNS, to set the public dns name of the driver program
        |# - SPARK_CLASSPATH, default classpath entries to append
        |
        |# Options read by executors and drivers running inside the cluster
        |# - SPARK_LOCAL_IP, to set the IP address Spark binds to on this node
        |# - SPARK_PUBLIC_DNS, to set the public DNS name of the driver program
        |# - SPARK_CLASSPATH, default classpath entries to append
        |# - SPARK_LOCAL_DIRS, storage directories to use on this node for shuffle and RDD data
        |# - MESOS_NATIVE_LIBRARY, to point to your libmesos.so if you use Mesos
        |
        |# Options read in YARN client mode
        |# - HADOOP_CONF_DIR, to point Spark towards Hadoop configuration files
        |# - SPARK_EXECUTOR_INSTANCES, Number of workers to start (Default: 2)
        |# - SPARK_EXECUTOR_CORES, Number of cores for the workers (Default: 1).
        |# - SPARK_EXECUTOR_MEMORY, Memory per Worker (e.g. 1000M, 2G) (Default: 1G)
        |# - SPARK_DRIVER_MEMORY, Memory for Master (e.g. 1000M, 2G) (Default: 512 Mb)
        |# - SPARK_YARN_APP_NAME, The name of your application (Default: Spark)
        |# - SPARK_YARN_QUEUE, The hadoop queue to use for allocation requests (Default: ‘default’)
        |# - SPARK_YARN_DIST_FILES, Comma separated list of files to be distributed with the job.
        |# - SPARK_YARN_DIST_ARCHIVES, Comma separated list of archives to be distributed with the job.
        |
        |# Options for the daemons used in the standalone deploy mode
        |# - SPARK_MASTER_IP, to bind the master to a different IP address or hostname
        |# - SPARK_MASTER_PORT / SPARK_MASTER_WEBUI_PORT, to use non-default ports for the master
        |# - SPARK_MASTER_OPTS, to set config properties only for the master (e.g. "-Dx=y")
        |# - SPARK_WORKER_CORES, to set the number of cores to use on this machine
        |# - SPARK_WORKER_MEMORY, to set how much total memory workers have to give executors (e.g. 1000m, 2g)
        |# - SPARK_WORKER_PORT / SPARK_WORKER_WEBUI_PORT, to use non-default ports for the worker
        |# - SPARK_WORKER_INSTANCES, to set the number of worker processes per node
        |# - SPARK_WORKER_DIR, to set the working directory of worker processes
        |# - SPARK_WORKER_OPTS, to set config properties only for the worker (e.g. "-Dx=y")
        |# - SPARK_HISTORY_OPTS, to set config properties only for the history server (e.g. "-Dx=y")
        |# - SPARK_DAEMON_JAVA_OPTS, to set config properties for all daemons (e.g. "-Dx=y")
        |# - SPARK_PUBLIC_DNS, to set the public dns name of the master or workers
        |
        |# Generic options for the daemons used in the standalone deploy mode
        |# - SPARK_CONF_DIR      Alternate conf dir. (Default: ${SPARK_HOME}/conf)
        |# - SPARK_LOG_DIR       Where log files are stored.  (Default: ${SPARK_HOME}/logs)
        |# - SPARK_PID_DIR       Where the pid file is stored. (Default: /tmp)
        |# - SPARK_IDENT_STRING  A string representing this instance of spark. (Default: $USER)
        |# - SPARK_NICENESS      The scheduling priority for daemons. (Default: 0)
        |
        |###
        |### === IMPORTANT ===
        |### Change the following to specify a real cluster's Master host
        |###
        |export STANDALONE_SPARK_MASTER_HOST=`hostname`
        |
        |export SPARK_MASTER_IP=$STANDALONE_SPARK_MASTER_HOST
        |
        |### Let's run everything with JVM runtime, instead of Scala
        |export SPARK_LAUNCH_WITH_SCALA=0
        |export SPARK_LIBRARY_PATH=${SPARK_HOME}/lib
        |export SCALA_LIBRARY_PATH=${SPARK_HOME}/lib
        |export SPARK_MASTER_WEBUI_PORT=18080
        |export SPARK_MASTER_PORT=7077
        |export SPARK_WORKER_PORT=7078
        |export SPARK_WORKER_WEBUI_PORT=18081
        |export SPARK_WORKER_DIR=/var/run/spark/work
        |export SPARK_LOG_DIR=/var/log/spark
        |export SPARK_PID_DIR='/var/run/spark/'
        |
        |if [ -n "$HADOOP_HOME" ]; then
        |  export LD_LIBRARY_PATH=:/lib/native
        |fi
        |
        |export HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-etc/hadoop/conf}
        |
        |### Comment above 2 lines and uncomment the following if
        |### you want to run with scala version, that is included with the package
        |#export SCALA_HOME=${SCALA_HOME:-/usr/lib/spark/scala}
        |#export PATH=$PATH:$SCALA_HOME/bin
      """.stripMargin)

    write.over(confdTomlDir/"spark_defaults.toml",
      """|[template]
         |src = "spark_defaults.tmpl"
         |dest = "/etc/spark/conf/spark-defaults.conf"
         |keys = [
         |]
         |reload_cmd =  "
         |  service spark-master condrestart
         |  service spark-worker restart
         |"
         |""".stripMargin)
    write.over(confdTemplateDir/"spark_defaults.tmpl",
      """|# Default system properties included when running spark-submit.
         |# This is useful for setting default environmental settings.
         |
         |# Example:
         |# spark.master                     spark://master:7077
         |# spark.eventLog.enabled           true
         |# spark.eventLog.dir               hdfs://namenode:8021/directory
         |# spark.serializer                 org.apache.spark.serializer.KryoSerializer
         |# spark.driver.memory              5g
         |# spark.executor.extraJavaOptions  -XX:+PrintGCDetails -Dkey=value -Dnumbers="one two three"
      """.stripMargin)

    write.over(confdTomlDir/"core_site.toml",
      """[template]
        |src = "core_site.tmpl"
        |dest = "/etc/hadoop/conf/core-site.xml"
        |keys = [
        |  "/aws_key",
        |  "/aws_secret_key"
        |]
        |uid = 0
        |reload_cmd = "export AWS_ACCESS_KEY_ID={{getv \"/aws_key\"}} && export AWS_SECRET_ACCESS_KEY={{getv \"/aws_secret_key\"}}"
      """.stripMargin)
    write.over(confdTemplateDir/"core_site.tmpl",
      """|<?xml version="1.0" encoding="UTF-8"?>
        |
        |<configuration>
        |  <property>
        |    <name>dfs.namenode.name.dir</name>
        |    <value>file:///dfs/nn</value>
        |  </property>
        |  <property>
        |    <name>dfs.namenode.servicerpc-address</name>
        |    <value>hadoop1.danieltrinh.com:8022</value>
        |  </property>
        |  <property>
        |    <name>dfs.https.address</name>
        |    <value>hadoop1.danieltrinh.com:50470</value>
        |  </property>
        |  <property>
        |    <name>dfs.https.port</name>
        |    <value>50470</value>
        |  </property>
        |  <property>
        |    <name>dfs.namenode.http-address</name>
        |    <value>hadoop1.danieltrinh.com:50070</value>
        |  </property>
        |  <property>
        |    <name>dfs.replication</name>
        |    <value>3</value>
        |  </property>
        |  <property>
        |    <name>dfs.blocksize</name>
        |    <value>134217728</value>
        |  </property>
        |  <property>
        |    <name>dfs.client.use.datanode.hostname</name>
        |    <value>false</value>
        |  </property>
        |  <property>
        |    <name>fs.permissions.umask-mode</name>
        |    <value>022</value>
        |  </property>
        |  <property>
        |    <name>dfs.namenode.acls.enabled</name>
        |    <value>false</value>
        |  </property>
        |  <property>
        |    <name>dfs.client.read.shortcircuit</name>
        |    <value>false</value>
        |  </property>
        |  <property>
        |    <name>dfs.domain.socket.path</name>
        |    <value>/var/run/hdfs-sockets/dn</value>
        |  </property>
        |  <property>
        |    <name>dfs.client.read.shortcircuit.skip.checksum</name>
        |    <value>false</value>
        |  </property>
        |  <property>
        |    <name>dfs.client.domain.socket.data.traffic</name>
        |    <value>false</value>
        |  </property>
        |  <property>
        |    <name>dfs.datanode.hdfs-blocks-metadata.enabled</name>
        |    <value>true</value>
        |  </property>
        |  <property>
        |    <name>fs.s3.awsAccessKeyId</name>
        |    <value>{{getv "/aws_key"}}</value>
        |  </property>
        |  <property>
        |    <name>fs.s3.awsSecretAccessKey</name>
        |    <value>{{getv "/aws_secret_key"}}</value>
        |  </property>
        |</configuration>
      """.stripMargin
    )
  }

  createFiles
}