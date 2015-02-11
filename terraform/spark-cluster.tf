variable "access_key" {}
variable "secret_key" {}
variable "ssh_key_path" {}
variable "ssh_pub_key" {}

variable "region" {
    default = "us-east-1"
}
variable "default_ami_id" {
}
variable "amis" {
    default = {
        us-east-1 = "${var.default_ami_id}"
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

provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

resource "aws_instance" "data_master" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "${lookup(var.instance_sizes, concat("instance", count.index))}"
    provisioner "remote-exec" {
      script = "bashrc_file"
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    provisioner "remote-exec" {
      inline = [
        "sudo echo '${aws_instance.data_master.private_ip}' | sudo tee /etc/master_private_ip",
        "sudo echo '${lookup(var.instance_names, concat("instance", count.index))}' | sudo tee /etc/host_prefix",
        "sudo echo '${var.ssh_pub_key}' | sudo tee -a /root/.ssh/authorized_keys"
        "sudo echo '${var.access_key}' | sudo tee -a /etc/access_key"
        "sudo echo '${var.secret_key}' | sudo tee -a /etc/secret_key"
      ]
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    key_name = "spark-cluster"
}

resource "aws_instance" "data_node" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "${lookup(var.instance_sizes, concat("instance", count.index))}"
    count = 2
    provisioner "remote-exec" {
      script = "bashrc_file"
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    provisioner "remote-exec" {
      inline = [
        "sudo echo '${aws_instance.data_master.private_ip}' | sudo tee /etc/master_private_ip",
        "sudo echo '${lookup(var.instance_names, concat("instance", count.index))}' | sudo tee /etc/host_prefix",
        "sudo echo '${var.ssh_pub_key}' | sudo tee -a /root/.ssh/authorized_keys"
        "sudo echo '${var.access_key}' | sudo tee -a /etc/access_key"
        "sudo echo '${var.secret_key}' | sudo tee -a /etc/secret_key"
      ]
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
      connection {
        user = "ec2-user"
        key_file = "~/.ssh/spark-cluster.pem"
      }
    }
    key_name = "spark-cluster"
}

resource "aws_eip" "ip_master" {
    instance = "${aws_instance.data_master.id}"
}
resource "aws_eip" "ip_worker1" {
    instance = "${aws_instance.data_node.0.id}"
}
resource "aws_eip" "ip_worker2" {
    instance = "${aws_instance.data_node.1.id}"
}

output "Message" {
  value = "Manually ssh and run ../scripts/load_consul_variables.sh on the following IPs: ${aws_eip.ip_master.public_ip}"
}
output "ip_master" {
  value = "Manually ssh and run ../scripts/load_consul_variables.sh on the following IPs: ${aws_eip.ip_master.public_ip}"
}
output "ip_worker1" {
  value = "${aws_eip.ip_worker1.public_ip}"
}
output "ip_worker2" {
  value = "${aws_eip.ip_worker2.public_ip}"
}