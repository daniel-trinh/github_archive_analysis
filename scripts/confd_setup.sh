#!/bin/bash
### Create confd template files

sudo mkdir /etc/confd
sudo mkdir /etc/confd/conf.d
sudo mkdir /etc/confd/templates

for filename in confd/*.toml; do
  name=${filename##*/}
  echo "writing confd/$name to /etc/confd/conf.d/$name"
  sudo cat "confd/$name" | sudo tee "/etc/confd/conf.d/$name"
done

for filename in confd/*.tmpl; do
  name=${filename##*/}
  echo "writing confd/$name to /etc/confd/templates/$name"
  sudo cat "confd/$name" | sudo tee "/etc/confd/templates/$name"
done