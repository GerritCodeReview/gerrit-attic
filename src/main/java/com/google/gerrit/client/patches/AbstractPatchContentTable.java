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

package com.google.gerrit.client.patches;

import com.google.gerrit.client.Gerrit;
import com.google.gerrit.client.changes.PatchTable;
import com.google.gerrit.client.changes.PublishCommentScreen;
import com.google.gerrit.client.changes.Util;
import com.google.gerrit.client.data.AccountInfo;
import com.google.gerrit.client.data.AccountInfoCache;
import com.google.gerrit.client.data.PatchScript;
import com.google.gerrit.client.data.SparseFileContent;
import com.google.gerrit.client.reviewdb.Patch;
import com.google.gerrit.client.reviewdb.PatchLineComment;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.client.ui.CommentPanel;
import com.google.gerrit.client.ui.NavigationTable;
import com.google.gerrit.client.ui.NeedsSignInKeyCommand;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HTMLTable.CellFormatter;
import com.google.gwtexpui.globalkey.client.GlobalKey;
import com.google.gwtexpui.globalkey.client.KeyCommand;
import com.google.gwtexpui.globalkey.client.KeyCommandSet;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractPatchContentTable extends NavigationTable<Object> {
  protected PatchTable fileList;
  protected AccountInfoCache accountCache = AccountInfoCache.empty();
  protected Patch.Key patchKey;
  protected PatchSet.Id idSideA;
  protected PatchSet.Id idSideB;
  protected boolean onlyOneHunk;
  protected String formatLanguage;

  private final KeyCommandSet keysComment;
  private HandlerRegistration regComment;

  protected AbstractPatchContentTable() {
    keysNavigation.add(new PrevKeyCommand(0, 'k', PatchUtil.C.linePrev()));
    keysNavigation.add(new NextKeyCommand(0, 'j', PatchUtil.C.lineNext()));
    keysNavigation.add(new PrevChunkKeyCmd(0, 'p', PatchUtil.C.chunkPrev()));
    keysNavigation.add(new NextChunkKeyCmd(0, 'n', PatchUtil.C.chunkNext()));

    keysAction.add(new OpenKeyCommand(0, 'o', PatchUtil.C.expandComment()));
    keysAction.add(new OpenKeyCommand(0, KeyCodes.KEY_ENTER, PatchUtil.C
        .expandComment()));

    if (Gerrit.isSignedIn()) {
      keysAction.add(new InsertCommentCommand(0, 'c', PatchUtil.C
          .commentInsert()));
      keysAction.add(new PublishCommentsKeyCommand(0, 'r', Util.C
          .keyPublishComments()));

      // See CommentEditorPanel
      //
      keysComment = new KeyCommandSet(PatchUtil.C.commentEditorSet());
      keysComment.add(new NoOpKeyCommand(KeyCommand.M_CTRL, 's', PatchUtil.C
          .commentSaveDraft()));
      keysComment.add(new NoOpKeyCommand(KeyCommand.M_CTRL, 'd', PatchUtil.C
          .commentDiscard()));
      keysComment.add(new NoOpKeyCommand(0, KeyCodes.KEY_ESCAPE, PatchUtil.C
          .commentCancelEdit()));
    } else {
      keysComment = null;
    }

    table.setStyleName("gerrit-PatchContentTable");
  }

  void notifyDraftDelta(final int delta) {
    if (fileList != null) {
      fileList.notifyDraftDelta(patchKey, delta);
    }
  }

  @Override
  public void setRegisterKeys(final boolean on) {
    super.setRegisterKeys(on);
    if (on && keysComment != null && regComment == null) {
      regComment = GlobalKey.add(this, keysComment);
    } else if (!on && regComment != null) {
      regComment.removeHandler();
      regComment = null;
    }
  }

  public void display(final Patch.Key k, final PatchSet.Id a,
      final PatchSet.Id b, final PatchScript s) {
    patchKey = k;
    idSideA = a;
    idSideB = b;

    final String pathName = patchKey.get();
    int ext = pathName.lastIndexOf('.');
    formatLanguage = ext > 0 ? pathName.substring(ext + 1).toLowerCase() : null;

    render(s);
  }

  protected abstract void render(PatchScript script);

  protected abstract void onInsertComment(PatchLine pl);

  public abstract void display(CommentDetail comments);

  @Override
  protected MyFlexTable createFlexTable() {
    return new DoubleClickFlexTable();
  }

  @Override
  protected Object getRowItemKey(final Object item) {
    return null;
  }

  protected void initScript(final PatchScript script) {
    if (script.getEdits().size() == 1) {
      final SparseFileContent a = script.getA();
      final SparseFileContent b = script.getB();
      onlyOneHunk = a.size() == 0 || b.size() == 0;
    } else {
      onlyOneHunk = false;
    }
  }

  private boolean isChunk(final int row) {
    final Object o = getRowItem(row);
    if (!onlyOneHunk && o instanceof PatchLine) {
      final PatchLine pl = (PatchLine) o;
      switch (pl.getType()) {
        case DELETE:
        case INSERT:
        case REPLACE:
          return true;
      }
    } else if (o instanceof CommentList) {
      return true;
    }
    return false;
  }

  private int findChunkStart(int row) {
    while (0 <= row && isChunk(row)) {
      row--;
    }
    return row + 1;
  }

  private int findChunkEnd(int row) {
    final int max = table.getRowCount();
    while (row < max && isChunk(row)) {
      row++;
    }
    return row - 1;
  }

  private static int oneBefore(final int begin) {
    return 1 <= begin ? begin - 1 : begin;
  }

  private int oneAfter(final int end) {
    return end + 1 < table.getRowCount() ? end + 1 : end;
  }

  private void moveToPrevChunk(int row) {
    while (0 <= row && isChunk(row)) {
      row--;
    }
    for (; 0 <= row; row--) {
      if (isChunk(row)) {
        final int start = findChunkStart(row);
        movePointerTo(start, false);
        scrollIntoView(oneBefore(start), oneAfter(row));
        return;
      }
    }

    // No prior hunk found? Try to hit the first line in the file.
    //
    for (row = 0; row < table.getRowCount(); row++) {
      if (getRowItem(row) != null) {
        movePointerTo(row);
        break;
      }
    }
  }

  private void moveToNextChunk(int row) {
    final int max = table.getRowCount();
    while (row < max && isChunk(row)) {
      row++;
    }
    for (; row < max; row++) {
      if (isChunk(row)) {
        movePointerTo(row, false);
        scrollIntoView(oneBefore(row), oneAfter(findChunkEnd(row)));
        return;
      }
    }

    // No next hunk found? Try to hit the last line in the file.
    //
    for (row = max - 1; row >= 0; row--) {
      if (getRowItem(row) != null) {
        movePointerTo(row);
        break;
      }
    }
  }

  /** Invoked when the user clicks on a table cell. */
  protected abstract void onCellDoubleClick(int row, int column);

  /**
   * Invokes createCommentEditor() with an empty string as value for the comment
   * parent UUID. This method is invoked by callers that want to create an
   * editor for a comment that is not a reply.
   */
  protected void createCommentEditor(final int suggestRow, final int column,
      final int line, final short file) {
    createCommentEditor(suggestRow, column, line, file, null /* no parent */);
  }

  protected void createReplyEditor(final PublishedCommentPanel currentPanel) {
    final int row = rowOf(currentPanel.getElement());
    if (row >= 0) {
      final int column = columnOf(currentPanel.getElement());
      final PatchLineComment c = currentPanel.comment;
      final String uuid = c.getKey().get();
      final PatchSet.Id psId = c.getKey().getParentKey().getParentKey();
      final short file;
      if (idSideB.equals(psId)) {
        file = 1;
      } else {
        file = 0;
      }
      createCommentEditor(row, column, c.getLine(), file, uuid);
    }
  }

  private void createCommentEditor(final int suggestRow, final int column,
      final int line, final short file, final String parentUuid) {
    if (line < 1) {
      // Refuse to create an editor before the start of the file.
      //
      return;
    }

    int row = suggestRow;
    if (parentUuid != null) {
      row++;
    } else {
      int spans[] = new int[column + 1];
      OUTER: while (row < table.getRowCount()) {
        int col = 0;
        for (int cell = 0; row < table.getRowCount()
            && cell < table.getCellCount(row); cell++) {
          while (col < column && 0 < spans[col]) {
            spans[col++]--;
          }
          spans[col] = table.getFlexCellFormatter().getRowSpan(row, cell);
          if (col == column) {
            final Widget w = table.getWidget(row, cell);
            if (w instanceof CommentEditorPanel) {
              break OUTER;
            } else if (w instanceof CommentPanel) {
              row++;
            } else {
              break OUTER;
            }
          }
        }
      }
    }
    if (row < table.getRowCount() && column < table.getCellCount(row)
        && table.getWidget(row, column) instanceof CommentEditorPanel) {
      // Don't insert two editors on the same position, it doesn't make
      // any sense to the user.
      //
      ((CommentEditorPanel) table.getWidget(row, column)).setFocus(true);
      return;
    }

    if (!Gerrit.isSignedIn()) {
      Gerrit.doSignIn();
      return;
    }

    final Patch.Key parentKey;
    final short side;
    switch (file) {
      case 0:
        if (idSideA == null) {
          parentKey = new Patch.Key(idSideB, patchKey.get());
          side = (short) 0;
        } else {
          parentKey = new Patch.Key(idSideA, patchKey.get());
          side = (short) 1;
        }
        break;
      case 1:
        parentKey = new Patch.Key(idSideB, patchKey.get());
        side = (short) 1;
        break;
      default:
        throw new RuntimeException("unexpected file id " + file);
    }

    final PatchLineComment newComment =
        new PatchLineComment(new PatchLineComment.Key(parentKey, null), line,
            Gerrit.getUserAccount().getId(), parentUuid);
    newComment.setSide(side);
    newComment.setMessage("");

    final CommentEditorPanel ed = new CommentEditorPanel(newComment);
    boolean isCommentRow = false;
    boolean needInsert = false;
    if (row < table.getRowCount()) {
      for (int cell = 0; cell < table.getCellCount(row); cell++) {
        final Widget w = table.getWidget(row, cell);
        if (w instanceof CommentEditorPanel || w instanceof CommentPanel) {
          if (column == cell) {
            needInsert = true;
          }
          isCommentRow = true;
        }
      }
    }
    if (needInsert || !isCommentRow) {
      insertRow(row);
      styleCommentRow(row);
    }
    table.setWidget(row, column, ed);

    int span = 1;
    for (int r = row + 1; r < table.getRowCount(); r++) {
      boolean hasComment = false;
      for (int c = 0; c < table.getCellCount(r); c++) {
        final Widget w = table.getWidget(r, c);
        if (w instanceof CommentPanel || w instanceof CommentEditorPanel) {
          if (c != column) {
            hasComment = true;
            break;
          }
        }
      }
      if (hasComment) {
        table.removeCell(r, column);
        span++;
      } else {
        break;
      }
    }
    if (span > 1) {
      table.getFlexCellFormatter().setRowSpan(row, column, span);
    }

    for (int r = row - 1; r > 0; r--) {
      if (getRowItem(r) instanceof CommentList) {
        continue;
      } else if (getRowItem(r) != null) {
        movePointerTo(r);
        break;
      }
    }

    ed.setFocus(true);
  }

  protected void insertRow(final int row) {
    table.insertRow(row);
    table.getCellFormatter().setStyleName(row, 0, S_ICON_CELL);
  }

  @Override
  protected void onOpenRow(final int row) {
    final Object item = getRowItem(row);
    if (item instanceof CommentList) {
      for (final CommentPanel p : ((CommentList) item).panels) {
        p.setOpen(!p.isOpen());
      }
    }
  }

  public void setAccountInfoCache(final AccountInfoCache aic) {
    assert aic != null;
    accountCache = aic;
  }

  static void destroyEditor(final FlexTable table, final int row, final int col) {
    table.clearCell(row, col);
    final int span = table.getFlexCellFormatter().getRowSpan(row, col);
    boolean removeRow = true;
    final int nCells = table.getCellCount(row);
    for (int cell = 0; cell < nCells; cell++) {
      if (table.getWidget(row, cell) != null) {
        removeRow = false;
        break;
      }
    }
    if (removeRow) {
      for (int r = row - 1; 0 <= r; r--) {
        boolean data = false;
        for (int c = 0; c < table.getCellCount(r); c++) {
          data |= table.getWidget(r, c) != null;
          final int s = table.getFlexCellFormatter().getRowSpan(r, c) - 1;
          if (r + s == row) {
            table.getFlexCellFormatter().setRowSpan(r, c, s);
          }
        }
        if (!data) {
          break;
        }
      }
      table.removeRow(row);
    } else if (span != 1) {
      table.getFlexCellFormatter().setRowSpan(row, col, 1);
      for (int r = row + 1; r < row + span; r++) {
        table.insertCell(r, col + 1);
      }
    }
  }

  protected void bindComment(final int row, final int col,
      final PatchLineComment line, final boolean isLast) {
    if (line.getStatus() == PatchLineComment.Status.DRAFT) {
      final CommentEditorPanel plc = new CommentEditorPanel(line);
      table.setWidget(row, col, plc);

    } else {
      final AccountInfo author = accountCache.get(line.getAuthor());
      final PublishedCommentPanel panel =
          new PublishedCommentPanel(author, line);
      table.setWidget(row, col, panel);

      CommentList l = (CommentList) getRowItem(row);
      if (l == null) {
        l = new CommentList();
        setRowItem(row, l);
      }
      l.comments.add(line);
      l.panels.add(panel);
    }

    styleCommentRow(row);
  }

  private void styleCommentRow(final int row) {
    final CellFormatter fmt = table.getCellFormatter();
    final Element iconCell = fmt.getElement(row, 0);
    UIObject.setStyleName(DOM.getParent(iconCell), "CommentHolder", true);
  }

  protected static class CommentList {
    final List<PatchLineComment> comments = new ArrayList<PatchLineComment>();
    final List<PublishedCommentPanel> panels =
        new ArrayList<PublishedCommentPanel>();
  }

  protected class DoubleClickFlexTable extends MyFlexTable {
    public DoubleClickFlexTable() {
      sinkEvents(Event.ONDBLCLICK | Event.ONCLICK);
    }

    @Override
    public void onBrowserEvent(final Event event) {
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK: {
          // Find out which cell was actually clicked.
          final Element td = getEventTargetCell(event);
          if (td == null) {
            break;
          }
          final int row = rowOf(td);
          if (getRowItem(row) != null) {
            movePointerTo(row);
            return;
          }
          break;
        }
        case Event.ONDBLCLICK: {
          // Find out which cell was actually clicked.
          Element td = getEventTargetCell(event);
          if (td == null) {
            return;
          }
          onCellDoubleClick(rowOf(td), columnOf(td));
          return;
        }
      }
      super.onBrowserEvent(event);
    }
  }

  public static class NoOpKeyCommand extends NeedsSignInKeyCommand {
    public NoOpKeyCommand(int mask, int key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
    }
  }

  public class InsertCommentCommand extends NeedsSignInKeyCommand {
    public InsertCommentCommand(int mask, int key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      ensurePointerVisible();
      for (int row = getCurrentRow(); 0 <= row; row--) {
        final Object item = getRowItem(row);
        if (item instanceof PatchLine) {
          onInsertComment((PatchLine) item);
          return;
        } else if (item instanceof CommentList) {
          continue;
        } else {
          return;
        }
      }
    }
  }

  public class PublishCommentsKeyCommand extends NeedsSignInKeyCommand {
    public PublishCommentsKeyCommand(int mask, char key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      final PatchSet.Id id = patchKey.getParentKey();
      Gerrit.display("change,publish," + id.toString(),
          new PublishCommentScreen(id));
    }
  }

  public class PrevChunkKeyCmd extends KeyCommand {
    public PrevChunkKeyCmd(int mask, int key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      ensurePointerVisible();
      moveToPrevChunk(getCurrentRow());
    }
  }

  public class NextChunkKeyCmd extends KeyCommand {
    public NextChunkKeyCmd(int mask, int key, String help) {
      super(mask, key, help);
    }

    @Override
    public void onKeyPress(final KeyPressEvent event) {
      ensurePointerVisible();
      moveToNextChunk(getCurrentRow());
    }
  }

  private class PublishedCommentPanel extends CommentPanel implements
      ClickHandler {
    final PatchLineComment comment;

    PublishedCommentPanel(final AccountInfo author, final PatchLineComment c) {
      super(author, c.getWrittenOn(), c.getMessage());
      this.comment = c;

      final Button reply = new Button(PatchUtil.C.buttonReply());
      reply.addClickHandler(this);
      getButtonPanel().add(reply);
    }

    @Override
    public void onClick(final ClickEvent event) {
      createReplyEditor(this);
    }
  }
}
