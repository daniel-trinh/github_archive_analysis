#!/bin/bash

# Restart sshd
/etc/init.d/sshd restart

# Ignore overcommitment memory issues. This is a hack.
echo 1 > /proc/sys/vm/overcommit_memory

# modify /etc/hosts
hosts_text='127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
10.132.164.226 hadoop1.danieltrinh.com hadoop1
10.132.165.24 hadoop2.danieltrinh.com hadoop2
10.132.164.88 hadoop3.danieltrinh.com hadoop3
10.132.164.89 hadoop4.danieltrinh.com hadoop4'

# This only needs to be done on one node
sudo -u hdfs hadoop fs -mkdir /user/spark
sudo -u hdfs hadoop fs -mkdir /user/spark/applicationHistory
sudo -u hdfs hadoop fs -chown -R spark:spark /user/spark
sudo -u hdfs hadoop fs -chmod 1777 /user/spark/applicationHistory

# Run consul in the background
HOST=`hostname`
if [[ $HOST == "hadoop2.danieltrinh.com" ]]; then
  # Server
  consul agent -server -data-dir /etc/consul >> /var/log/consul 2>&1 &
elif [[ $HOST == "hadoop3.danieltrinh.com" ]]; then
  # Leader and Server
  consul agent -bootstrap -server -data-dir /etc/consul >> /var/log/consul 2>&1 &
else
  # Client
  consul agent -data-dir /etc/consul >> /var/log/consul 2>&1 &
fi

# There should probably be a better way of joining the cluster, but this works for now
consul join 10.132.164.226:8301

# Run confd in background
confd -backend consul -node 127.0.0.1:8500 > /dev/null 2>&1 &

# TODO: conf.d
sudo mkdir -p /etc/confd/{conf.d,templates}

# TODO: change log directory of fail2ban for sshd, enable sshd scanning
sudo service fail2ban restart

sudo cd /root/src
sudo git clone https://github.com/daniel-trinh/github_archive_analysis.git

cd github_archive_analysis

# Run the conf.d setup code to create template files
sbt "project scripts" "run"

### Spark stuff

# TODO: move to confd .sh and run the .sh file
# sudo service spark-master start
# OR
# sudo service spark-worker restart

# on one node: sudo service spark-history-server start