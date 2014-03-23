/////////////////////////////////////////////////////////////////////////////
//
// Project   ProjectForge
//
// Copyright 2001-2009, Micromata GmbH, Kai Reinhard
//           All rights reserved.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.plugins.skillmatrix;

import java.util.List;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.projectforge.core.BaseDao;
import org.projectforge.core.BaseSearchFilter;
import org.projectforge.core.QueryFilter;
import org.projectforge.user.PFUserDO;
import org.projectforge.user.UserDao;
import org.projectforge.user.UserRightId;

/**
 * This is the base data access object class. Most functionality such as access checking, select, insert, update, save, delete etc. is
 * implemented by the super class.
 * @author Werner Feder (werner.feder@t-online.de)
 */
public class AttendeeDao extends BaseDao<AttendeeDO>
{

  public static final String UNIQUE_PLUGIN_ID = "PLUGIN_SKILL_MATRIX_TRAINING_ATTENDEE";

  public static final String I18N_KEY_SKILL_PREFIX = "plugins.skillmatrix.training";

  public static final UserRightId USER_RIGHT_ID = new UserRightId(UNIQUE_PLUGIN_ID, "plugin20", I18N_KEY_SKILL_PREFIX);

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "attendee.firstname", "attendee.lastname", "attendee.username", "training.title",
    "training.skill.title", "rating", "certificate" };

  private TrainingDao trainingDao;
  private UserDao userDao;

  public AttendeeDao()
  {
    super(AttendeeDO.class);
    userRightId = USER_RIGHT_ID;
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public AttendeeDO newInstance()
  {
    return new AttendeeDO();
  }

  public TrainingDao getTraingDao()
  {
    return trainingDao;
  }

  public AttendeeDao setTraingDao(final TrainingDao trainingDao)
  {
    this.trainingDao = trainingDao;
    return this;
  }

  public UserDao getUserDao()
  {
    return userDao;
  }

  public AttendeeDao setUserDao(final UserDao userDao)
  {
    this.userDao = userDao;
    return this;
  }

  @Override
  public List<AttendeeDO> getList(final BaseSearchFilter filter)
  {
    final AttendeeFilter myFilter;
    if (filter instanceof AttendeeFilter) {
      myFilter = (AttendeeFilter) filter;
    } else {
      myFilter = new AttendeeFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    final String searchString = myFilter.getSearchString();

    if (myFilter.getAttendeeId() != null) {
      final PFUserDO attendee = new PFUserDO();
      attendee.setId(myFilter.getAttendeeId());
      queryFilter.add(Restrictions.eq("attendee", attendee));
    }
    if (myFilter.getTrainingId() != null) {
      final TrainingDO training = new TrainingDO();
      training.setId(myFilter.getTrainingId());
      queryFilter.add(Restrictions.eq("training", training));
    }
    queryFilter.addOrder(Order.desc("created"));
    final List<AttendeeDO> list = getList(queryFilter);
    myFilter.setSearchString(searchString); // Restore search string.
    return list;
  }
}
