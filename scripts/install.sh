#!/bin/bash

echo "Installing a whole bunch of yum dependencies..."

sudo yum -y install wget
sudo yum -y localinstall https://dl.bintray.com/sbt/rpm/sbt-0.13.7.rpm
sudo yum -y install nc
sudo yum -y install unzip
sudo yum -y install epel-release
sudo yum -y install yum-utils
sudo yum -y install gcc
sudo yum-config-manager --enable epel
sudo yum-config-manager --add-repo http://184.73.217.71/cdh5/redhat/6/x86_64/cdh/cloudera-cdh5.repo
sudo yum -y groupinstall "Development Tools"
sudo yum -y install gettext-devel openssl-devel perl-CPAN perl-devel zlib-devel
sudo yum -y install fail2ban
sudo yum -y install java-1.8.0-openjdk-devel
sudo yum -y install spark-core spark-master spark-worker spark-history-server
sudo yum -y install hadoop-client
sudo yum -y install ruby
sudo yum -y install golang
sudo yum -y install irb
sudo yum -y install git

sudo yum clean all
curl "https://bootstrap.pypa.io/get-pip.py" -o "get-pip.py"
sudo python get-pip.py
sudo pip install awscli

# Download cloudera manager setup. only needed on one node
# wget http://archive.cloudera.com/cm5/installer/latest/cloudera-manager-installer.bin
# chmod u+x cloudera-manager-installer.bin
# sudo ./cloudera-manager-installer.bin

# Upload new ssh key
# cat ~/.ssh/oss_rsa.pub | ssh root@104.236.14.211 "cat >> ~/.ssh/authorized_keys"
# Setup Repoforge to install htop
sudo wget "http://pkgs.repoforge.org/rpmforge-release/rpmforge-release-0.5.3-1.el7.rf.x86_64.rpm"
if [ ! -f rpmforge-release-0.5.3-1.el7.rf.x86_64.rpm ]; then
    exit 1
fi
sudo rpm -ihv rpmforge-release*.rf.x86_64.rpm
sudo yum -y install htop
sudo /bin/rm rpmforge-release-0.5.3-1.el7.rf.x86_64.rpm

# forward port traffic for browsing remote servers
# ssh -L 7180:localhost:7180 root@104.236.59.249 -N

# Add supergroup for accessing hdfs
sudo groupadd supergroup
sudo usermod -a -G supergroup root

#### Confd stuff

# Setup confd to share config across nodes
sudo wget https://github.com/kelseyhightower/confd/releases/download/v0.11.0/confd-0.11.0-linux-amd64
if [ ! -f confd-0.11.0-linux-amd64 ]; then
    exit 1
fi
sudo mv confd-0.11.0-linux-amd64 /usr/bin/confd
sudo chmod +x /usr/bin/confd

# Setup consul for confd backend
sudo wget https://releases.hashicorp.com/consul/0.6.3/consul_0.6.3_linux_amd64.zip
if [ ! -f consul_0.6.3_linux_amd64.zip ]; then
    exit 1
fi
sudo unzip consul_0.6.3_linux_amd64.zip
sudo mv consul /usr/bin/consul
sudo chmod +x /usr/bin/consul
sudo /bin/rm consul_0.6.3_linux_amd64.zip

# Create data folder for consul
sudo mkdir /etc/consul
# Directory for consul configurations
sudo mkdir -p /etc/consul.d/{bootstrap,server,client}

# Create folders for confd configs
sudo mkdir /etc/confd
sudo mkdir /etc/confd/conf.d
sudo mkdir /etc/confd/templates/
sudo mkdir /root/src/