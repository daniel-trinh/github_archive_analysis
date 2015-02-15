provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

resource "aws_instance" "data_master" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "${lookup(var.instance_sizes, concat("instance", count.index))}"
    provisioner "remote-exec" {
      script = "../scripts/pre_boot.sh"
      connection {
        user = "ec2-user"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    provisioner "remote-exec" {
      inline = [
        "sudo echo '${aws_instance.data_master.private_ip}' | sudo tee /etc/master_private_ip",
        "sudo echo '${lookup(var.instance_names, concat("instance", count.index))}' | sudo tee /etc/host_prefix",
        "sudo echo '${var.ssh_pub_key}' | sudo tee -a /root/.ssh/authorized_keys",
        "sudo echo '${var.access_key}' | sudo tee -a /etc/access_key",
        "sudo echo '${var.secret_key}' | sudo tee -a /etc/secret_key"
      ]
      connection {
        user = "ec2-user"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
      connection {
        user = "root"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    key_name = "spark-cluster"
    tags {
        Name = "data_master"
    }
}

resource "aws_instance" "data_node" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "${lookup(var.instance_sizes, concat("instance", count.index))}"
    count = 2
    provisioner "remote-exec" {
      script = "../scripts/pre_boot.sh"
      connection {
        user = "ec2-user"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    provisioner "remote-exec" {
      inline = [
        "sudo echo '${aws_instance.data_master.private_ip}' | sudo tee /etc/master_private_ip",
        "sudo echo '${lookup(var.instance_names, concat("instance", count.index))}' | sudo tee /etc/host_prefix",
        "sudo echo '${var.ssh_pub_key}' | sudo tee -a /root/.ssh/authorized_keys",
        "sudo echo '${var.access_key}' | sudo tee -a /etc/access_key",
        "sudo echo '${var.secret_key}' | sudo tee -a /etc/secret_key"
      ]
      connection {
        user = "root"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
      connection {
        user = "root"
        key_file = "${var.ssh_private_key_path}"
      }
    }
    key_name = "spark-cluster"
    tags {
        Name = "data_node${count.index}"
    }
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
  value = "Manually ssh and run ../scripts/load_consul_variables.sh on the following IPs"
}
output "ip_master" {
  value = "${aws_eip.ip_master.public_ip}"
}
output "ip_worker1" {
  value = "${aws_eip.ip_worker1.public_ip}"
}
output "ip_worker2" {
  value = "${aws_eip.ip_worker2.public_ip}"
}