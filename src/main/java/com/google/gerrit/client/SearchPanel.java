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

package com.google.gerrit.client;

import com.google.gerrit.client.changes.ChangeScreen;
import com.google.gerrit.client.reviewdb.Change;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwtexpui.globalkey.client.GlobalKey;
import com.google.gwtexpui.globalkey.client.KeyCommand;
import com.google.gwtexpui.globalkey.client.NpTextBox;

class SearchPanel extends Composite {
  private final NpTextBox searchBox;
  private HandlerRegistration regFocus;

  SearchPanel() {
    final FlowPanel body = new FlowPanel();
    initWidget(body);
    setStyleName("gerrit-SearchPanel");

    searchBox = new NpTextBox();
    searchBox.setVisibleLength(46);
    searchBox.setText(Gerrit.C.searchHint());
    searchBox.addStyleName("gerrit-InputFieldTypeHint");
    searchBox.addFocusHandler(new FocusHandler() {
      @Override
      public void onFocus(FocusEvent event) {
        if (Gerrit.C.searchHint().equals(searchBox.getText())) {
          searchBox.setText("");
          searchBox.removeStyleName("gerrit-InputFieldTypeHint");
        }
      }
    });
    searchBox.addBlurHandler(new BlurHandler() {
      @Override
      public void onBlur(BlurEvent event) {
        if ("".equals(searchBox.getText())) {
          searchBox.setText(Gerrit.C.searchHint());
          searchBox.addStyleName("gerrit-InputFieldTypeHint");
        }
      }
    });
    searchBox.addKeyPressHandler(new KeyPressHandler() {
      @Override
      public void onKeyPress(final KeyPressEvent event) {
        switch (event.getCharCode()) {
          case KeyCodes.KEY_ENTER:
            doSearch();
            break;
          case KeyCodes.KEY_ESCAPE:
            searchBox.setText("");
            searchBox.setFocus(false);
            break;
        }
      }
    });

    final Button searchButton = new Button(Gerrit.C.searchButton());
    searchButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        doSearch();
      }
    });

    body.add(searchBox);
    body.add(searchButton);
  }

  void setText(final String query) {
    if (query == null || query.equals("")) {
      searchBox.setText(Gerrit.C.searchHint());
      searchBox.addStyleName("gerrit-InputFieldTypeHint");
    } else {
      searchBox.setText(query);
      searchBox.removeStyleName("gerrit-InputFieldTypeHint");
    }
  }

  @Override
  protected void onLoad() {
    super.onLoad();
    if (regFocus == null) {
      regFocus =
          GlobalKey.addApplication(this, new KeyCommand(0, '/', Gerrit.C
              .keySearch()) {
            @Override
            public void onKeyPress(final KeyPressEvent event) {
              event.preventDefault();
              searchBox.setFocus(true);
              searchBox.selectAll();
            }
          });
    }
  }

  @Override
  protected void onUnload() {
    if (regFocus != null) {
      regFocus.removeHandler();
      regFocus = null;
    }
  }

  private void doSearch() {
    final String query = searchBox.getText().trim();
    if (query.length() == 0 || Gerrit.C.searchHint().equals(query)) {
      return;
    }

    searchBox.setFocus(false);

    if (query.matches("^[1-9][0-9]*$")) {
      final Change.Id ck = Change.Id.parse(query);
      Gerrit.display(Link.toChange(ck), new ChangeScreen(ck));
    } else {
      Gerrit.display(Link.toChangeQuery(query), true);
    }
  }
}
