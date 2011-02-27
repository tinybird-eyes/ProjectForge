/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2011 Kai Reinhard (k.reinhard@me.com)
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

package org.projectforge.admin;

import static org.projectforge.admin.SystemUpdater.CORE_REGION_ID;

import java.util.ArrayList;
import java.util.List;

import org.projectforge.database.DatabaseUpdateDO;
import org.projectforge.database.DatabaseUpdateDao;
import org.projectforge.database.Table;
import org.projectforge.database.TableAttribute;
import org.projectforge.registry.Registry;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserDao;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseCoreUpdates
{
  @SuppressWarnings("serial")
  public static List<UpdateEntry> getUpdateEntries()
  {
    final List<UpdateEntry> list = new ArrayList<UpdateEntry>();
    list.add(new UpdateEntryImpl(CORE_REGION_ID, "3.5.4",
        "Adds table t_database_update. Adds attribute (excel_)date_format, hour_format_24 to table t_pf_user.") {
      final Table dbUpdateTable = new Table(DatabaseUpdateDO.class);

      final Table userTable = new Table(PFUserDO.class);

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        final DatabaseUpdateDao dao = SystemUpdater.instance().databaseUpdateDao;
        return this.preCheckStatus = dao.doesExist(dbUpdateTable) == true
            && dao.doesTableAttributesExist(userTable, "dateFormat", "excelDateFormat", "timeNotation") == true //
        ? UpdatePreCheckStatus.ALREADY_UPDATED : UpdatePreCheckStatus.OK;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        final DatabaseUpdateDao dao = SystemUpdater.instance().databaseUpdateDao;
        dbUpdateTable.addAttributes("updateDate", "regionId", "versionString", "executionResult", "executedBy", "description");
        dao.createTable(dbUpdateTable);
        dao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "dateFormat"));
        dao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "excelDateFormat"));
        dao.addTableAttributes(userTable, new TableAttribute(PFUserDO.class, "timeNotation"));
        final UserDao userDao = (UserDao) Registry.instance().getDao(UserDao.class);
        userDao.getUserGroupCache().setExpired();
        return this.runningStatus = UpdateRunningStatus.DONE;
      }
    });
    return list;
  }
}
