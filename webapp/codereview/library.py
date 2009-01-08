# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""Django template library for Gerrit."""

import cgi
import logging

from google.appengine.api import mail
from google.appengine.api import users
from google.appengine.ext import db

import django.template
import django.utils.safestring
from django.utils.safestring import mark_safe
from django.utils.timesince import timesince
from django.template import defaultfilters

from memcache import CachedDict

import email
import models
import view_util

register = django.template.Library()

class _CachedUser(object):
  """Important data about an Account, pickled into memcache for
     faster access when rendering pages to clients.
  """
  email = None
  real_name = None
  exists = False

def _store_users(emails):
  def _cache(pair):
    email, account = pair
    r = _CachedUser()
    if account:
      r.email = account.email
      r.real_name = account.real_name
      r.exists = account.real_name_entered
    else:
      r.email = email
      r.real_name = email
      if '@' in r.real_name:
        r.real_name = r.real_name.split('@', 1)[0]
    if r.real_name is None:
      r.real_name = 'Unknown Person (%s)' % r.email
    return r
  all = zip(emails, models.Account.get_accounts_for_emails(emails))
  return map(_cache, all)
_user_cache = CachedDict(prefix = 'CachedUser:',
                         compute_multi = _store_users,
                         timeout = 300)

def _to_email(u):
  if isinstance(u, users.User):
    return u.email()
  return u

def prefetch_names(emails):
  _user_cache.prefetch(map(_to_email, emails))


@register.filter
def real_name(email, arg=None):
  """Render an email address or a User object as a real_name.

  If the input is a user object that equals the current user,
  'me' is returned, unless the filter argument is non-empty.
  Example:
    {{foo|real_name}} may render 'me';
    {{foo|real_name:"x"}} will never render 'me'.
  """
  return real_names([email], arg)

@register.filter
def real_names(email_list, arg=None):
  """Render a list of email addresses or User objects as real_names.

  Each list item is first formatter via the real_name() filter above,
  and then the resulting strings are separated by commas.
  The filter argument is the same as for real_name() above.
  """
  if arg:
    user = None
  else:
    user = users.get_current_user()

  email_list = map(_to_email, email_list)
  all = _user_cache.get_multi(email_list)

  names = []
  for email in email_list:
    if user and user.email() == email:
      names.append('me')
    else:
      names.append(all[email].real_name)
  return ', '.join(names)


@register.filter
def show_user(email, arg=None):
  """Render a link to the user's dashboard, with text being
     the real_name.
  """
  return show_users([email], arg)

@register.filter
def show_users(email_list, arg=None):
  """Render list of links to each user's dashboard, with text
     being the real_name.
  """
  if arg:
    user = None
  else:
    user = users.get_current_user()

  email_list = map(_to_email, email_list)
  all = _user_cache.get_multi(email_list)

  names = []
  for email in email_list:
    if user and user.email() == email:
      names.append('me')
    else:
      u = all[email]
      if u.exists:
        names.append(
          '<a href="/user/%(link)s"'
          ' onMouseOver="M_showUserInfoPopup(this)">'
          '%(name)s</a>'
          % {'link': cgi.escape(u.email.replace('@',',,')),
             'name': cgi.escape(u.real_name)}
        )
      else:
        names.append(cgi.escape(u.real_name))
  return mark_safe(', '.join(names))


def _init_lgtm_text():
  r = {}
  for key, value in models.LGTM_CHOICES:
    r[key] = value
  return r
_lgtm_text = _init_lgtm_text()

@register.filter
def review_status_text(status, arg=None):
  try:
    return _lgtm_text[status]
  except KeyError:
    return ''


_lgtm_icon = {
  'lgtm':    mark_safe('<img src="/static/check.png" />'),
  'yes':     mark_safe('<font color="#08a400">+1</font>'),
  'abstain': '',
  'no':      mark_safe('<font color="#d10000"><b>-1</b></font>'),
  'reject':  mark_safe('<img src="/static/x.png" />'),
}

@register.filter
def review_status_icons(status, arg=None):
  try:
    return _lgtm_icon[status]
  except KeyError:
    return ''


@register.filter
def form_xsrf(url, arg=None):
  x = view_util.xsrf_for(url)
  return mark_safe('<input type="hidden" name="xsrf" value="%s" />' % x)

@register.filter
def bare_xsrf(url, arg=None):
  return mark_safe(view_util.xsrf_for(url))

_abbrev_units = {
  'year': 'y',     'years': 'y',
  'month': 'mo',   'months': 'm',
  'week': 'w',     'weeks': 'w',
  'day': 'd',      'days': 'd',
  'hour': 'h',     'hours': 'h',
  'minute': 'min', 'minutes': 'mins',
}

@register.filter
def abbrevtimesince(d, arg=None):
  r = []
  for p in timesince(d).split(', '):
    cnt, unit = p.split(' ', 2)
    try:
      r.append('%s %s' % (cnt, _abbrev_units[unit]))
    except KeyError:
      r.append(p)
  return ', '.join(r)


def change_url(change):
  base = models.Settings.get_settings().canonical_url
  return "%s/%s" % (base, change.key().id())

@register.filter
def closed_label(change, arg=None):
  if change.closed:
    if change.merged:
      return '(Merged)'
    else:
      return '(Abandoned)'
  else:
    return ''

@register.filter
def patchset_browse_url(patchset, arg=None):
  pattern = models.Settings.get_settings().source_browser_url
  return pattern % {
            'id': patchset.revision_hash(),
            'project': patchset.change.dest_project.name,
            }

@register.filter
def file_leaf(filename, arg=None):
  parts = filename.rsplit('/', 1)
  return parts[-1]

def _find_review_status_for_user(review_statuses, user):
  for rs in review_statuses:
    if rs.user == user:
      return rs
  return None

def update_reviewers(change, old_review_status, new_users):
  """Update the reviewers for a change

  ****
  Should be called in a transaction.  You need to call put()
  for each of the ReviewStatus objects in added_review_status and
  delete() for each of the ReviewStatus objects in deleted_review_status.
  ****

  Returns:
    A tuple of ReviewStatus objects of:
      0 - the new ones that were added
      1 - the ones that were deleted
      2 - the new set of review status objects
  """
  new_review_status = []
  added_review_status = []
  deleted_review_status = []
  for u in new_users:
    rs = _find_review_status_for_user(old_review_status, u)
    if not rs:
      rs = models.ReviewStatus.insert_status(change, u)
      rs.lgtm = 'abstain'
      rs.verified = False
      added_review_status.append(rs)
    new_review_status.append(rs)
  for rs in old_review_status:
    if rs not in new_review_status:
      deleted_review_status.append(rs)
  change.set_reviewers([db.Email(rs.user.email()) for rs
                        in new_review_status])

  return (added_review_status, deleted_review_status, new_review_status)

def send_new_change_emails(change, sender_user, to_users, cc_emails,
                            additional_message = ""):
  if to_users or cc_emails:
    sender_account = models.Account.get_account_for_user(sender_user)
    sender_string = email.get_default_sender()
    to_strings = email.make_to_strings(set(to_users + [sender_user]))
    cc_strings = email.make_to_strings(cc_emails)
    subject = email.make_change_subject(change)
    message_body = make_please_review_message(change, sender_account) + '\n' \
          + additional_message

    email_message = mail.EmailMessage(sender=sender_string,
                   subject=subject,
                   body=message_body)
    if to_strings:
      email_message.to = to_strings
    if cc_strings:
      email_message.cc = cc_strings
    email_message.send()

def make_please_review_message(change, account):
  description = defaultfilters.wordwrap(change.description, 70)
  description = '  ' + description.replace('\n', '\n  ')
  sentence = defaultfilters.wordwrap(
      "%(account)s has asked that you review this change." % {
        'account': account.get_email_formatted(),
      }, 70)
  return """Hi,

%(sentence)s

    %(url)s

Thanks.

The commit message for this change is:
%(description)s
""" % { 'url': change_url(change),
        'sentence': sentence,
        'description': description,
      }

