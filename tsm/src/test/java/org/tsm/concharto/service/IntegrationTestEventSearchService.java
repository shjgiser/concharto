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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.tsm.concharto.dao.EventDao;
import org.tsm.concharto.dao.EventTesterDao;
import org.tsm.concharto.dao.EventUtil;
import org.tsm.concharto.dao.StyleUtil;
import org.tsm.concharto.model.Event;
import org.tsm.concharto.model.time.TimeRange;
import org.tsm.concharto.service.EventSearchService;
import org.tsm.concharto.service.SearchParams;
import org.tsm.concharto.service.Visibility;
import org.tsm.concharto.util.ContextUtil;
import org.tsm.concharto.util.LatLngBounds;
import org.tsm.concharto.util.TimeRangeFormat;
import org.tsm.concharto.web.util.CatalogUtil;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class IntegrationTestEventSearchService {
    private static final int BOUNDS_SIZE = 100;

	private static EventSearchService eventSearchService;

    private static EventDao eventDao;

    private static EventUtil eventUtil;
    private static LatLngBounds searchBox = makeBoundingRectangle(300, 300);
    //Feb 22, 2005 - Feb 22, 2007
    private static TimeRange searchTimeRange = new TimeRange(makeDate(2005, 2, 22), makeDate(2007, 2, 22));
    private static LatLngBounds failBox = makeBoundingRectangle(3000, 3000);
    //Feb 22, 1005 - Feb 22, 1007
    private static TimeRange failTimeRange = new TimeRange(makeDate(1005, 2, 22), makeDate(1007, 2, 22));
    private static String[] failStrings = { "the a is", "is", "sdfgsdfg" };
    private static Geometry insideTheBox;
    //TODO refactor searchStrings and matches into one class
    private static String[] searchStrings = { 
        "description problem", 
        "description", 
        "small hand", 
        "was harassed by impatient",
        "battle", 
        "japanese" 
        };
    private static int[] matches = {
        3, 
        3, 
        3, 
        1, 
        3, 
        1};
    //TODO refactor shouldMatchDescription and shouldMatchSummary into one class
    //NOTE testing the full text search on a sparsely populated database sometimes results
    //in unexpected answers because of the complex filtering logic.  It behaves much more
    //reasonably when there are a lot of words.  Even the amount below really isn't enough
    //for normal usage, but for this test it is ok.
    private static String[] shouldMatchDescription = { 
        "this is a small description of the problem at hand",
        "description of this is a small the problem at hand",
        "The First Battle of Bull Run (named after the closest",
        "creek), also known as the First Battle of Manassas (named " ,
        "after the closest town), took place on July 21, 1861, and " ,
        "was the first major land  of the American Civil War. " ,
        "Unseasoned Union Army troops under Brig. Gen. Irvin McDowell advanced against the Confederate " ,
        "Army under Brig. Gens. Joseph E. Johnston and P.G.T. Beauregard at Manassas, Virginia, and " ,
        "despite the Union's early successes, they were routed and forced to retreat back to Washington, " ,
        "D.C. with capital T; Dutch: Image:Ltspkr.pngDen Haag, officially also Image:Ltspkr." ,
        "png's-Gravenhage (literally The Count's Hedge)) is the third-largest city in the Netherlands " ,
        "after Amsterdam and Rotterdam, with a population of 475,580 (as of January 1, 2006) " ,
        "(population of agglomeration: 600.000) and an area of approximately 100 km. It is " ,
        "located in the west of the country, in the province of South Holland, of which it is " ,
        "also the provincial capital. The Hague is like Amsterdam, Rotterdam and Utrecht, part " 
        };
    private static String[] shouldMatchSummary = { 
        "Battle of la la land Holland, where he intended to live after his coronation. ", 
        "www.wikipedia.com japanese the  He died in  before he could be crowned. His castle was not finished," ,
        "Brig. Gen. Irvin McDowell was appointed " ,
        "by President Abraham Lincoln to command " ,
        "the Army of Northeastern Virginia. Once in " ,
        "this capacity, McDowell was harassed by impatient " ,
        "politicians and citizens in Washington, who wished ",
        "of the conglomerate metropolitan area Randstad, with a population of 6,659,300 inhabitants." ,
        "The Hague is the actual seat of government, but, somewhat anomalously, not the official " ,
        "capital of the Netherlands, a role set aside by the Dutch constitution for Amsterdam. " ,
        "The Hague is the home of the Eerste Kamer (first chamber) and the Tweede Kamer (second " ,
        "chamber), respectively the upper and lower houses forming the Staten Generaal (literally " ,
        "the Estates-General). Queen Beatrix of the Netherlands lives and works in The Hague. " ,
        "All foreign embassies and government ministries are located in the city, as" ,
        " well as the Hoge Raad der Nederlanden (The Supreme Court), the Raad van State " ,
        "(Council of State) and many lobbying organisations."
        };
    private static String shouldNotMatch = "there is nothing to match here";
    //Feb 22, 2006 - Sept 22, 2006
    private static TimeRange insideTimeRange = new TimeRange(makeDate(2006, 2, 22), makeDate(2006, 9, 22));
    //Feb 22, 2000 - Feb 22, 2007
    private static TimeRange halfwayOutsideTimeRange 
                    = new TimeRange(makeDate(2000, 2, 22), makeDate(2007, 2, 22));
    //Feb 22, 2000 - Feb 22, 2002
    private static TimeRange outsideTimeRange = new TimeRange(makeDate(2000, 2, 22), makeDate(2002, 2, 22));
    private static Event actual;

    // search parameters
    private static int MAX_RESULTS = 3;

    @BeforeClass
    public static void setUpBigSearch() throws ParseException {
        ApplicationContext appCtx = ContextUtil.getCtx();
        eventSearchService = (EventSearchService) appCtx.getBean("eventSearchService");
        eventDao = (EventDao) appCtx.getBean("eventDao");
        EventTesterDao eventTesterDao = (EventTesterDao) appCtx.getBean("eventTesterDao");
        eventUtil = new EventUtil(eventSearchService.getSessionFactory());
        eventTesterDao.deleteAll();
        StyleUtil.setupStyle();

        insideTheBox = new WKTReader().read("POINT (330 330)");
        Geometry outsideTheBox = new WKTReader().read("POINT (130 130)");

        // sample data
        // these three pass with all parameters
        for (int i=0; i< shouldMatchDescription.length; i++) {
            actual = makeSearchEvent(insideTheBox, insideTimeRange, shouldMatchSummary[i], shouldMatchDescription[i]);
        }
        makeSearchEvent(insideTheBox, halfwayOutsideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);

        // the rest have at least one thing out of bounds
        makeSearchEvent(outsideTheBox, insideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);
        makeSearchEvent(insideTheBox, outsideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);
        makeSearchEvent(insideTheBox, insideTimeRange, shouldNotMatch, shouldNotMatch);
        makeSearchEvent(outsideTheBox, insideTimeRange, shouldNotMatch, shouldNotMatch);
        makeSearchEvent(outsideTheBox, outsideTimeRange, shouldNotMatch, shouldNotMatch);
    }

    @Test
    public void testSearchReturnsSome() throws ParseException {

        for (int i=0; i<searchStrings.length; i++) {
            String searchString = searchStrings[i];
            List<Event> events = eventSearchService.search(MAX_RESULTS, 0, searchBox, new SearchParams(searchString,
                    searchTimeRange, Visibility.NORMAL, true, null, CatalogUtil.CATALOG_WWW));
            for (Event event : events) {
                System.out.println(event.getDescription());
            }
            assertEquals("index " + i + " matches against '" + searchString + "'", matches[i], events.size());
            Event returned = events.get(0);
            assertEquals(insideTheBox.toText(), (returned.getTsGeometry()).getGeometry().toText());
            //it should be one of the description strings
            boolean failDescriptionMatch = true;
            for (String descr : shouldMatchDescription) {
                if (descr.equals(returned.getDescription())) {
                    failDescriptionMatch = false;
                }
            }
            if (failDescriptionMatch) {
                fail("descriptions didn't match");
            }
            assertWithin(insideTimeRange, returned.getWhen());
            // For one of them, make sure we can get everything
            for (Event event : events) {
                if (event.getId() == actual.getId()) {
                    eventUtil.assertEquivalent(actual, event);
                }
            }
        }
    }
    
 
    /**
     * Given: Event = 1900 - 2000 and Search Range = 1920, we should return the event
     * because the event spans 1920. 
     * @throws java.text.ParseException  e
     * 
     */
    @Test public void testInclusiveTimeRange() throws java.text.ParseException {
        TimeRange timeRange = TimeRangeFormat.parse("Jan 1, 2007"); 
        EventUtil.printTimeRange(searchTimeRange);
        EventUtil.printTimeRange(timeRange);
        EventUtil.printTimeRange(TimeRangeFormat.parse("Jan 1, 1007"));
        List<Event> events = eventSearchService.search(MAX_RESULTS, 0, null, new SearchParams(null,
                timeRange, Visibility.NORMAL, true, null, CatalogUtil.CATALOG_WWW));
        assertEquals(1, events.size());
    }
    
    @Test public void noneInBox() {
        // now search in a bounding box that is out
        assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS, 0, failBox,  
                new SearchParams(searchStrings[0], searchTimeRange, Visibility.NORMAL, true, null, 
                		CatalogUtil.CATALOG_WWW)).size());
    }

    @Test public void noTextMach() {
        // now search strings that are don't count words
        for (String failString : failStrings) {
            assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS, 0, searchBox, new SearchParams(failString,
                    searchTimeRange, Visibility.NORMAL, true, null, CatalogUtil.CATALOG_WWW)).size());
        }
    }
    
    @Test public void noTimeRangeMatch() {
        // now search timeframes that are out
        assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS, 0, searchBox, 
                new SearchParams(searchStrings[0], failTimeRange, Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)).size());
        
    }
    
    @Test public void checkMaxReturn() {
        // now set the max return threshold to below the number of possible results
        assertEquals("only one should match", 1, eventSearchService.search(1, 0, searchBox, 
                new SearchParams(searchStrings[0], searchTimeRange,  Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)).size());
    }
    
    @Test public void checkNullSearchText() {
        assertEquals("three should match", 3, eventSearchService.search(MAX_RESULTS, 0, searchBox, 
                new SearchParams(null, searchTimeRange, Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)).size());
    }

    @Test public void checkCount() {
        assertEquals(3, eventSearchService.search(MAX_RESULTS, 0, searchBox,
                new SearchParams(searchStrings[0], searchTimeRange,  Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)).size());
        assertEquals(3L, (long) eventSearchService.getCount(searchBox,
                new SearchParams(searchStrings[0], searchTimeRange,  Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)));
    }

    @Test public void checkCountPartial() {
        assertEquals(5, eventSearchService.search(10, 0, null,  
                new SearchParams(searchStrings[0], null, Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)).size());
        assertEquals(5L, (long) eventSearchService.getCount(null,
                new SearchParams(searchStrings[0], null, Visibility.NORMAL, true, null,
                		CatalogUtil.CATALOG_WWW)));
    }

    private static LatLngBounds makeBoundingRectangle(int x, int y) {
        GeometryFactory gf = new GeometryFactory();
        LatLngBounds bounds = new LatLngBounds();
        bounds.setSouthWest(gf.createPoint(new Coordinate(x,y)));
        bounds.setNorthEast(gf.createPoint(new Coordinate(x+BOUNDS_SIZE,y+BOUNDS_SIZE)));
        return bounds;
    }

    private static Event makeSearchEvent(Geometry geometry, TimeRange timeRange, String summary, String description) {
        Event event = eventUtil.createEvent(geometry, timeRange, summary, description);
        eventDao.save(event);
        return event;
    }

    private static Date makeDate(int year, int month, int day) {
        return new GregorianCalendar(year, month, day).getTime();
    }
    
    /**
     * Verify the timerange is within expected 
     * @param searchTr the search 
     * @param evTr the one to verify
     */
    private void assertWithin(TimeRange searchTr, TimeRange evTr) {
        boolean fail = true;
        if ((evTr.getEnd().getDate().compareTo(searchTr.getBegin().getDate()) >= 0) && (evTr.getEnd().getDate().compareTo(searchTr.getEnd().getDate()) <= 0)) {  
           fail = false; 
        }
        if ((evTr.getBegin().getDate().compareTo(searchTr.getBegin().getDate()) >= 0) && (evTr.getBegin().getDate().compareTo(searchTr.getEnd().getDate()) <= 0)) { 
            fail = false; 
        }
        if ((evTr.getBegin().getDate().compareTo(searchTr.getBegin().getDate()) <= 0) && (evTr.getEnd().getDate().compareTo(searchTr.getEnd().getDate()) >= 0)) { 
            fail = false; 
        }
        if (fail) {
            fail ("event " + TimeRangeFormat.format(evTr) 
                    + " doesn't fall within the date range " + TimeRangeFormat.format(searchTr));
        }
    }
    

}
