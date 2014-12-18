### Disables ssh password logins
echo 'PermitRootLogin without-password' >> /etc/ssh/sshd_config
echo 'PasswordAuthentication no' >> /etc/ssh/sshd_config

# Grabs sshd process id
SSHD_PID="$(ps auxw | grep "/usr/sbin/sshd" | awk '{print $2}' |  sed -n '1 p')"
kill -HUP $SSHD_PID

# Restart sshd
/etc/init.d/sshd restart

yum -y install wget
yum -y localinstall https://dl.bintray.com/sbt/rpm/sbt-0.13.7.rpm
yum -y install git
yum -y install nc
yum -y install unzip
yum -y install fail2ban

# Set environment to production. This should probably in conf.d
# TODO: move to conf.d
echo 'production' >> /etc/env

# Increase sbt size
export SBT_OPTS="-Xmx512M -XX:+UseConcMarkSweepGC -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=512M -Xss2M  -Duser.timezone=GMT"
export HADOOP_HOME="/opt/cloudera/parcels/CDH/lib/hadoop/etc/hadoop"

# Download cloudera manager setup. only needed on one node
wget http://archive.cloudera.com/cm5/installer/latest/cloudera-manager-installer.bin
chmod u+x cloudera-manager-installer.bin
sudo ./cloudera-manager-installer.bin

# Upload new ssh key
cat ~/.ssh/oss_rsa.pub | ssh root@104.236.14.211 "cat >> ~/.ssh/authorized_keys"

# modify /etc/hosts
# TODO: use conf.d
echo '10.132.164.226 hadoop1.danieltrinh.com hadoop1' >> /etc/hosts
echo '10.132.165.24 hadoop2.danieltrinh.com hadoop2' >> /etc/hosts
echo '10.132.164.88 hadoop3.danieltrinh.com hadoop3' >> /etc/hosts
echo '10.132.164.89 hadoop4.danieltrinh.com haddop4' >> /etc/hosts

# Setup Repoforge to install htop
wget http://packages.sw.be/rpmforge-release/rpmforge-release-0.5.2-2.el6.rf.i686.rpm
rpm -ihv rpmforge-release*.rf.i686.rpm
yum -y install htop
/bin/rm rpmforge-release-0.5.2-2.el6.rf.i686.rpm

# forward port traffic for browsing remote servers
ssh -L 7180:localhost:7180 root@104.236.59.249 -N

# Add supergroup for accessing hdfs
groupadd supergroup
usermod -a -G supergroup root

# Ignore overcommitment memory issues. This is a hack.
echo 1 > /proc/sys/vm/overcommit_memory

# Setup confd to share config across nodes
wget https://github.com/kelseyhightower/confd/releases/download/v0.6.3/confd-0.6.3-linux-amd64
mv confd-0.6.3-linux-amd64 /usr/local/bin/confd
chmod +x /usr/local/bin/confd

# Setup consul for confd backend
wget https://dl.bintray.com/mitchellh/consul/0.4.1_linux_amd64.zip
unzip 0.4.1_linux_amd64.zip
mv consul /usr/local/bin/consul
chmod +x /usr/local/bin/consul
/bin/rm 0.4.1_linux_amd64.zip

# Create data folder for consul
mkdir /etc/consul

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

# TODO: conf.d
sudo mkdir -p /etc/confd/{conf.d,templates}

# TODO: change log directory of fail2ban for sshd, enable sshd scanning
sudo service fail2ban restart