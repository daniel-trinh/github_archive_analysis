# Spark Cluster Deployer (eventually Github Archive Analyzer)
Code for setting up a 3 node spark cluster.

## Cluster Setup Steps
* Get an AWS account
* Install [packer](https://packer.io/)
* Install [terraform](https://terraform.io/)
* Run packer using `packer build -var 'aws_access_key=<key goes here>' -var 'aws_secret_key=<key goes here>' spark-ami.json` in the `packer` folder, and save the ami_id output for the next step
* Create a local ssh key if you don't have one already
* Create a ssh key file in AWS, and save the .pem file somewhere on your local machine
* Create a terraform.tfvars file in the `terraform` folder, based off of the terraform.tfvars.template file,
using the information from above
* Run terraform in the terraform folder, using `terraform apply`. Optionally run `terraform plan` to see what the cluster will look like.
* SSH into all nodes, and run `load_consul_variables.sh` under the scripts folder. This is to load variables into consul for confd to update configuration files.

## Things installed onto the cluster
* Scala via sbt
* fail2ban, to prevent random botnets from trying to brute force login to your server
* Spark
* Consul, fully connected
* Confd

## WARNING
The terraform code will put your AWS keys on each node, and consul
will load the keys into its cluster. See the provisioner sections in the spark-cluster.tf terraform file. Be careful.