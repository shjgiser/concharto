package com.tech4d.tsm.service;

import java.math.BigInteger;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.springframework.transaction.annotation.Transactional;

import com.tech4d.tsm.model.Event;
import com.tech4d.tsm.model.time.TimeRange;
import com.tech4d.tsm.util.LapTimer;
import com.vividsolutions.jts.geom.Geometry;

@Transactional
public class EventSearchServiceHib implements EventSearchService {
    private SessionFactory sessionFactory;
    /** Logger that is available to subclasses */
    protected final Log logger = LogFactory.getLog(getClass());

    private static String SQL_SELECT_STUB = " FROM Event f, TsGeometry g, EventSearchText es, TimePrimitive t "
            + "WHERE f.tsgeometry_id = g.id "
            + "AND f.eventsearchtext_id = es.id "
            + "AND f.when_id = t.id ";

    private static String SQL_TIMERANGE_CLAUSE = 
        "AND ((t.begin >= :earliest AND t.begin < :latest) OR " +  
        "     (t.end > :earliest AND t.end <= :latest) OR " +
        "     (t.begin < :earliest AND t.end > :latest)) ";

    private static String SQL_VISIBLE_CLAUSE = " AND NOT(f.visible  <=> false) ";
    private static String SQL_INVISIBLE_CLAUSE = " AND f.visible  <=> false ";
    	
    private static String SQL_MBRWITHIN_CLAUSE = 
        "AND MBRIntersects(geometryCollection, Envelope(GeomFromText(:geom_text))) ";

    private static String SQL_MATCH_CLAUSE = 
        "AND MATCH (es.summary, es._where, es.usertags, es.description, es.source) AGAINST (:search_text) ";

    private static String SQL_PREFIX_GET_COUNT = "SELECT count(*) "; 

    private static String SQL_PREFIX_SEARCH = "SELECT * "; 
    
    private static String SQL_ORDER_CLAUSE = "order by t.begin asc";

    /*
     * (non-Javadoc)
     * 
     * @see com.tech4d.tsm.service.EventSearchService#setSessionFactory(org.hibernate.SessionFactory)
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.tech4d.tsm.service.EventSearchService#getSessionFactory()
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /*
     * @see com.tech4d.tsm.service.EventSearchService#getCount
     */
    @SuppressWarnings("unchecked")
	public Long getCount(String textFilter, TimeRange timeRange, Geometry boundingBox, boolean showVisible) {
        LapTimer timer = new LapTimer(this.logger);
        SQLQuery sqlQuery = createQuery(SQL_PREFIX_GET_COUNT, textFilter, timeRange, boundingBox, showVisible);
        List result = sqlQuery.addScalar("count(*)", Hibernate.LONG).list();
        timer.timeIt("count").logDebugTime();
        return (Long) result.get(0);
    }

    /*
     * @see com.tech4d.tsm.service.EventSearchService#search
     */
    @SuppressWarnings("unchecked")
    public List<Event> search(int maxResults, int firstResult, String textFilter, TimeRange timeRange,
            Geometry boundingBox, boolean showVisible) {
        LapTimer timer = new LapTimer(this.logger);
        SQLQuery sqlQuery = createQuery(SQL_PREFIX_SEARCH, textFilter, timeRange, boundingBox, showVisible);
               
        List<Event> events = sqlQuery
            .addEntity(Event.class)
            .setMaxResults(maxResults)
            .setFirstResult(firstResult)
            .list(); 
        timer.timeIt("search").logDebugTime();
        return events;
    }

    private SQLQuery createQuery(String prefix, String textFilter, TimeRange timeRange, 
    		Geometry boundingBox, boolean showVisible) {
        StringBuffer query = new StringBuffer(prefix).append(SQL_SELECT_STUB);
        if (showVisible) {
            query.append(SQL_VISIBLE_CLAUSE);
        } else {
        	query.append(SQL_INVISIBLE_CLAUSE);
        }
        if (!StringUtils.isEmpty(textFilter)) {
        	query.append(SQL_MATCH_CLAUSE);
        }
        if (boundingBox != null) {
            query.append(SQL_MBRWITHIN_CLAUSE);
        }
        if (timeRange != null) {
            query.append(SQL_TIMERANGE_CLAUSE);
        }
        query.append(SQL_ORDER_CLAUSE);

        // Note: Hibernate always uses prepared statements
        SQLQuery sqlQuery = this.sessionFactory.getCurrentSession()
                .createSQLQuery(query.toString());
        
        if (boundingBox != null) {
            sqlQuery.setString("geom_text", boundingBox.toText());
        }
        if (!StringUtils.isEmpty(textFilter)) {
            sqlQuery.setString("search_text", textFilter);
        }
        if (timeRange != null) {
            sqlQuery.setBigInteger("earliest", BigInteger.valueOf(timeRange.getBegin().getTime()));
            sqlQuery.setBigInteger("latest", BigInteger.valueOf(timeRange.getEnd().getTime()));
        }
        return sqlQuery;
    }

}
