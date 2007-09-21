package com.tech4d.tsm.model;

import com.tech4d.tsm.model.geometry.StyleSelector;
import com.tech4d.tsm.model.geometry.TimePrimitive;
import com.tech4d.tsm.model.geometry.TsGeometry;
import com.tech4d.tsm.model.geometry.KmlFeature;
import org.hibernate.annotations.Cascade;

import javax.persistence.*;
import java.util.List;

/**
 * Copyright 2007, Time Space Map
 *
 * This is the main class for storing events in time and space.  It contains enough information to
 * be serialized to KML and can be searched using a spatial query.
 */
@Entity
public class TsEvent extends BaseAuditableEntity implements KmlFeature {
    
    private String summary;
    private String address;
    private String snippet;
    private String description;
    private TimePrimitive timePrimitive;
    private StyleSelector styleSelector;
    private TsGeometry tsGeometry;
    private List<UserTag> userTags;
    private Votes votes;
    private List<ChangeGroup> history;
    private List<User> contributors;
    private String sourceUrl;
    private Catalog catalog;
    public enum Catalog {ENCYCLOPEDIA, ANECDOTAL, PERSONAL, CURRENT_EVENT}
    private boolean visible;
    private Flag flag;
    public enum Flag {FOR_DELETION, FOR_CONTENT}

    /**
     * @see com.tech4d.tsm.model.geometry.KmlFeature
     */
    public String getStreetAddress() {
        return address;
    }

    public void setStreetAddress(String address) {
        this.address = address;
    }

    @OneToOne(cascade = { CascadeType.ALL })
    public TsGeometry getTsGeometry() {
        return tsGeometry;
    }

    public void setTsGeometry(TsGeometry geometry) {
        this.tsGeometry = geometry;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
        this.snippet = snippet;
    }

    @ManyToOne(cascade = { CascadeType.ALL })
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    public TimePrimitive getTimePrimitive() {
        return timePrimitive;
    }

    public void setTimePrimitive(TimePrimitive timePrimative) {
        this.timePrimitive = timePrimative;
    }

    @ManyToOne(cascade = { CascadeType.REFRESH, CascadeType.MERGE })
    public StyleSelector getStyleSelector() {
        return styleSelector;
    }

    public void setStyleSelector(StyleSelector styleSelector) {
        this.styleSelector = styleSelector;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    @OneToMany(cascade={CascadeType.ALL})
    public List<ChangeGroup> getHistory() {
        return history;
    }

    public void setHistory(List<ChangeGroup> history) {
        this.history = history;
    }

    @ManyToMany(cascade={CascadeType.ALL})
    public List<UserTag> getUserTags() {
        return userTags;
    }

    public void setUserTags(List<UserTag> userTags) {
        this.userTags = userTags;
    }

    public Votes getVotes() {
        return votes;
    }

    public void setVotes(Votes votes) {
        this.votes = votes;
    }

    @ManyToMany(cascade={CascadeType.ALL})
    public List<User> getContributors() {
        return contributors;
    }

    public void setContributors(List<User> participants) {
        this.contributors = participants;
    }

    public Flag getFlag() {
        return flag;
    }

    public void setFlag(Flag flag) {
        this.flag = flag;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}