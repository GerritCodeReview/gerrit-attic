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

package com.google.gerrit.client.changes;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.Link;
import com.google.gerrit.client.data.ChangeInfo;
import com.google.gerrit.client.data.SingleListChangeInfo;
import com.google.gerrit.client.rpc.GerritCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwtorm.client.KeyUtil;



public class ChangeQueryResultsScreen extends AllSingleListScreen {
  private final String query;

  public ChangeQueryResultsScreen(final String encQuery,
      final String positionToken) {
    super("q," + encQuery, positionToken);
    query = KeyUtil.decode(encQuery);
  }

  @Override
  protected void onInitUI() {
    super.onInitUI();
    setWindowTitle(Util.M.changeQueryWindowTitle(query));
    setPageTitle(Util.M.changeQueryPageTitle(query));
  }

  @Override
  protected AsyncCallback<SingleListChangeInfo> loadCallback() {
    return new GerritCallback<SingleListChangeInfo>() {
      public final void onSuccess(final SingleListChangeInfo result) {
        if (isAttached()) {
          if (result.getChanges().size() == 1) {
            final ChangeInfo c = result.getChanges().get(0);
            Gerrit.display(Link.toChange(c), new ChangeScreen(c));
          } else {
            Gerrit.setQueryString(query);
            display(result);
            ChangeQueryResultsScreen.this.display();
          }
        }
      }
    };
  }

  @Override
  protected void loadPrev() {
    Util.LIST_SVC.allQueryPrev(query, pos, pageSize, loadCallback());
  }

  @Override
  protected void loadNext() {
    Util.LIST_SVC.allQueryNext(query, pos, pageSize, loadCallback());
  }
}
