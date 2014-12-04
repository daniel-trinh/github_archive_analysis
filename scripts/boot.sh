### Disables ssh password logins
echo 'PermitRootLogin without-password' >> /etc/ssh/sshd_config
# Grabs sshd process id
SSHD_PID="$(ps auxw | grep "/usr/sbin/sshd" | awk '{print $2}' |  sed -n '1 p')"
kill -HUP $SSHD_PID

# install wget
yum -y install wget
# install sbt
yum -y localinstall https://dl.bintray.com/sbt/rpm/sbt-0.13.7.rpm
# install git
yum -y install git
# install netcat
yum -y install nc

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