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

import com.google.gwtorm.client.Access;
import com.google.gwtorm.client.OrmException;
import com.google.gwtorm.client.PrimaryKey;
import com.google.gwtorm.client.Query;
import com.google.gwtorm.client.ResultSet;

/** Access interface for {@link ContributorAgreement}. */
public interface ContributorAgreementAccess extends
    Access<ContributorAgreement, ContributorAgreement.Id> {
  @PrimaryKey("id")
  ContributorAgreement get(ContributorAgreement.Id key) throws OrmException;

  @Query("WHERE active = true ORDER BY shortName")
  ResultSet<ContributorAgreement> active() throws OrmException;
}
