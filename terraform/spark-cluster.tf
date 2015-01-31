variable "access_key" {}
variable "secret_key" {}
variable "ssh_pub_key" {}
variable "region" {
    default = "us-east-1"
}
variable "amis" {
    default = {
        us-east-1 = "ami-eebac086"
    }
}
variable "ips" {
    default = {
        data_master  = "10.0.0.1"
        data_worker1 = "10.0.0.2"
        data_worker2 = "10.0.0.3"
    }
}

provider "aws" {
    access_key = "${var.access_key}"
    secret_key = "${var.secret_key}"
    region     = "${var.region}"
}

resource "aws_instance" "data_master" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "t2.medium"
    provisioner "local-exec" {
      command = "echo '${aws_instance.web.private_ip}, data.master.danieltrinh.com' > private_ips.txt"
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
    }
    private_ip = "${lookup(var.ips,\"data_master\")}"
}

resource "aws_instance" "data_worker1" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "t2.medium"
    provisioner "local-exec" {
      command = "echo '${aws_instance.web.private_ip}, data.worker1.danieltrinh.com' > private_ips.txt"
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
    }
    private_ip = "${lookup(var.ips,\"data_master\")}"
}

resource "aws_instance" "data_worker2" {
    ami           = "${lookup(var.amis, var.region)}"
    instance_type = "t2.small"
    provisioner "local-exec" {
      command = "echo ${aws_instance.web.private_ip}, data.worker2.danieltrinh.com > private_ips.txt"
    }
    provisioner "remote-exec" {
      script = "../scripts/boot.sh"
    }
    private_ip = "${lookup(var.ips,\"data_worker2\")}"
}

resource "aws_eip" "ip_master" {
    instance = "${aws_instance.data_master.id}"
}
resource "aws_eip" "ip_worker1" {
    instance = "${aws_instance.data_worker1.id}"
}
resource "aws_eip" "ip_worker2" {
    instance = "${aws_instance.data_worker2.id}"
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