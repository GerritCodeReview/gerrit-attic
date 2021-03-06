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

package com.google.gerrit.pgm;

import com.google.gerrit.server.cache.CachePool;
import com.google.gerrit.server.config.GerritGlobalModule;
import com.google.gerrit.server.ssh.SshDaemon;
import com.google.gerrit.server.ssh.SshModule;
import com.google.inject.Injector;

/** Run only the SSH daemon portions of Gerrit. */
public class Daemon extends AbstractProgram {
  @Override
  public int run() throws Exception {
    Injector sysInjector = GerritGlobalModule.createInjector();
    Injector sshInjector = sysInjector.createChildInjector(new SshModule());
    sysInjector.getInstance(CachePool.class).start();
    sshInjector.getInstance(SshDaemon.class).start();
    return never();
  }
}
