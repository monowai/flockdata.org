/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.transform;

import org.flockdata.helper.FlockException;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Wraps a call to transform points to a WGS84 geo format
 *
 * Created by mike on 29/07/15.
 */
public class GeoSupport {
    private static final Logger logger = getLogger(GeoSupport.class);
    static final CoordinateReferenceSystem targetCrs = DefaultGeographicCRS.WGS84;

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
        } catch ( Exception e ){
            logger.error("Geo conversion exception ", e);
            return null;
        }

    }

    public static double[] convert(GeoPayload geoPayload) throws FlockException {
        return convert(geoPayload.getSourceFormat(), geoPayload.getXValue(), geoPayload.getYValue());
    }
}
