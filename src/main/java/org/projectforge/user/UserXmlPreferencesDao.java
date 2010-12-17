/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2010 Kai Reinhard (k.reinhard@me.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.user;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;
import org.projectforge.access.AccessChecker;
import org.projectforge.task.TaskFilter;
import org.projectforge.timesheet.TimesheetPrefData;
import org.projectforge.web.scripting.RecentScriptCalls;
import org.projectforge.web.scripting.ScriptCallData;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.thoughtworks.xstream.XStream;

/**
 * Stores all user persistent objects such as filter settings, personal settings and persists them to the database.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class UserXmlPreferencesDao extends HibernateDaoSupport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(UserXmlPreferencesDao.class);

  private AccessChecker accessChecker;

  private XStream xstream;

  public UserXmlPreferencesDao()
  {
    xstream = new XStream();
    xstream.processAnnotations(new Class< ? >[] { UserXmlPreferencesMap.class, TaskFilter.class, TimesheetPrefData.class,
        ScriptCallData.class, RecentScriptCalls.class});
  }

  public void setAccessChecker(AccessChecker accessChecker)
  {
    this.accessChecker = accessChecker;
  }

  /**
   * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the given userId must be
   * the id of the context user.
   * @param userId
   */
  private UserXmlPreferencesDO getUserPreferencesByUserId(Integer userId, String key, boolean checkAccess)
  {
    if (checkAccess == true) {
      checkAccess(userId);
    }
    @SuppressWarnings("unchecked")
    final List<UserXmlPreferencesDO> list = getHibernateTemplate().find("from UserXmlPreferencesDO u where u.userId = ? and u.key = ?",
        new Object[] { userId, key});
    Validate.isTrue(list.size() <= 1);
    if (list.size() == 1) {
      return list.get(0);
    } else return null;
  }

  /**
   * Throws AccessException if the context user is not admin user and not owner of the UserXmlPreferences, meaning the given userId must be
   * the id of the context user.
   * @param userId
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<UserXmlPreferencesDO> getUserPreferencesByUserId(Integer userId)
  {
    checkAccess(userId);
    @SuppressWarnings("unchecked")
    final List<UserXmlPreferencesDO> list = getHibernateTemplate().find("from UserXmlPreferencesDO u where u.userId = ?", userId);
    return list;
  }

  /**
   * Checks if the given userIs is equals to the context user or the if the user is an admin user. If not a AccessException will be thrown.
   * @param userId
   */
  public void checkAccess(Integer userId)
  {
    Validate.notNull(userId);
    final PFUserDO user = PFUserContext.getUser();
    if (ObjectUtils.equals(userId, user.getId()) == false) {
      accessChecker.checkIsUserMemberOfAdminGroup();
    }
  }

  /**
   * Here you can update user preferences formats by manipulation the stored xml string.
   * @param userPrefs
   * @param logError
   */
  protected Object deserialize(UserXmlPreferencesDO userPrefs, boolean logError)
  {
    String xml = null;
    try {
      UserXmlPreferencesMigrationDao.migrate(userPrefs);
      xml = userPrefs.getSerializedSettings();
      final Object value = (Object) xstream.fromXML(xml);
      return value;
    } catch (final Throwable ex) {
      if (logError == true) {
        log.error("Can't deserialize user preferences: "
            + ex.getMessage()
            + " for user: "
            + userPrefs.getUserId()
            + ":"
            + userPrefs.getKey()
            + ". xml="
            + xml, ex);
      }
      return null;
    }
  }

  // REQUIRES_NEW needed for avoiding a lot of new data base connections from HibernateFilter.
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void saveOrUpdateUserEntries(final Integer userId, final UserXmlPreferencesMap data, final boolean checkAccess)
  {
    for (final Map.Entry<String, Object> prefEntry : data.getPersistentData().entrySet()) {
      final String key = prefEntry.getKey();
      if (data.isModified(key) == true) {
        saveOrUpdate(userId, key, prefEntry.getValue(), checkAccess);
        data.setModified(key, false);
      }
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.REPEATABLE_READ)
  public void saveOrUpdate(Integer userId, String key, Object entry, boolean checkAccess)
  {
    if (accessChecker.isDemoUser(userId) == true) {
      // Do nothing.
      return;
    }
    String xml = xstream.toXML(entry);
    boolean isNew = false;
    UserXmlPreferencesDO userPrefs = getUserPreferencesByUserId(userId, key, checkAccess);
    Date date = new Date();
    if (userPrefs == null) {
      isNew = true;
      userPrefs = new UserXmlPreferencesDO();
      userPrefs.setCreated(date);
      userPrefs.setUserId(userId);
      userPrefs.setKey(key);
    }
    userPrefs.setSerializedSettings(xml);
    userPrefs.setLastUpdate(date);
    userPrefs.setVersion();
    if (isNew == true) {
      if (log.isDebugEnabled() == true) {
        log.debug("Storing new user preference for user '" + userId + "': " + xml);
      }
      getHibernateTemplate().save(userPrefs);
    } else {
      if (log.isDebugEnabled() == true) {
        log.debug("Updating user preference for user '" + userPrefs.getUserId() + "': " + xml);
      }
      getHibernateTemplate().update(userPrefs);
    }
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void remove(Integer userId, String key)
  {
    if (accessChecker.isDemoUser(userId) == true) {
      // Do nothing.
      return;
    }
    final UserXmlPreferencesDO userPreferencesDO = getUserPreferencesByUserId(userId, key, true);
    if (userPreferencesDO != null) {
      getHibernateTemplate().delete(userPreferencesDO);
    }
  }
}
