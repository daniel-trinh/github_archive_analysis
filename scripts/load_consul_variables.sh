#!/bin/bash

### This script needs to be manually run on the cluster
# after all nodes have been launched.

# Get this node's private IP
PRIVATE_IP="$(ip addr | grep 'state UP' -A2 | tail -n1 | awk '{print $2}' | cut -f1  -d'/')"

curl -X PUT -d "$PRIVATE_IP $HOST_PREFIX.danieltrinh.com $HOST_PREFIX" "http://localhost:8500/v1/kv/hosts/$HOST_PREFIX"
