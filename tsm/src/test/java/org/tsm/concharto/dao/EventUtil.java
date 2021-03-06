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
package org.tsm.concharto.dao;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.hibernate.SessionFactory;
import org.tsm.concharto.model.Event;
import org.tsm.concharto.model.UserTag;
import org.tsm.concharto.model.geometry.TsGeometry;
import org.tsm.concharto.model.kml.Style;
import org.tsm.concharto.model.time.TimeRange;
import org.tsm.concharto.model.user.User;
import org.tsm.concharto.web.util.CatalogUtil;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class EventUtil {
    public SessionFactory sessionFactory;
    private Date begin;
    private Date end;
    List<User> users = new ArrayList<User>();
    private static Geometry defaultGeometry; 

    /**
     * Instantiate this utility with a hibernate session factory so 
     * that we can refresh objects
     * @param sessionFactory  hibernate session factory
     */
    public EventUtil(SessionFactory sessionFactory)  {
        super();
        this.sessionFactory = sessionFactory;
        Calendar cal = new GregorianCalendar(107 + 1900, 8, 22, 12, 22, 3);
        cal.set(Calendar.MILLISECOND, 750);
        begin = cal.getTime();
        cal.set(Calendar.SECOND, 35);
        end = cal.getTime();
        try {
			defaultGeometry = new WKTReader().read("POINT (20 20)");
		} catch (ParseException e) {
			e.printStackTrace();
		}
    }

    public Date getBegin() {
        return begin;
    }

    public Date getEnd() {
        return end;
    }

    public Event createEvent() throws ParseException {
        return createEvent(new Date(), new Date());
    }
    public Event createEvent(String summary) { 
    	return createEvent(defaultGeometry, new TimeRange(new Date(), new Date()),
            null, summary, "sdf");
    }

    public Event createEvent(Date begin, Date end) throws ParseException {
        return createEvent(defaultGeometry, new TimeRange(begin, end));

    }

    public Event createEvent(Geometry geometry, TimeRange timeRange) {
        return createEvent(geometry, timeRange, StyleUtil.getStyle(), null, null);
    }

    public Event createEvent(Geometry geometry, TimeRange timeRange,
            String description) {
        return createEvent(geometry, timeRange, StyleUtil.getStyle(), null, description);
    }

    public Event createEvent(Geometry geometry, TimeRange timeRange,
            String summary, String description) {
        return createEvent(geometry, timeRange, StyleUtil.getStyle(), summary, description);
    }

    public Event createEvent(Geometry geometry, TimeRange timeRange,
            org.tsm.concharto.model.kml.Style style, String summary, String description) {

//        List<User> people = new ArrayList<User>();
//        people.add(new User("Joe", "1234", "f@joe.com"));
//        people.add(new User("Mary", "1234", "m@mary.com"));

        List<UserTag> tags = new ArrayList<UserTag>();
        tags.add(new UserTag("tag a"));
        tags.add(new UserTag("tag b"));
        tags.add(new UserTag("tag b"));
        return createEvent(tags, geometry, timeRange, style, summary, description) ;
    }
    
    public Event createEvent( List<UserTag> usertags, Geometry geometry, TimeRange timeRange,
            Style style, String summary, String description) {
        Event event = new Event();
        event.setWhere("17 Mockinbird Ln, Nameless, TN, 60606");
        event.setSnippet("This is like some sort of small description yo");
        event.setSummary(summary);
        event.setDescription(description);

        TsGeometry tsPoint = new TsGeometry(geometry);
        event.setTsGeometry(tsPoint);
        event.setWhen(timeRange);
        event.setStyleSelector(style);
        event.setSource("http://www.wikipedia.com");
        event.setUserTags(usertags);
        event.setCatalog(CatalogUtil.CATALOG_WWW);
        return event;
    }
    
    public void assertEquivalent(Event expected, Event actual) {

        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getSummary(), actual.getSummary());
        assertEquals(expected.getWhen().getBegin().getDate(), actual.getWhen().getBegin().getDate());
        org.tsm.concharto.model.kml.Style expectedStyle = (Style) expected.getStyleSelector();
        org.tsm.concharto.model.kml.Style actualStyle = (Style) actual.getStyleSelector();
        assertEquals(
                expectedStyle.getBaloonStyle().getBgColor(), 
                actualStyle.getBaloonStyle().getBgColor());
        assertEquals(expected.getUserTags().size(), actual.getUserTags().size());
        
        if (expected.getFlags() != null) {
            assertEquals(expected.getFlags().size(), actual.getFlags().size());
        }

    }
    
    public static Date filterMilliseconds(Date date) {
        // NOTE: MySQL doesn't store dates with millisecond precision, so we
        // need to strip out
        // the msec in order to compare
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static void printTimeRange(TimeRange tr) {
        System.out.println("begin: " + tr.getBegin().getDate().getTime() 
            + ", end: " + tr.getEnd().getDate().getTime());
    }

    public Event createEvent(TimeRange tr) {
        return createEvent(defaultGeometry, tr);
    }


}
