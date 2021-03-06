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

package com.google.gerrit.client.account;

import com.google.gwt.i18n.client.Constants;

public interface AccountConstants extends Constants {
  String accountSettingsHeading();

  String fullName();
  String preferredEmail();
  String registeredOn();
  String accountId();

  String defaultContextFieldLabel();
  String maximumPageSizeFieldLabel();
  String contextWholeFile();
  String showSiteHeader();
  String useFlashClipboard();
  String buttonSaveChanges();

  String tabPreferences();
  String tabContactInformation();
  String tabSshKeys();
  String tabWebIdentities();
  String tabMyGroups();
  String tabAgreements();

  String buttonShowAddSshKey();
  String buttonCloseAddSshKey();
  String buttonDeleteSshKey();
  String buttonClearSshKeyInput();
  String buttonOpenSshKey();
  String buttonAddSshKey();

  String sshUserName();
  String buttonChangeSshUserName();

  String sshKeyInvalid();
  String sshKeyAlgorithm();
  String sshKeyKey();
  String sshKeyComment();
  String sshKeyLastUsed();
  String sshKeyStored();

  String addSshKeyPanelHeader();
  String addSshKeyHelp();
  String sshJavaAppletNotAvailable();
  String invalidSshKeyError();

  String sshHostKeyTitle();
  String sshHostKeyFingerprint();
  String sshHostKeyKnownHostEntry();

  String webIdLastUsed();
  String webIdStatus();
  String webIdEmail();
  String webIdIdentity();
  String untrustedProvider();
  String buttonDeleteIdentity();
  String buttonLinkIdentity();

  String watchedProjects();
  String buttonWatchProject();
  String defaultProjectName();
  String watchedProjectColumnEmailNotifications();
  String watchedProjectColumnNewChanges();
  String watchedProjectColumnAllComments();
  String watchedProjectColumnSubmittedChanges();

  String contactFieldFullName();
  String contactFieldEmail();
  String contactPrivacyDetailsHtml();
  String contactFieldAddress();
  String contactFieldCountry();
  String contactFieldPhone();
  String contactFieldFax();
  String buttonOpenRegisterNewEmail();
  String buttonSendRegisterNewEmail();
  String titleRegisterNewEmail();
  String descRegisterNewEmail();

  String newAgreement();
  String agreementStatus();
  String agreementName();
  String agreementDescription();
  String agreementAccepted();
  String agreementStatus_EXPIRED();
  String agreementStatus_NEW();
  String agreementStatus_REJECTED();
  String agreementStatus_VERIFIED();

  String newAgreementSelectTypeHeading();
  String newAgreementNoneAvailable();
  String newAgreementReviewLegalHeading();
  String newAgreementReviewContactHeading();
  String newAgreementCompleteHeading();
  String newAgreementIAGREE();
  String newAgreementAlreadySubmitted();
  String buttonSubmitNewAgreement();

  String welcomeToGerritCodeReview();
  String welcomeReviewContact();
  String welcomeContactFrom();
  String welcomeSshKeyHeading();
  String welcomeSshKeyText();
  String welcomeAgreementHeading();
  String welcomeAgreementText();
  String welcomeAgreementLater();
  String welcomeContinue();
}
