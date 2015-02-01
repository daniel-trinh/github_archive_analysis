#!/bin/bash

echo "Installing a whole bunch of yum dependencies..."
sudo yum -y install wget
sudo yum -y localinstall https://dl.bintray.com/sbt/rpm/sbt-0.13.7.rpm
sudo yum -y install git
sudo yum -y install nc
sudo yum -y install unzip
rpm -Uvh http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
sudo yum -y install yum-utils
sudo yum-config-manager --enable epel
sudo yum-config-manager --add-repo http://archive.cloudera.com/cdh5/redhat/6/x86_64/cdh/cloudera-cdh5.repo
sudo yum -y install fail2ban
sudo yum -y install spark-core spark-master spark-worker spark-history-server
sudo yum -y install java-1.7.0-openjdk-devel
sudo yum -y install hadoop-client
sudo yum clean all

# Download cloudera manager setup. only needed on one node
# wget http://archive.cloudera.com/cm5/installer/latest/cloudera-manager-installer.bin
# chmod u+x cloudera-manager-installer.bin
# sudo ./cloudera-manager-installer.bin

# Upload new ssh key
# cat ~/.ssh/oss_rsa.pub | ssh root@104.236.14.211 "cat >> ~/.ssh/authorized_keys"
# Setup Repoforge to install htop
sudo wget http://apt.sw.be/redhat/el6/en/i386/rpmforge/RPMS/rpmforge-release-0.5.2-2.el6.rf.i686.rpm
sudo rpm -ihv rpmforge-release*.rf.i686.rpm
sudo yum -y install htop
sudo /bin/rm rpmforge-release-0.5.2-2.el6.rf.i686.rpm

# forward port traffic for browsing remote servers
# ssh -L 7180:localhost:7180 root@104.236.59.249 -N

# Add supergroup for accessing hdfs
sudo groupadd supergroup
sudo usermod -a -G supergroup root

#### Confd stuff

# Setup confd to share config across nodes
sudo wget https://github.com/kelseyhightower/confd/releases/download/v0.6.3/confd-0.6.3-linux-amd64
sudo mv confd-0.6.3-linux-amd64 /usr/local/bin/confd
sudo chmod +x /usr/local/bin/confd

# Setup consul for confd backend
sudo wget https://dl.bintray.com/mitchellh/consul/0.4.1_linux_amd64.zip
sudo unzip 0.4.1_linux_amd64.zip
sudo mv consul /usr/local/bin/consul
sudo chmod +x /usr/local/bin/consul
sudo /bin/rm 0.4.1_linux_amd64.zip

# Create data folder for consul
sudo mkdir /etc/consul
# Directory for consul configurations
sudo mkdir -p /etc/consul.d/{bootstrap,server,client}

# Create folders for confd configs
sudo mkdir /etc/confd
sudo mkdir /etc/confd/conf.d
sudo mkdir /etc/confd/templates/
sudo mkdir /root/src/