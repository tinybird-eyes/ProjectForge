/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2013 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.plugins.liquidityplanning;

import java.io.Serializable;
import java.math.BigDecimal;

public class LiquidityAnalysisSettings implements Serializable
{
  private static final long serialVersionUID = -6429410479048275707L;

  private BigDecimal startAmount = BigDecimal.ZERO;

  /**
   * @return the startAmount if given or {@link BigDecimal#ZERO}.
   */
  public BigDecimal getStartAmount()
  {
    if (startAmount == null) {
      return BigDecimal.ZERO;
    }
    return startAmount;
  }

  /**
   * @param startAmount the startAmount to set
   * @return this for chaining.
   */
  public LiquidityAnalysisSettings setStartAmount(final BigDecimal startAmount)
  {
    this.startAmount = startAmount;
    return this;
  }
}
