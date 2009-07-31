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

package com.google.gerrit.server.ssh;

import static com.google.inject.Scopes.SINGLETON;

import com.google.gerrit.server.ssh.commands.DefaultCommandModule;
import com.google.inject.AbstractModule;

import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Configures standard dependencies for {@link GerritSshDaemon}. */
public class SshDaemonModule extends AbstractModule {
  static final Logger log = LoggerFactory.getLogger(SshDaemonModule.class);

  @Override
  protected void configure() {
    bind(Sshd.class).to(GerritSshDaemon.class).in(SINGLETON);
    bind(CommandFactory.class).toProvider(CommandFactoryProvider.class);
    bind(PublickeyAuthenticator.class).to(DatabasePubKeyAuth.class);

    install(new DefaultCommandModule());
  }
}
