/*
 * Copyright © 2012-2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.cask.coopr.shell.command;

import co.cask.common.cli.Arguments;
import co.cask.common.cli.Command;
import co.cask.coopr.client.PluginClient;
import com.google.inject.Inject;

import java.io.PrintStream;

/**
 * Synchronize plugins
 */
public class SyncPluginCommand implements Command {

  private final PluginClient pluginClient;

  @Inject
  public SyncPluginCommand(PluginClient pluginClient) {
    this.pluginClient = pluginClient;
  }

  @Override
  public void execute(Arguments arguments, PrintStream printStream) throws Exception {
    pluginClient.syncPlugins();
  }

  @Override
  public String getPattern() {
    return String.format("sync plugins");
  }

  @Override
  public String getDescription() {
    return "Synchronize plugins";
  }
}