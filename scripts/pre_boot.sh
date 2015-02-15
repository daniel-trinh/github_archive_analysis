#!/bin/bash

sudo echo '
# .bashrc

# Source global definitions
if [ -f /etc/bashrc ]; then
  . /etc/bashrc
fi

# User specific aliases and functions

export PATH=/bin:/sbin:/usr/bin:/usr/local/sbin:/usr/local/bin:$PATH
export AWS_ACCESS_KEY_ID="$(cat /etc/access_key)"
export AWS_SECRET_ACCESS_KEY="$(cat /etc/secret_key)"
' | sudo tee -a /root/.bashrc


# Replace code that prevents ssh via root
sudo sed -i "s/command=\"echo 'Please login as the ec2-user user rather than root user\.';echo;sleep 10\" //g" /root/.ssh/authorized_keys

# Allow ssh as root user
sudo echo 'PermitRootLogin without-password' | sudo tee -a /etc/ssh/sshd_config
sudo echo 'PasswordAuthentication no' | sudo tee -a /etc/ssh/sshd_config
sudo service sshd restart

sleep 5