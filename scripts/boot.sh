#!/bin/bash

### Disables ssh password logins

# TODO: move into confd
# sudo echo 'PermitRootLogin without-password' >> /etc/ssh/sshd_config
# sudo echo 'PasswordAuthentication no' >> /etc/ssh/sshd_config

# TODO: move into confd
# export SBT_OPTS="-Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -Xss2M  -Duser.timezone=GMT"
# export HADOOP_HOME="/opt/cloudera/parcels/CDH/lib/hadoop/etc/hadoop"

echoerr() { echo "$@" 1>&2; }

checkFileExists() {
  if [ ! -f "$1" ]; then
    echoerr "$1 is missing, please populate with this cluster's master private IP"
    exit 1
  fi
}

checkFileExists "/etc/master_private_ip"
checkFileExists "/etc/host_prefix"

# /etc/master_private_ip needs to be set on the instance before this script runs
MASTER_PRIVATE_IP="$(cat /etc/master_private_ip)"
if [ -z "$MASTER_PRIVATE_IP" ]; then
  echoerr "/etc/master_private_ip is empty, please populate with this cluster's master private IP"
  exit 1
fi
HOST_PREFIX="$(cat /etc/host_prefix)"
if [ -z "$HOST_PREFIX" ]; then
  echoerr "/etc/host_prefix is empty, please populate with this cluster's master private IP"
  exit 1
fi
# Restart sshd
/etc/init.d/sshd restart

# Ignore overcommitment memory issues. This is a hack.
# echo 1 > /proc/sys/vm/overcommit_memory

# modify /etc/hosts
# NOTE: this gets populated with the IPs of the other nodes
# once consul is up and running.
# The master IP is used to connect the consul cluster initially.
HOSTS_TEXT="127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
$MASTER_PRIVATE_IP $HOST_PREFIX.danieltrinh.com $HOST_PREFIX"

# This only needs to be done on one node
# sudo -u hdfs hadoop fs -mkdir /user/spark
# sudo -u hdfs hadoop fs -mkdir /user/spark/applicationHistory
# sudo -u hdfs hadoop fs -chown -R spark:spark /user/spark
# sudo -u hdfs hadoop fs -chmod 1777 /user/spark/applicationHistory

# Run consul in the background
consul agent -server -bootstrap-expect 3 -data-dir /etc/consul >> /var/log/consul 2>&1 &

# Have all nodes join the master
consul join "$MASTER_PRIVATE_IP:8301"

sudo cd /root/src
sudo git clone https://github.com/daniel-trinh/github_archive_analysis.git

cd github_archive_analysis

# Run the conf.d setup code to create template files
sbt "project scripts" "run"

# Run confd in background
confd -interval 30 -backend consul -node 127.0.0.1:8500 > /var/log/confd 2>&1 &

# Get this node's private IP
PRIVATE_IP="$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/')"

# Add this node's private IP to consul so confd can reload /etc/hosts
curl -X PUT -d "$PRIVATE_IP $HOST_PREFIX.danieltrinh.com $HOST_PREFIX" "http://localhost:8500/v1/kv/hosts/$HOST_PREFIX"

### Spark stuff

# TODO: move to confd .sh and run the .sh file
if [ "$PRIVATE_IP" -eq "$MASTER_PRIVATE_IP" ]; then
  service spark-master start
  service spark-worker start
else
  service spark-worker start
fi