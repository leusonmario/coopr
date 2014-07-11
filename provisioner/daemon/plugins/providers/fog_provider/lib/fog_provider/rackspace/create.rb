#!/usr/bin/env ruby
# encoding: UTF-8
#
# Copyright 2012-2014, Continuuity, Inc.
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

class FogProviderRackspaceCreate

  include FogProviderRackspace

  def run
    $stdout.sync = true

    unless image
      log.debug "Missing image value"
      exit 1
    end

    server = connection.servers.create(
      :flavor_id    => flavor,
      :image_id     => image,
      :name         => hostname,
      :config_drive => false,
      :personality  => files,
      :keypair      => rackspace_ssh_keypair
    )

    server.save

    if (server.password && !server.key_name)
      return { 'status' => 0, 'providerid' => server.id.to_s, 'rootpassword' => server.password }
    else
      return { 'status' => 0, 'providerid' => server.id.to_s }
    end

  end
end
