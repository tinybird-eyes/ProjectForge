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

package org.projectforge.fibu.kost.reporting;

import java.io.InputStream;
import java.util.List;

import org.projectforge.access.AccessChecker;
import org.projectforge.fibu.kost.BuchungssatzDO;
import org.projectforge.fibu.kost.BuchungssatzDao;
import org.projectforge.fibu.kost.BuchungssatzFilter;
import org.projectforge.user.ProjectForgeGroup;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.Annotations;


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
public class ReportDao extends HibernateDaoSupport
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ReportDao.class);

  private XStream xstream;

  private AccessChecker accessChecker;

  private BuchungssatzDao buchungssatzDao;

  public ReportDao()
  {
    xstream = new XStream();
    Annotations.configureAliases(xstream, ReportObjective.class);
  }

  /**
   * Erzeugt einen Report ohne Zeitraumangabe. Es wird lediglich das ReportObjective als xml vom InputStream deserialisiert und dem
   * erzeugtem Report zugewiesen.
   * @param reportObjectiveAsXml ReportObjective als XML.
   * @see #deserializeFromXML(InputStream)
   */
  public Report createReport(InputStream reportObjectiveAsXml)
  {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    ReportObjective reportObjective = deserializeFromXML(reportObjectiveAsXml);
    Report report = new Report(reportObjective);
    return report;
  }
  
  /**
   * Only for test cases.
   * @param reportObjectiveAsXml
   * @return
   */
  public Report createReport(String reportObjectiveAsXml) {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    ReportObjective reportObjective = deserializeFromXML(reportObjectiveAsXml);
    Report report = new Report(reportObjective);
    return report;
  }

  /**
   * Zeitraum muss gegeben sein. Liest die Buchungssätze anhand des Zeitraums aus der Datenbank und selektiert anhand des ReportObjectives
   * die zu verwendenden Buchungssätze.
   * @param report
   * @see BuchungssatzDao#getList(org.projectforge.core.BaseSearchFilter)
   * @see Report#select(List)
   */
  public void loadReport(Report report)
  {
    accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP);
    final BuchungssatzFilter filter = new BuchungssatzFilter();
    filter.setFromYear(report.getFromYear());
    filter.setFromMonth(report.getFromMonth());
    filter.setToYear(report.getToYear());
    filter.setToMonth(report.getToMonth());
    final List<BuchungssatzDO> list = buchungssatzDao.getList(filter);
    report.select(list);
  }

  public ReportObjective deserializeFromXML(String xml)
  {
    try {
      ReportObjective reportObjective = (ReportObjective) xstream.fromXML(xml);
      return reportObjective;
    } catch (Throwable ex) {
      log.error("Can't deserialize report: " + ex.getMessage(), ex);
      return null;
    }
  }

  public ReportObjective deserializeFromXML(InputStream is)
  {
    try {
      ReportObjective reportObjective = (ReportObjective) xstream.fromXML(is);
      return reportObjective;
    } catch (Throwable ex) {
      log.error("Can't deserialize report: " + ex.getMessage(), ex);
      return null;
    }
  }

  public String serializeToXML(ReportObjective report)
  {
    String xml = xstream.toXML(report);
    return xml;
  }

  public void setAccessChecker(AccessChecker accessChecker)
  {
    this.accessChecker = accessChecker;
  }

  public void setBuchungssatzDao(BuchungssatzDao buchungssatzDao)
  {
    this.buchungssatzDao = buchungssatzDao;
  }
}
