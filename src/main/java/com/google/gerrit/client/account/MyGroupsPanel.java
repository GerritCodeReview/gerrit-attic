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

package com.google.gerrit.client.account;

import com.google.gerrit.client.admin.GroupTable;
import com.google.gerrit.client.reviewdb.AccountGroup;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import java.util.List;

class MyGroupsPanel extends Composite {
  private GroupTable groups;

  MyGroupsPanel() {
    final FlowPanel body = new FlowPanel();

    groups = new GroupTable(false /* do not hyperlink to admin */);
    body.add(groups);

    initWidget(body);
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    refresh();
  }

  private void refresh() {
    Util.ACCOUNT_SEC.myGroups(new GerritCallback<List<AccountGroup>>() {
      public void onSuccess(final List<AccountGroup> result) {
        groups.display(result);
      }
    });
  }
}
