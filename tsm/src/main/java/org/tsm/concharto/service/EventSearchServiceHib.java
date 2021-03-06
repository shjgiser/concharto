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
package org.tsm.concharto.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;
import org.tsm.concharto.model.Event;
import org.tsm.concharto.util.GeometryType;
import org.tsm.concharto.util.LapTimer;
import org.tsm.concharto.util.LatLngBounds;
import org.tsm.concharto.util.ProximityHelper;

import com.vividsolutions.jts.geom.Geometry;

/**
 * For searching for events.  We can't use hql or criteria queries because the
 * geo and text stuff is so specialized and the indexes only work if you have it
 * just right to get the desired indexing performance.
 * @author frank
 *
 */
@Transactional
public class EventSearchServiceHib implements EventSearchService {
    public static final String UNTAGGED = "untagged";

    private static final String PARAM_GEOM_TEXT = "geom_text";
	private static final String PARAM_SEARCH_TEXT = "search_text";
	private static final String PARAM_LATEST = "latest";
	private static final String PARAM_EARLIEST = "earliest";
	private static final String PARAM_TAG = "tag";
	private static final String PARAM_CATALOG = "catalog";
	private SessionFactory sessionFactory;
    protected final Log log = LogFactory.getLog(getClass());

    private static final String SQL_PREFIX_GET_COUNT = "SELECT count(*) "; 
    private static final String SQL_PREFIX_SEARCH = "SELECT * "; 
    private static final String SQL_SELECT_STUB = " FROM Event ev ";
     
    private static final String SQL_GEO_JOIN = "INNER JOIN TsGeometry AS g ON ev.tsgeometry_id = g.id ";
    private static final String SQL_SEARCH_JOIN = "INNER JOIN EventSearchText AS es ON ev.eventsearchtext_id = es.id ";
    private static final String SQL_TIME_JOIN = "INNER JOIN TimePrimitive AS t ON ev.when_id = t.id ";
    private static final String SUB_SQL_TAG_JOIN = " JOIN Event_UserTag AS ev_tag ON ev.id =ev_tag.Event_id "; 
    private static final String SUB_SQL_TAG_JOIN2 = " JOIN UserTag AS tag ON tag.id = ev_tag.userTags_id"; 
    private static final String SQL_TAG_JOIN = "INNER" + SUB_SQL_TAG_JOIN + "INNER" + SUB_SQL_TAG_JOIN2;
    private static final String SQL_TAG_EMPTY_JOIN = "LEFT" + SUB_SQL_TAG_JOIN + "LEFT" + SUB_SQL_TAG_JOIN2;
    private static final String SQL_WHERE = " WHERE ";
    private static final String SQL_AND = " AND ";
    
    private static final String SQL_TIMERANGE_CLAUSE = 
        "((t.begin >= :earliest AND t.begin < :latest) OR " +  
        " (t.end > :earliest AND t.end <= :latest) OR " +
        " (t.begin < :earliest AND t.end > :latest)) ";
    private static String SQL_TIMERANGE_EXCLUDE_OVERLAPS_CLAUSE = 
        " (t.begin between :earliest AND :latest) AND " +  
        " (t.end between :earliest AND :latest)";

    private static final String SQL_VISIBLE_CLAUSE = " NOT(ev.visible  <=> false) ";
    private static final String SQL_HIDDEN_CLAUSE = " ev.visible  <=> false ";
    private static final String SQL_FLAGGED_CLAUSE = " ev.hasUnresolvedFlag = true ";
    //TODO I think this will be a performance problem when the DB gets large!!
    private static final String SQL_TAG_CLAUSE = " upper(tag.tag) = upper(:tag) ";
    private static final String SQL_TAG_EMPTY_CLAUSE = " tag.tag is null";
    	
    private static final String SQL_MBRWITHIN_CLAUSE = 
        " MBRIntersects(geometryCollection, Envelope(GeomFromText(:geom_text))) ";

    private static final String SQL_MATCH_CLAUSE = 
        " MATCH (es.summary, es._where, es.usertags, es.description, es.source) AGAINST (:search_text IN BOOLEAN MODE) ";
    private static final String SQL_CATALOG_CLAUSE=" ev.catalog = :catalog ";

    /**
     * NOTE: be careful about this order by clause because it can break indexed searches
     * @see EventSearchServiceHib.sortResults()
     */
    private static final String SQL_ORDER_CLAUSE = " order by t.begin asc ";

    /*
     * (non-Javadoc)
     * 
     * @see org.tsm.concharto.service.EventSearchService#setSessionFactory(org.hibernate.SessionFactory)
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tsm.concharto.service.EventSearchService#getSessionFactory()
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.tsm.concharto.service.EventSearchService#getTotalCount()
     */
    @SuppressWarnings("unchecked")
	public Integer getTotalCount() {
    	List results = this.sessionFactory.getCurrentSession()
    	.createQuery("select count(event) from Event event")
    	.list();
    	Long count = (Long) results.get(0);
    	//cast to Integer.  It aint never going to be bigger!
    	return Math.round(count);
    }

    
    /*
     * (non-Javadoc)
     * 
     * @see org.tsm.concharto.service.EventSearchService#findById()
     */
    public Event findById(Long id) {
        return (Event) this.sessionFactory.getCurrentSession().get(
                Event.class, id);
    }

    /*
     * @see org.tsm.concharto.service.EventSearchService#getCount
     */
	public Long getCount(LatLngBounds bounds, SearchParams params) {
        Long totalResults = 0L;
		if (bounds != null) {
	    	Set<Geometry> boxes =ProximityHelper.getBoundingBoxes(bounds.getSouthWest(), bounds.getNorthEast());
	        //There are 1 or 2 bounding boxes (see comment above)
	        for (Geometry boundingBox : boxes) {
	        	totalResults += getCountInternal(boundingBox, params);
	        }
		} else {
			totalResults = getCountInternal(null, params);
		}
		return totalResults;
	}

    /*
     * @see org.tsm.concharto.service.EventSearchService#getCount
     */
    @SuppressWarnings("unchecked")
	private Long getCountInternal(Geometry boundingBox, SearchParams params) {
        LapTimer timer = new LapTimer(this.log);
        SQLQuery sqlQuery = createQuery(SQL_PREFIX_GET_COUNT, boundingBox, params);
        List result = sqlQuery.addScalar("count(*)", Hibernate.LONG).list();
        timer.timeIt("count").logInfoTime();
        return (Long) result.get(0);
    }

    /*
     * @see org.tsm.concharto.service.EventSearchService#search
     */
    public List<Event> search(int maxResults, int firstResult, 
            LatLngBounds bounds, SearchParams params) {
        List<Event> events = new ArrayList<Event>();
    	if (bounds != null) {
        	Set<Geometry> boxes = ProximityHelper.getBoundingBoxes(bounds.getSouthWest(), bounds.getNorthEast());
            //There are 1 or 2 bounding boxes (see comment above)
            for (Geometry boundingBox : boxes) {
            	if (log.isDebugEnabled()) {
            		log.debug(boundingBox.toText());
            	}
                List<Event> results = searchInternal(maxResults, firstResult, 
                		boundingBox, params);

                //to fix mysql spatial search weaknesses
                removeNonIntersectingPolys(boundingBox, results);
               
				//add what is left
				events.addAll(results);
               
            }

            //if there were two boxes, one on either side of the international date line, we
            //may have twice as many records as we need so we have to sort and then strip off
            //the excess
            if (boxes.size() > 1) {
                sortByDate(events);
                removeExcess(events, maxResults);
            }
    		
    	} else {
    		events = searchInternal(maxResults, firstResult, null, params);
    	}
        return events;
    }

    /**
     *                 
     * Mysql doesn't implement polygon/polyline searching to spec.  We only want to show 
     * objects that have at least one point in the bounding box, but mysql will return 
     * a result if the bounding box of the line intersects with the bounding box of the 
     * search, event though the points on the line are not within the bounding box of 
     * the line.  See issue TSM-323. 
	 *
     * @param bounds
     * @param events
     */
	private void removeNonIntersectingPolys(Geometry boundingBox, List<Event> events) {

		Iterator<Event> it = events.iterator(); 
		while (it.hasNext()) {
			Event event = it.next();
			Geometry geom = event.getTsGeometry().getGeometry();
			if (GeometryType.getGeometryType(geom) != GeometryType.POINT) {
				if (!(boundingBox.intersects(geom))) {
					it.remove();
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void sortByDate(List<Event> events) {
		Collections.sort(events, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				Event event0 = (Event) arg0;
				Event event1 = (Event) arg1;
				return(event0.getWhen().getBegin().getDate().compareTo(event1.getWhen().getBegin().getDate()));
			}
		  });
	}

	private void removeExcess(List<Event> events, int maxRecords) {
		int originalSize = events.size();
		for (int i=0; i<originalSize; i++) {
			if (i >= maxRecords) {
				events.remove(events.size()-1);
			}
		}
		
	}


    @SuppressWarnings("unchecked")
    private List<Event> searchInternal(int maxResults, int firstResult, 
            Geometry boundingBox, SearchParams params) {
        LapTimer timer = new LapTimer(this.log);
        SQLQuery sqlQuery = createQuery(SQL_PREFIX_SEARCH, boundingBox, params);
               
        List<Event> events = sqlQuery
            .addEntity(Event.class)
            .setMaxResults(maxResults)
            .setFirstResult(firstResult)
            .list();
        sortResults(events);
        timer.timeIt("search").logInfoTime();
        return events;
    }

    /**
     * This is somewhat of a hack because we get a performance problem otherwise (TSM-283)
     * MySQL can't index ORDER BY with an INNER JOIN with a LIMIT by.  E.g. 
     * <pre>
     * SELECT *  FROM Event ev 
     * INNER JOIN TimePrimitive AS t ON ev.when_id = t.id  
	 * WHERE  NOT(ev.visible  <=> false)  
	 * order by t.begin asc, ev.summary asc limit 22
     * </pre>  
     * 
     * This means we have to do go back to order by t.begin asc limit 22 and
     * then sort the events by hand.  The alternative is to denormalize the DB and put
     * t.begin in Event or summary in TimePrimitive.   The limit of maxResults is always
     * small so there is very little cost for this method.
     * 
     * This method sorts first by time then by summary
     * @param results unsorted list of results
     */
    private void sortResults(List<Event> events) {
		Collections.sort(events, new Comparator<Event>() {
			public int compare(Event o1, Event o2) {
				int dateCompare = o1.getWhen().getBegin().getDate().compareTo(o2.getWhen().getBegin().getDate());
				if (dateCompare != 0) {
					//dates are different
					return dateCompare;
				} else {
				    //sort by sequence
				    int sequenceCompare = 0;
				    if (( o1.getSequence() != null) && (o2.getSequence() != null)) {
	                    sequenceCompare = o1.getSequence().compareTo(o2.getSequence());
				    } 
				    if (sequenceCompare != 0) {
				        return sequenceCompare;
				    } else {
	                    //dates are the same, sort by summary
	                    return o1.getSummary().compareTo(o2.getSummary());
				    }
				}
			}
		});
		
	}

	private SQLQuery createQuery(String prefix, Geometry boundingBox, SearchParams params) {
        StringBuffer select = new StringBuffer(prefix).append(SQL_SELECT_STUB);
    	select.append(SQL_TIME_JOIN); //always join on time, so we can order by time
        StringBuffer clause = new StringBuffer();
        boolean hasConjuncted = false;
        if (params.getVisibility() == Visibility.NORMAL) {
            hasConjuncted = addClause(hasConjuncted, clause, SQL_VISIBLE_CLAUSE);
        } else if (params.getVisibility() == Visibility.HIDDEN) {
        	hasConjuncted = addClause(hasConjuncted, clause, SQL_HIDDEN_CLAUSE);
        } else if (params.getVisibility() == Visibility.FLAGGED) {
        	hasConjuncted = addClause(hasConjuncted, clause, SQL_FLAGGED_CLAUSE);
        } 
        if (!StringUtils.isEmpty(params.getTextFilter())) {
        	select.append(SQL_SEARCH_JOIN);
        	hasConjuncted = addClause(hasConjuncted, clause, SQL_MATCH_CLAUSE);
        }
        if (boundingBox != null) {
        	select.append(SQL_GEO_JOIN);
        	hasConjuncted = addClause(hasConjuncted, clause, SQL_MBRWITHIN_CLAUSE);
        }
        if (params.getTimeRange() != null) {
        	if (params.isIncludeTimeRangeOverlaps()) {
            	addClause(hasConjuncted, clause, SQL_TIMERANGE_CLAUSE);
        	} else {
            	addClause(hasConjuncted, clause, SQL_TIMERANGE_EXCLUDE_OVERLAPS_CLAUSE);
        	}
        }
        if (!StringUtils.isEmpty(params.getUserTag())) {
            //special case for empty tags
            if (!UNTAGGED.equals(params.getUserTag())) {
                select.append(SQL_TAG_JOIN);
                hasConjuncted = addClause(hasConjuncted, clause, SQL_TAG_CLAUSE);
            } else {
                select.append(SQL_TAG_EMPTY_JOIN);
                hasConjuncted = addClause(hasConjuncted, clause, SQL_TAG_EMPTY_CLAUSE);
            }
        }
        if (!StringUtils.isEmpty(params.getCatalog())) {
        	hasConjuncted = addClause(hasConjuncted, clause, SQL_CATALOG_CLAUSE);
        }
        clause.append(SQL_ORDER_CLAUSE);
        select.append(clause);

        // Note: Hibernate always uses prepared statements
        SQLQuery sqlQuery = this.sessionFactory.getCurrentSession()
                .createSQLQuery(select.toString());
        
        if (boundingBox != null) {
            sqlQuery.setString(PARAM_GEOM_TEXT, boundingBox.toText());
        }
        if (!StringUtils.isEmpty(params.getTextFilter())) {
            sqlQuery.setString(PARAM_SEARCH_TEXT, params.getTextFilter());
        }
        if (!StringUtils.isEmpty(params.getUserTag())) {
            //special case for empty tags
            if (!UNTAGGED.equals(params.getUserTag())) {
                sqlQuery.setString(PARAM_TAG, StringUtils.trim(params.getUserTag()));
            }  
        }
        if (!StringUtils.isEmpty(params.getCatalog())) {
        	sqlQuery.setString(PARAM_CATALOG, StringUtils.trim(params.getCatalog()));
        }
        if (params.getTimeRange() != null) {
            sqlQuery.setBigInteger(PARAM_EARLIEST, BigInteger.valueOf(params.getTimeRange().getBegin().getDate().getTime()));
            sqlQuery.setBigInteger(PARAM_LATEST, BigInteger.valueOf(params.getTimeRange().getEnd().getDate().getTime()));
        }
        return sqlQuery;
    }
    
    private boolean addClause(boolean hasConjuncted, StringBuffer clause, String sql) {
    	if (!hasConjuncted) {
    		hasConjuncted = true;
    		clause.append(SQL_WHERE);
    	} else {
    		clause.append(SQL_AND);
    	}
    	clause.append(sql);
    	return hasConjuncted;
    }

}
