package org.flockdata.transform;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Created by mike on 29/07/15.
 */
public class GeoSupport {
    static final CoordinateReferenceSystem targetCrs = DefaultGeographicCRS.WGS84;

    public static double[] convert(String sourceFormat, double x, double y) throws Exception{

        CoordinateReferenceSystem sourceCrs = CRS.decode(sourceFormat);


        MathTransform mathTransform
                = CRS.findMathTransform(sourceCrs, targetCrs, true);

        DirectPosition srcDirectPosition2D
                = new DirectPosition2D(sourceCrs, x, y);

        DirectPosition destDirectPosition2D
                = new DirectPosition2D();

        return mathTransform.transform(srcDirectPosition2D, destDirectPosition2D).getCoordinate();

    }
}
