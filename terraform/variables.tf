variable "access_key" {}
variable "secret_key" {}
variable "ssh_private_key_path" {}
variable "ssh_pub_key" {}

variable "region" {
    default = "us-east-1"
}

variable "amis" {
    default = {
        us-east-1 = "ami-9ac195f2"
    }
}

variable "instance_names" {
  default = {
    instance0 = "data.worker0"
    instance1 = "data.worker1"
  }
}

variable "instance_sizes" {
  default = {
    instance0 = "t2.medium"
    instance1 = "t2.small"
  }
}