#!/bin/bash

# This script needs to be manually run on each node in the cluster
# after all nodes have been launched.

# Get this node's private IP
PRIVATE_IP="$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/')"
HOST_PREFIX="$(cat /etc/host_prefix)"
if [ -z "$HOST_PREFIX" ]; then
  echoerr "/etc/host_prefix is empty, please populate with this cluster's master private IP"
  exit 1
fi
AWS_KEY="$(cat /etc/access_key)"
AWS_SECRET_KEY="$(cat /etc/secret_key)"

# TODO: don't hardcode this when I actually have an environment
# that isn't production
curl -X PUT -d "production" "http://localhost:8500/v1/kv/environment"
curl -X PUT -d "6000" "http://localhost:8500/v1/kv/fail2banconfig/bantime"
curl -X PUT -d "$AWS_KEY" "http://localhost:8500/v1/kv/aws_key"
curl -X PUT -d "$AWS_SECRET_KEY" "http://localhost:8500/v1/kv/aws_secret_key"
curl -X PUT -d "$PRIVATE_IP $HOST_PREFIX.danieltrinh.com $HOST_PREFIX" "http://localhost:8500/v1/kv/hosts/$HOST_PREFIX"
curl -X PUT -d "$MASTER_PRIVATE_IP" "http://localhost:8500/v1/kv/master_private_ip"