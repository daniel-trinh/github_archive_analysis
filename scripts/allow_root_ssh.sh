#!/bin/bash

sudo sed -i "s/command=\"echo 'Please login as the ec2-user user rather than root user\.';echo;sleep 10\" //g" /root/.ssh/authorized_keys
