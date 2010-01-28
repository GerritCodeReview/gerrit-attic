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

package com.google.gerrit.server.workflow;

import com.google.gerrit.common.data.ApprovalType;
import com.google.gerrit.common.data.ApprovalTypes;
import com.google.gerrit.reviewdb.AccountGroup;
import com.google.gerrit.reviewdb.ApprovalCategory;
import com.google.gerrit.reviewdb.ApprovalCategoryValue;
import com.google.gerrit.reviewdb.Change;
import com.google.gerrit.reviewdb.PatchSet;
import com.google.gerrit.reviewdb.PatchSetApproval;
import com.google.gerrit.reviewdb.Project;
import com.google.gerrit.reviewdb.RefRight;
import com.google.gerrit.reviewdb.ApprovalCategory.Id;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupCache;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefControl;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** State passed through to a {@link CategoryFunction}. */
public class FunctionState {
  public interface Factory {
    FunctionState create(Change c, PatchSet.Id psId,
        Collection<PatchSetApproval> all);
  }

  private final ApprovalTypes approvalTypes;
  private final IdentifiedUser.GenericFactory userFactory;

  private final Map<ApprovalCategory.Id, Collection<PatchSetApproval>> approvals =
      new HashMap<ApprovalCategory.Id, Collection<PatchSetApproval>>();
  private final Map<ApprovalCategory.Id, Boolean> valid =
      new HashMap<ApprovalCategory.Id, Boolean>();
  private final Change change;
  private final ProjectState project;
  private final Map<ApprovalCategory.Id, List<RefRight>> allRights =
      new HashMap<ApprovalCategory.Id, List<RefRight>>();
  private Map<ApprovalCategory.Id, List<RefRight>> refRights;
  private Map<ApprovalCategory.Id, List<RefRight>> inheritedRights;
  private Set<PatchSetApproval> modified;

  @Inject
  FunctionState(final ApprovalTypes approvalTypes,
      final ProjectCache projectCache,
      final IdentifiedUser.GenericFactory userFactory, final GroupCache egc,
      @Assisted final Change c, @Assisted final PatchSet.Id psId,
      @Assisted final Collection<PatchSetApproval> all) {
    this.approvalTypes = approvalTypes;
    this.userFactory = userFactory;

    change = c;
    project = projectCache.get(change.getProject());

    for (final PatchSetApproval ca : all) {
      if (psId.equals(ca.getPatchSetId())) {
        Collection<PatchSetApproval> l = approvals.get(ca.getCategoryId());
        if (l == null) {
          l = new ArrayList<PatchSetApproval>();
          approvals.put(ca.getCategoryId(), l);
        }
        l.add(ca);
      }
    }
  }

  List<ApprovalType> getApprovalTypes() {
    return approvalTypes.getApprovalTypes();
  }

  public Change getChange() {
    return change;
  }

  public Project getProject() {
    return project.getProject();
  }

  public void valid(final ApprovalType at, final boolean v) {
    valid.put(id(at), v);
  }

  public boolean isValid(final ApprovalType at) {
    return isValid(id(at));
  }

  public boolean isValid(final ApprovalCategory.Id id) {
    final Boolean b = valid.get(id);
    return b != null && b;
  }

  public Collection<PatchSetApproval> getApprovals(final ApprovalType at) {
    return getApprovals(id(at));
  }

  public Collection<PatchSetApproval> getApprovals(final ApprovalCategory.Id id) {
    final Collection<PatchSetApproval> l = approvals.get(id);
    return l != null ? l : Collections.<PatchSetApproval> emptySet();
  }

  public void dirty(final PatchSetApproval ap) {
    if (modified == null) {
      modified = new HashSet<PatchSetApproval>();
    }
    modified.add(ap);
  }

  public Collection<PatchSetApproval> getDirtyChangeApprovals() {
    if (modified != null) {
      return modified;
    }
    return Collections.emptySet();
  }

  private List<RefRight> getRefRights(final ApprovalCategory.Id id) {
    if (refRights == null) {
      refRights = index(project.getLocalRights());
    }
    final List<RefRight> l = refRights.get(id);
    return l != null ? l : Collections.<RefRight> emptyList();
  }

  private List<RefRight> getWildcardRights(final ApprovalCategory.Id id) {
    if (inheritedRights == null) {
      inheritedRights = index(project.getInheritedRights());
    }
    final List<RefRight> l = inheritedRights.get(id);
    return l != null ? l : Collections.<RefRight> emptyList();
  }

  public Collection<RefRight> getAllRights(final ApprovalType at) {
    return getAllRights(id(at));
  }

  public List<RefRight> getAllRights(final ApprovalCategory.Id id) {
    List<RefRight> l = allRights.get(id);
    if (l == null) {
      l = new ArrayList<RefRight>();
      l.addAll(getRefRights(id));
      l.addAll(getWildcardRights(id));
      Collections.sort(l, RefRight.REF_PATTERN_ORDER);
      l = Collections.unmodifiableList(RefControl.filterMostSpecific(l));
      allRights.put(id, l);
    }
    return l;
  }

  private Map<Id, List<RefRight>> index(final Collection<RefRight> rights) {
    final HashMap<ApprovalCategory.Id, List<RefRight>> r;

    r = new HashMap<ApprovalCategory.Id, List<RefRight>>();
    for (final RefRight pr : rights) {
      if (RefControl.matches(change.getDest().get(), pr.getRefPattern())) {
        List<RefRight> l = r.get(pr.getApprovalCategoryId());
        if (l == null) {
          l = new ArrayList<RefRight>();
          r.put(pr.getApprovalCategoryId(), l);
        }
        l.add(pr);
      }
    }
    return r;
  }

  /**
   * Normalize the approval record down to the range permitted by the type, in
   * case the type was modified since the approval was originally granted.
   * <p>
   * If the record's value was modified, its automatically marked as dirty.
   */
  public void applyTypeFloor(final ApprovalType at, final PatchSetApproval a) {
    final ApprovalCategoryValue atMin = at.getMin();

    if (atMin != null && a.getValue() < atMin.getValue()) {
      a.setValue(atMin.getValue());
      dirty(a);
    }

    final ApprovalCategoryValue atMax = at.getMax();
    if (atMax != null && a.getValue() > atMax.getValue()) {
      a.setValue(atMax.getValue());
      dirty(a);
    }
  }

  /**
   * Normalize the approval record to be inside the maximum range permitted by
   * the RefRights granted to groups the account is a member of.
   * <p>
   * If multiple RefRights are matched (assigned to different groups the account
   * is a member of) the lowest minValue and the highest maxValue of the union
   * of them is used.
   * <p>
   * If the record's value was modified, its automatically marked as dirty.
   */
  public void applyRightFloor(final PatchSetApproval a) {
    final IdentifiedUser user = userFactory.create(a.getAccountId());

    // Find the maximal range actually granted to the user.
    //
    short minAllowed = 0, maxAllowed = 0;
    for (final RefRight r : getAllRights(a.getCategoryId())) {
      final AccountGroup.Id grp = r.getAccountGroupId();
      if (user.getEffectiveGroups().contains(grp)) {
        minAllowed = (short) Math.min(minAllowed, r.getMinValue());
        maxAllowed = (short) Math.max(maxAllowed, r.getMaxValue());
      }
    }

    // Normalize the value into that range, returning true if we changed
    // the value.
    //
    if (a.getValue() < minAllowed) {
      a.setValue(minAllowed);
      dirty(a);

    } else if (a.getValue() > maxAllowed) {
      a.setValue(maxAllowed);
      dirty(a);
    }
  }

  /** Run <code>applyTypeFloor</code>, <code>applyRightFloor</code>. */
  public void normalize(final ApprovalType at, final PatchSetApproval ca) {
    applyTypeFloor(at, ca);
    applyRightFloor(ca);
  }

  private static Id id(final ApprovalType at) {
    return at.getCategory().getId();
  }
}
