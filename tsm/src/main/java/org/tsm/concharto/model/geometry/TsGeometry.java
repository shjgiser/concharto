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
package org.tsm.concharto.model.geometry;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.tsm.concharto.model.BaseEntity;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "geometrytype", discriminatorType = DiscriminatorType.STRING)
@org.hibernate.annotations.Table(comment = "ENGINE : MyISAM", appliesTo = "TsGeometry")
public class TsGeometry extends BaseEntity {

    private GeometryCollection geometryCollection;

    public TsGeometry() {
        super();
    }

    public TsGeometry(Geometry geometry) {
        super();
        this.setGeometry(geometry);
    }

    @Type(type = "org.tsm.concharto.model.geometry.GeometryUserType")
    @Column(name = "geometryCollection", columnDefinition = "GEOMETRYCOLLECTION", nullable = false)
    private GeometryCollection getGeom() {
        return geometryCollection;
    }

    private void setGeom(GeometryCollection geom) {
        this.geometryCollection = geom;
    }

    @Transient
    public Geometry getGeometry() {
        // We assume zero or one geometries in the GeometryCollection
        if ((geometryCollection == null)
                || (geometryCollection.getNumGeometries() == 0)) {
            return null;
        } else {
            return getGeom().getGeometryN(0);
        }
    }

    public void setGeometry(Geometry geometry) {
        // We put zero or one geometries in the GeometryCollection
        Geometry[] geometries = new Geometry[1];
        geometries[0] = geometry;
        setGeom(new GeometryCollection(geometries, new GeometryFactory()));
    }

}
