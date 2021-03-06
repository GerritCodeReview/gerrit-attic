-- Gerrit 2 : Generic
--

-- Indexes to support @Query
--

-- *********************************************************************
-- AccountAccess
--    covers:             byPreferredEmail, suggestByPreferredEmail
CREATE INDEX accounts_byPreferredEmail
ON accounts (preferred_email);

--    covers:             suggestByFullName
CREATE INDEX accounts_byFullName
ON accounts (full_name);


-- *********************************************************************
-- AccountAgreementAccess
--    @PrimaryKey covers: byAccount


-- *********************************************************************
-- AccountExternalIdAccess
--    covers:             byAccount
CREATE INDEX account_external_ids_byAccount
ON account_external_ids (account_id);

--    covers:             byEmailAddress, suggestByEmailAddress
CREATE INDEX account_external_ids_byEmail
ON account_external_ids (email_address);


-- *********************************************************************
-- AccountGroupAccess
--    @SecondaryKey("name") covers:  all, suggestByName
CREATE INDEX account_groups_ownedByGroup
ON account_groups (owner_group_id);


-- *********************************************************************
-- AccountGroupMemberAccess
--    @PrimaryKey covers: byAccount
CREATE INDEX account_group_members_byGroup
ON account_group_members (group_id);


-- *********************************************************************
-- AccountProjectWatchAccess
--    @PrimaryKey covers: byAccount
--    covers:             notifyNewChanges
CREATE INDEX account_project_watches_ntNew
ON account_project_watches (notify_new_changes, project_name);

--    covers:             notifyAllComments
CREATE INDEX account_project_watches_ntCmt
ON account_project_watches (notify_all_comments, project_name);

--    covers:             notifySubmittedChanges
CREATE INDEX account_project_watches_ntSub
ON account_project_watches (notify_submitted_changes, project_name);


-- *********************************************************************
-- AccountSshKeyAccess
--    @PrimaryKey covers: byAccount, valid


-- *********************************************************************
-- ApprovalCategoryAccess
--    too small to bother indexing


-- *********************************************************************
-- ApprovalCategoryValueAccess
--     @PrimaryKey covers: byCategory


-- *********************************************************************
-- BranchAccess
--    @PrimaryKey covers: byProject


-- *********************************************************************
-- ChangeAccess
--    covers:             byOwnerOpen
CREATE INDEX changes_byOwnerOpen
ON changes (open, owner_account_id, created_on, change_id);

--    covers:             byOwnerClosed
CREATE INDEX changes_byOwnerClosed
ON changes (open, owner_account_id, last_updated_on);

--    covers:             submitted, allSubmitted
CREATE INDEX changes_submitted
ON changes (status, dest_project_name, dest_branch_name, last_updated_on);

--    covers:             allOpenPrev, allOpenNext
CREATE INDEX changes_allOpen
ON changes (open, sort_key);

--    covers:             byProjectOpenPrev, byProjectOpenNext
CREATE INDEX changes_byProjectOpen
ON changes (open, dest_project_name, sort_key);

--    covers:             allClosedPrev, allClosedNext
CREATE INDEX changes_allClosed
ON changes (open, status, sort_key);

CREATE INDEX changes_key
ON changes (change_key);


-- *********************************************************************
-- PatchSetApprovalAccess
--    @PrimaryKey covers: byPatchSet, byPatchSetUser
--    covers:             openByUser
CREATE INDEX patch_set_approvals_openByUser
ON patch_set_approvals (change_open, account_id);

--    covers:             closedByUser
CREATE INDEX patch_set_approvals_closedByUser
ON patch_set_approvals (change_open, account_id, change_sort_key);


-- *********************************************************************
-- ChangeMessageAccess
--    @PrimaryKey covers: byChange


-- *********************************************************************
-- ContributorAgreementAccess
--    covers:             active
CREATE INDEX contributor_agreements_active
ON contributor_agreements (active, short_name);


-- *********************************************************************
-- PatchLineCommentAccess
--    @PrimaryKey covers: published, draft
CREATE INDEX patch_comment_drafts
ON patch_comments (status, author_id);


-- *********************************************************************
-- PatchSetAncestorAccess
--    @PrimaryKey covers: ancestorsOf
--    covers:             descendantsOf
CREATE INDEX patch_set_ancestors_desc
ON patch_set_ancestors (ancestor_revision);


-- *********************************************************************
-- ProjectAccess
--    @PrimaryKey covers: all, suggestByName


-- *********************************************************************
-- ProjectRightAccess
--    @PrimaryKey covers: byProject
--    covers:             byCategoryGroup
CREATE INDEX project_rights_byCatGroup
ON project_rights (category_id, group_id);


-- *********************************************************************
-- StarredChangeAccess
--    @PrimaryKey covers: byAccount

CREATE INDEX starred_changes_byChange
ON starred_changes (change_id);
