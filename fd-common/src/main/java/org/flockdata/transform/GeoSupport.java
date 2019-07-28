/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform;

import static org.slf4j.LoggerFactory.getLogger;

import org.flockdata.helper.FlockException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;

/**
 * Wraps a call to transform points to a WGS84 geo format
 *
 * @author mholdsworth
 * @since 29/07/2015
 */
public class GeoSupport {
  static final CoordinateReferenceSystem targetCrs = DefaultGeographicCRS.WGS84;
  private static final Logger logger = getLogger(GeoSupport.class);

  public static double[] convert(String sourceFormat, double x, double y) throws FlockException {

    try {
      CoordinateReferenceSystem sourceCrs = CRS.decode(sourceFormat);


      MathTransform mathTransform
          = CRS.findMathTransform(sourceCrs, targetCrs, true);

      DirectPosition srcDirectPosition2D
          = new DirectPosition2D(sourceCrs, x, y);

      DirectPosition destDirectPosition2D
          = new DirectPosition2D();

      return mathTransform.transform(srcDirectPosition2D, destDirectPosition2D).getCoordinate();
    } catch (Exception e) {
      logger.error("Geo conversion exception ", e);
      return null;
    }

  }

  public static double[] convert(GeoPayload geoPayload) throws FlockException {
    return convert(geoPayload.getSourceFormat(), geoPayload.getXValue(), geoPayload.getYValue());
  }
}
