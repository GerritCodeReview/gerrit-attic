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

package com.google.gerrit.server.rpc.patch;

import com.google.gerrit.client.reviewdb.Change;
import com.google.gerrit.client.reviewdb.PatchSetApproval;
import com.google.gerrit.client.reviewdb.ChangeMessage;
import com.google.gerrit.client.reviewdb.PatchSet;
import com.google.gerrit.client.reviewdb.ReviewDb;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.Nullable;
import com.google.gerrit.server.mail.AbandonedSender;
import com.google.gerrit.server.mail.EmailException;
import com.google.gerrit.server.project.ChangeControl;
import com.google.gerrit.server.project.NoSuchChangeException;
import com.google.gerrit.server.rpc.Handler;
import com.google.gwtjsonrpc.client.VoidResult;
import com.google.gwtorm.client.OrmException;
import com.google.gwtorm.client.OrmRunnable;
import com.google.gwtorm.client.Transaction;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.Collections;
import java.util.List;

class AbandonChange extends Handler<VoidResult> {
  interface Factory {
    AbandonChange create(PatchSet.Id patchSetId, String message);
  }

  private final ChangeControl.Factory changeControlFactory;
  private final ReviewDb db;
  private final IdentifiedUser currentUser;
  private final AbandonedSender.Factory abandonedSenderFactory;

  private final PatchSet.Id patchSetId;
  @Nullable
  private final String message;

  @Inject
  AbandonChange(final ChangeControl.Factory changeControlFactory,
      final ReviewDb db, final IdentifiedUser currentUser,
      final AbandonedSender.Factory abandonedSenderFactory,
      @Assisted final PatchSet.Id patchSetId,
      @Assisted @Nullable final String message) {
    this.changeControlFactory = changeControlFactory;
    this.db = db;
    this.currentUser = currentUser;
    this.abandonedSenderFactory = abandonedSenderFactory;

    this.patchSetId = patchSetId;
    this.message = message;
  }

  @Override
  public VoidResult call() throws NoSuchChangeException, OrmException,
      EmailException {
    final Change.Id changeId = patchSetId.getParentKey();
    final ChangeControl control = changeControlFactory.validateFor(changeId);
    if (!control.canAbandon()) {
      throw new NoSuchChangeException(changeId);
    }
    final Change change = control.getChange();
    final PatchSet patch = db.patchSets().get(patchSetId);
    if (patch == null) {
      throw new NoSuchChangeException(changeId);
    }

    final ChangeMessage cmsg =
        new ChangeMessage(new ChangeMessage.Key(changeId, ChangeUtil
            .messageUUID(db)), currentUser.getAccountId());
    final StringBuilder msgBuf =
        new StringBuilder("Patch Set " + change.currentPatchSetId().get()
            + ": Abandoned");
    if (message != null && message.length() > 0) {
      msgBuf.append("\n\n");
      msgBuf.append(message);
    }
    cmsg.setMessage(msgBuf.toString());

    Boolean dbSuccess = db.run(new OrmRunnable<Boolean, ReviewDb>() {
      public Boolean run(ReviewDb db, Transaction txn, boolean retry)
          throws OrmException {
        return doAbandonChange(message, change, patchSetId, cmsg, db, txn);
      }
    });

    if (dbSuccess) {
      // Email the reviewers
      final AbandonedSender cm = abandonedSenderFactory.create(change);
      cm.setFrom(currentUser.getAccountId());
      cm.setReviewDb(db);
      cm.setChangeMessage(cmsg);
      cm.send();
    }

    return VoidResult.INSTANCE;
  }

  private Boolean doAbandonChange(final String message, final Change change,
      final PatchSet.Id psid, final ChangeMessage cm, final ReviewDb db,
      final Transaction txn) throws OrmException {

    // Check to make sure the change status and current patchset ID haven't
    // changed while the user was typing an abandon message
    if (change.getStatus().isOpen() && change.currentPatchSetId().equals(psid)) {
      change.setStatus(Change.Status.ABANDONED);
      ChangeUtil.updated(change);

      final List<PatchSetApproval> approvals =
          db.patchSetApprovals().byChange(change.getId()).toList();
      for (PatchSetApproval a : approvals) {
        a.cache(change);
      }
      db.patchSetApprovals().update(approvals, txn);

      db.changeMessages().insert(Collections.singleton(cm), txn);
      db.changes().update(Collections.singleton(change), txn);
      return Boolean.TRUE;
    }

    return Boolean.FALSE;
  }
}
