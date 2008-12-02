// Copyright 2008 Google Inc.
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

package com.google.gerrit.client.data;

import com.google.gerrit.client.reviewdb.Change;
import com.google.gwtorm.client.OrmException;

public class ChangeInfo {
  protected Change.Id id;
  protected String subject;
  protected Change.Status status;
  protected AccountInfo owner;
  protected ProjectInfo project;
  protected boolean starred;

  protected ChangeInfo() {
  }

  public ChangeInfo(final Change c, final AccountCache accounts)
      throws OrmException {
    id = c.getKey();
    subject = c.getSubject();
    status = c.getStatus();
    owner = new AccountInfo(accounts.get(c.getOwner()));
    project = new ProjectInfo(c.getDest().getParentKey());
  }

  public Change.Id getId() {
    return id;
  }

  public String getSubject() {
    return subject;
  }

  public Change.Status getStatus() {
    return status;
  }

  public AccountInfo getOwner() {
    return owner;
  }

  public ProjectInfo getProject() {
    return project;
  }

  public boolean isStarred() {
    return starred;
  }

  public void setStarred(final boolean s) {
    starred = s;
  }
}