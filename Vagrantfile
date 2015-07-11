# -*- mode: ruby -*-
# # vi: set ft=ruby :

require 'fileutils'

Vagrant.require_version ">= 1.6.0"

CLOUD_CONFIG_PATH = File.join(File.dirname(__FILE__), "user-data")

CONFIG = File.join(File.dirname(__FILE__), "config.rb")

# Defaults for config options defined in CONFIG
$update_channel = "stable"
$vb_gui = false
$vb_memory = 512
$vb_cpus = 1


if File.exist?(CONFIG)
  require CONFIG
end

Vagrant.configure("2") do |config|
  config.vm.box = "coreos-%s" % $update_channel
  config.vm.box_version = ">= 308.0.1"
  config.vm.box_url = "http://%s.release.core-os.net/amd64-usr/current/coreos_production_vagrant.json" % $update_channel

  config.vm.provider :virtualbox do |v|
    # On VirtualBox, we don't have guest additions or a functional vboxsf
    # in CoreOS, so tell Vagrant that so it can be smarter.
    v.check_guest_additions = false
    v.functional_vboxsf     = false
  end

  # plugin conflict
  if Vagrant.has_plugin?("vagrant-vbguest") then
    config.vbguest.auto_update = false
  end

  $vb_gui_local = $vb_gui
  $vb_cpus_local = $vb_cpus
  $vb_memory_local = $vb_memory
  $cloud_config_local = CLOUD_CONFIG_PATH

    config.vm.define vm_name = "core-1" do |config|
      config.vm.hostname = vm_name

      config.vm.provider :vmware_fusion do |vb|
        vb.gui = $vb_gui_local
      end

      config.vm.provider :virtualbox do |vb|
        vb.gui = $vb_gui_local
        vb.memory = $vb_memory_local
        vb.cpus = $vb_cpus_local
      end

      ip = "172.17.8.201"
      config.vm.network :private_network, ip: ip

      if File.exist?($cloud_config_local)
        config.vm.provision :file, :run => "always", :source => "#{$cloud_config_local}", :destination => "/tmp/vagrantfile-user-data"
        config.vm.provision :shell, :run => "always", :inline => "mv /tmp/vagrantfile-user-data /var/lib/coreos-vagrant/", :privileged => true
      end
    end
end