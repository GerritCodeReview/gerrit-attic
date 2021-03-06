// Copyright (C) 2008 The Android Open Source Project
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

package com.google.gerrit.client.reviewdb;

import com.google.gwtorm.client.Column;
import com.google.gwtorm.client.StringKey;

/** Registered line of development within a {@link Project}. */
public final class Branch {
  public static final String R_HEADS = "refs/heads/";
  public static final String R_REFS = "refs/";

  /** Branch name key */
  public static class NameKey extends StringKey<Project.NameKey> {
    private static final long serialVersionUID = 1L;

    @Column
    protected Project.NameKey projectName;

    @Column
    protected String branchName;

    protected NameKey() {
      projectName = new Project.NameKey();
    }

    public NameKey(final Project.NameKey proj, final String n) {
      projectName = proj;
      branchName = n;
    }

    @Override
    public String get() {
      return branchName;
    }

    @Override
    protected void set(String newValue) {
      branchName = newValue;
    }

    @Override
    public Project.NameKey getParentKey() {
      return projectName;
    }

    public String getShortName() {
      final String n = get();

      // Git style branches will tend to start with "refs/heads/".
      //
      if (n.startsWith(R_HEADS)) {
        return n.substring(R_HEADS.length());
      }

      return n;
    }
  }

  @Column(name = Column.NONE)
  protected NameKey name;

  protected Branch() {
  }

  public Branch(final Branch.NameKey newName) {
    name = newName;
  }

  public Branch.NameKey getNameKey() {
    return name;
  }

  public String getName() {
    return name.get();
  }

  public String getShortName() {
    return name.getShortName();
  }
}
