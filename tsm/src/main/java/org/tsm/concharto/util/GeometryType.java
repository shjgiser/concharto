/*******************************************************************************
 * Copyright 2009 Time Space Map, LLC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.tsm.concharto.util;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GeometryType {
    public static final String POINT="point";
    public static final String LINE="line";
    public static final String POLYGON="polygon";

    /**
     * Utility class to get geometry type as a short string.
     * TODO we should probably use the classname here 
     * @param geom
     * @return geometry type string 
     */
    public static String getGeometryType(Geometry geom) {
        if (geom instanceof Point) {
            return GeometryType.POINT;
        } else if (geom instanceof LineString) {
            return GeometryType.LINE;
        } else if (geom instanceof Polygon) {
            return GeometryType.POLYGON;
        } 
        return null;
    }
}
