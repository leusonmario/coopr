#
# Cookbook Name:: loom_base
# Recipe:: default
#
# Copyright 2013, Continuuity, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This forces an apt-get update on Ubuntu/Debian
case node['platform_family']
when 'debian'
  include_recipe 'apt::default'
when 'rhel'
  include_recipe 'yum-epel::default' if node['base']['use_epel'].to_s == 'true'
end

# We always run our dns, firewall, and hosts cookbooks
%w(dns firewall hosts).each do |cb|
  include_recipe "loom_#{cb}::default"
end

# ensure user ulimits are enabled 
include_recipe 'ulimit::default'
