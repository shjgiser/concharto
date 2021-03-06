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
package org.tsm.concharto.model;

import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.split;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;
import org.tsm.concharto.model.geometry.TsGeometry;
import org.tsm.concharto.model.time.TimeRange;
import org.tsm.concharto.model.wiki.WikiText;


/**
 * Copyright 2007, Concharto
 *
 * This is the main class for storing events in time and space.  It contains enough information to
 * be serialized to KML and can be searched using a spatial query.
 */
@Entity
public class Event extends BaseAuditableEntity {
    public final static int SZ_DESCRIPTION = 2048;
    public static final int SZ_SNIPPET = 1024;
    public static final int SZ_SUMMARY = 512;
    public static final int SZ_SOURCE = 1024;
    public static final int SZ_WHERE = 512;
    public static final int SZ_USERTAGS = 2048;  //this is used by validators and EventSearchText
    public static final int MAP_TYPE_MAP = 0;
    public static final int MAP_TYPE_HYBRID = 1;
    public static final int MAP_TYPE_SATELLITE = 2;
    public static final int SZ_CATALOG = 64;
    private String summary;
    private String where;
    private String snippet;
    private String description;
    private TimeRange when;
    private org.tsm.concharto.model.kml.StyleSelector styleSelector;
    private TsGeometry tsGeometry;
    private List<UserTag> userTags;
    private Votes votes;
    private String source;
    private Boolean visible;
    private Boolean hasUnresolvedFlag;
    private List<Flag> flags;
    private EventSearchText eventSearchText;
    private Integer zoomLevel;
    private Integer mapType;
    private WikiText discussion;
    private PositionalAccuracy positionalAccuracy;
    private String catalog;
    private Double sequence;
    
    @Column(name = "_where", length=SZ_WHERE)  //'where' is a sql reserved word
    public String getWhere() {
        return where;
    }

    public void setWhere(String address) {
        this.where = address;
    }

    @OneToOne(cascade = { CascadeType.ALL })
    @ForeignKey(name="FK_EVENT_GEOM")
    public TsGeometry getTsGeometry() {
        return tsGeometry;
    }

    public void setTsGeometry(TsGeometry geometry) {
        this.tsGeometry = geometry;
    }

    @Column(length=SZ_DESCRIPTION)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Column(length=SZ_SNIPPET)
    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
        this.snippet = snippet;
    }

    @ManyToOne(cascade = { CascadeType.ALL })
    @ForeignKey(name="FK_EVENT_TIMEPR")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE)
    public TimeRange getWhen() {
        return when;
    }

    public void setWhen(TimeRange timeRange) {
        this.when = timeRange;
    }

    @ManyToOne(cascade = { CascadeType.REFRESH, CascadeType.MERGE })
    @ForeignKey(name="FK_EVENT_STYLE")
    public org.tsm.concharto.model.kml.StyleSelector getStyleSelector() {
        return styleSelector;
    }

    public void setStyleSelector(org.tsm.concharto.model.kml.StyleSelector styleSelector) {
        this.styleSelector = styleSelector;
    }

    @Column(length=SZ_SUMMARY)
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    @Column(length= SZ_SOURCE)
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    //TODO this should probably be one to many!  many to many would be slow to fetch.
    @ManyToMany(cascade={CascadeType.ALL})
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE,
          org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
    @ForeignKey(name="FK_EVENT_USERTAG", inverseName = "FK_USERTAG_EVENT")
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

    @OneToMany(mappedBy="event", cascade={CascadeType.ALL})
    public List<Flag> getFlags() {
		return flags;
	}

	public void setFlags(List<Flag> flags) {
		this.flags = flags;
	}
	
	/** 
	 * Traverses the list of flags to find out whether there are any
	 * flags that have not disposition.
	 * TODO fixme.  two methods with practically the same name but different
	 * implementations...
	 * @return true if there are any flags with empty dispositions.
	 */
	@Transient
	public boolean getHasUnresolvedFlags() {
	    if (flags != null) {
    		for (Flag flag : flags) {
    			if (StringUtils.isEmpty(flag.getDisposition()) ) {
    				return true;
    			}
    		}
	    }
		return false;
	}

	public Boolean isVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getHasUnresolvedFlag() {
		return hasUnresolvedFlag;
	}

	public void setHasUnresolvedFlag(Boolean unresolvedFlag) {
		this.hasUnresolvedFlag = unresolvedFlag;
	}

	public Integer getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(Integer editZoomLevel) {
        this.zoomLevel = editZoomLevel;
    }

    public Integer getMapType() {
        return mapType;
    }

    public void setMapType(Integer mapType) {
        this.mapType = mapType;
    }

    @OneToOne (cascade = CascadeType.ALL)
    @ForeignKey(name="FK_EVENT_DISCUSS")
    public WikiText getDiscussion() {
		return discussion;
	}

	public void setDiscussion(WikiText discussion) {
		this.discussion = discussion;
	}

	/**
     * Returns a comma separated list of tags
     * @return a comma separated list of tags
     */
    @Transient
    public String getUserTagsAsString() {
        return join(userTags, ',');
    }
    
    /**
     * Populates the tag list from a comma separated list
     * @param tagList a comma separated list of tags
     */
    @Transient
    public void setUserTagsAsString(String tagList) {
        // a dirty check so we don't have to save each time
        String originalTags = getUserTagsAsString();
        boolean dirty = false;
        if (originalTags == null) {
            if (tagList != null) {
                dirty = true;
            }
        } else {
            if (!originalTags.equals(tagList)) {
                dirty = true;
            }
        }
        if (dirty) {
            String[] tags = split(tagList, ",");
            if (userTags == null) {
            	userTags = new ArrayList<UserTag>();
            } else {
            	userTags.clear();
            }
            for (String tag : tags) {
                userTags.add(new UserTag(StringUtils.trim(tag)));
            }
        }
    }

    @OneToOne(cascade = { CascadeType.ALL })
    @ForeignKey(name="FK_EVENT_EVENTSEARCHTEXT")
    public EventSearchText getEventSearchText() {
        return eventSearchText;
    }

    public void setEventSearchText(EventSearchText eventSearchText) {
        this.eventSearchText = eventSearchText;
    }

    @OneToOne (cascade = {})
    @ForeignKey(name="FK_EVENT_POSACCURACY")
	public PositionalAccuracy getPositionalAccuracy() {
		return positionalAccuracy;
	}

	public void setPositionalAccuracy(PositionalAccuracy positionalAccuracy) {
		this.positionalAccuracy = positionalAccuracy;
	}

    @Column(length=SZ_CATALOG) 
    @Index(name="catalogindex")
	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

    @Index(name="sequenceindex")
    public Double getSequence() {
        return sequence;
    }

    public void setSequence(Double sequence) {
        this.sequence = sequence;
    }
    
}