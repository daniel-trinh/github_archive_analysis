#!/bin/bash

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

### Spark stuff

# TODO: move to confd .sh and run the .sh file
# sudo service spark-master start
# OR
# sudo service spark-worker start

# on one node: sudo service spark-history-server start