/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.database;

import org.projectforge.common.DatabaseDialect;
import org.projectforge.continuousdb.DatabaseUpdateDao;
import org.projectforge.continuousdb.SchemaGenerator;
import org.projectforge.continuousdb.Table;
import org.projectforge.continuousdb.TableAttribute;
import org.projectforge.continuousdb.TableAttributeType;
import org.projectforge.continuousdb.UpdateEntry;
import org.projectforge.continuousdb.UpdateEntryImpl;
import org.projectforge.continuousdb.UpdatePreCheckStatus;
import org.projectforge.continuousdb.UpdateRunningStatus;
import org.projectforge.user.PFUserDO;

import de.micromata.hibernate.history.HistoryEntry;
import de.micromata.hibernate.history.delta.PropertyDelta;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class DatabaseCoreInitial
{
  public static final String CORE_REGION_ID = "ProjectForge";

  @SuppressWarnings("serial")
  public static UpdateEntry getInitializationUpdateEntry(final MyDatabaseUpdater databaseUpdater)
  {
    final DatabaseUpdateDao dao = databaseUpdater.getDatabaseUpdateDao();

    return new UpdateEntryImpl(CORE_REGION_ID, "2014-09-02", "Adds all core tables T_*.") {

      @Override
      public UpdatePreCheckStatus runPreCheck()
      {
        // Does the data-base tables already exist?
        if (dao.doEntitiesExist(HibernateEntities.CORE_ENTITIES) == false
            || dao.doEntitiesExist(HibernateEntities.HISTORY_ENTITIES) == false) {
          return UpdatePreCheckStatus.READY_FOR_UPDATE;
        }
        return UpdatePreCheckStatus.ALREADY_UPDATED;
      }

      @Override
      public UpdateRunningStatus runUpdate()
      {
        if (dao.doExist(new Table(PFUserDO.class)) == false && HibernateUtils.getDialect() == DatabaseDialect.PostgreSQL) {
          // User table doesn't exist, therefore schema should be empty. PostgreSQL needs sequence for primary keys:
          dao.createSequence("hibernate_sequence", true);
        }
        final SchemaGenerator schemaGenerator = new SchemaGenerator(dao).add(HibernateEntities.CORE_ENTITIES).add(
            HibernateEntities.HISTORY_ENTITIES);
        final Table propertyDeltaTable = schemaGenerator.getTable(PropertyDelta.class);
        final TableAttribute attr = propertyDeltaTable.getAttributeByName("clazz");
        attr.setNullable(false).setType(TableAttributeType.VARCHAR).setLength(31); // Discriminator value is may-be not handled correctly by
        propertyDeltaTable.getAttributeByName("old_value").setLength(20000); // Increase length.
        propertyDeltaTable.getAttributeByName("new_value").setLength(20000); // Increase length.
        // continuous-db.
        final Table historyEntryTable = schemaGenerator.getTable(HistoryEntry.class);
        final TableAttribute typeAttr = historyEntryTable.getAttributeByName("type");
        typeAttr.setType(TableAttributeType.INT);
        schemaGenerator.createSchema();
        dao.createMissingIndices();

        return UpdateRunningStatus.DONE;
      }
    };
  }
}
