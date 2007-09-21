package com.tech4d.tsm.model.geometry;

import java.util.Map;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("map")
public class StyleMap extends StyleSelector{
    private Map<String, StyleUrl> map;

    @org.hibernate.annotations.CollectionOfElements
    public Map<String, StyleUrl> getMap() {
        return map;
    }

    public void setMap(Map<String, StyleUrl> map) {
        this.map = map;
    }
    
}