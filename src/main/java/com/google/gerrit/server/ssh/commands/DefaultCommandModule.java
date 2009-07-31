// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.ssh.commands;

import com.google.gerrit.server.ssh.CommandModule;


/** Register the basic commands any Gerrit server should support. */
public class DefaultCommandModule extends CommandModule {
  @Override
  protected void configure() {
    command("gerrit-flush-caches").to(AdminFlushCaches.class);
    command("gerrit-ls-projects").to(ListProjects.class);
    command("gerrit-receive-pack").to(Receive.class);
    command("gerrit-replicate").to(AdminReplicate.class);
    command("gerrit-show-caches").to(AdminShowCaches.class);
    command("gerrit-show-connections").to(AdminShowConnections.class);
    command("gerrit-show-queue").to(AdminShowQueue.class);
    command("gerrit-upload-pack").to(Upload.class);
    command("scp").to(ScpCommand.class);

    alias("git-upload-pack", "gerrit-upload-pack");
    alias("git-receive-pack", "gerrit-receive-pack");
  }
}
