#!/bin/bash

# Replace code that prevents ssh via root
sudo sed -i "s/command=\"echo 'Please login as the ec2-user user rather than root user\.';echo;sleep 10\" //g" /root/.ssh/authorized_keys

# Allow ssh as root user
sudo echo 'PermitRootLogin without-password' | sudo tee -a /etc/ssh/sshd_config
sudo echo 'PasswordAuthentication no' | sudo tee -a /etc/ssh/sshd_config
sudo echo -e "Host github.com\n\tStrictHostKeyChecking no\n" | sudo tee -a /root/.ssh/config
sudo service sshd restart
sleep 5