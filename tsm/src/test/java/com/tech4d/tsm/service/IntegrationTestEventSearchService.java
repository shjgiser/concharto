package com.tech4d.tsm.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.tech4d.tsm.dao.StyleUtil;
import com.tech4d.tsm.dao.TsEventDao;
import com.tech4d.tsm.dao.TsEventTesterDao;
import com.tech4d.tsm.dao.TsEventUtil;
import com.tech4d.tsm.model.TsEvent;
import com.tech4d.tsm.model.geometry.TimeRange;
import com.tech4d.tsm.util.ContextUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class IntegrationTestEventSearchService {
    private static EventSearchService eventSearchService;

    private static TsEventDao tsEventDao;
    private static TsEventTesterDao tsEventTesterDao;

    private static TsEventUtil tsEventUtil;
    private static Polygon searchBox = makeBoundingRectangle(300, 300);
    private static TimeRange searchTimeRange = new TimeRange(makeDate(2005, 2, 22), makeDate(2007, 2, 22));
    private static Polygon failBox = makeBoundingRectangle(3000, 3000);
    private static TimeRange failTimeRange = new TimeRange(makeDate(1005, 2, 22), makeDate(1007, 2, 22));
    private static String[] failStrings = { "the a is", "is", "sdfgsdfg" };
    private static Geometry insideTheBox;
    private static Geometry outsideTheBox;
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
        2, 
        2, 
        2, 
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
        "(population of agglomeration: 600.000) and an area of approximately 100 km�. It is " ,
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
    private static TimeRange insideTimeRange = new TimeRange(makeDate(2006, 2, 22), makeDate(2006, 9, 22));
    private static TimeRange halfwayOutsideTimeRange = new TimeRange(makeDate(2000, 2, 22), makeDate(2007, 2,
            22));
    private static TimeRange outsideTimeRange = new TimeRange(makeDate(2000, 2, 22), makeDate(2002, 2, 22));
    private static TsEvent actual;

    // search parameters
    private static int MAX_RESULTS = 3;

    @BeforeClass
    public static void setUpClass() throws ParseException {
        ApplicationContext appCtx = ContextUtil.getCtx();
        eventSearchService = (EventSearchService) appCtx.getBean("eventSearchService");
        tsEventDao = (TsEventDao) appCtx.getBean("tsEventDao");
        tsEventTesterDao = (TsEventTesterDao) appCtx.getBean("tsEventTesterDao");
        tsEventUtil = new TsEventUtil(eventSearchService.getSessionFactory());
        tsEventTesterDao.deleteAll();
        StyleUtil.setupStyle();

        insideTheBox = new WKTReader().read("POINT (330 330)");
        outsideTheBox = new WKTReader().read("POINT (130 130)");

        // sample data
        // these two pass with all parameters
        for (int i=0; i< shouldMatchDescription.length; i++) {
            actual = makeSearchTsEvent(insideTheBox, insideTimeRange, shouldMatchSummary[i], shouldMatchDescription[i]);
        }
        // the rest have at least one thing out of bounds
        makeSearchTsEvent(outsideTheBox, insideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);
        makeSearchTsEvent(insideTheBox, outsideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);
        makeSearchTsEvent(insideTheBox, halfwayOutsideTimeRange, shouldMatchSummary[0], shouldMatchDescription[0]);
        makeSearchTsEvent(insideTheBox, insideTimeRange, shouldNotMatch, shouldNotMatch);
        makeSearchTsEvent(outsideTheBox, insideTimeRange, shouldNotMatch, shouldNotMatch);
        makeSearchTsEvent(outsideTheBox, outsideTimeRange, shouldNotMatch, shouldNotMatch);
    }


    @Test
    public void testSearchReturnsSome() throws ParseException {

        for (int i=0; i<searchStrings.length; i++) {
            String searchString = searchStrings[i];
            List<TsEvent> tsEvents = eventSearchService.search(MAX_RESULTS, searchString,
                    searchTimeRange, searchBox);
            assertEquals("matches against '" + searchString + "'", matches[i], tsEvents.size());
            TsEvent returned = tsEvents.get(0);
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
            assertEquals(insideTimeRange.getBegin(), returned.getWhen().getBegin());
            // Make sure we can get everything
            tsEventUtil.assertEquivalent(actual, tsEvents.get(0));
        }
    }
    
    @Test public void noneInBox() {
        // now search in a bounding box that is out
        assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS,
                searchStrings[0], searchTimeRange, failBox).size());
    }

    @Test public void noTextMach() {
        // now search strings that are don't count words
        for (String failString : failStrings) {
            assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS, failString,
                    searchTimeRange, searchBox).size());
        }
    }
    
    @Test public void noTimeRangeMatch() {
        // now search timeframes that are out
        assertEquals("none should match", 0, eventSearchService.search(MAX_RESULTS,
                searchStrings[0], failTimeRange, searchBox).size());
    }
    
    @Test public void checkMaxReturn() {
        // now set the max return threshold to below the number of possible results
        assertEquals("only one should match", 1, eventSearchService.search(1,
                searchStrings[0], searchTimeRange, searchBox).size());
    }
    
    @Test public void checkNullSearchText() {
        assertEquals("three should match", 3, eventSearchService.search(MAX_RESULTS,
                null, searchTimeRange, searchBox).size());
    }

    @Test public void checkCount() {
        assertEquals("two should match", 2, eventSearchService.search(MAX_RESULTS,
                searchStrings[0], searchTimeRange, searchBox).size());
        assertEquals("two should match", 2L, (long) eventSearchService.getCount(
                searchStrings[0], searchTimeRange, searchBox));
    }

    private static Polygon makeBoundingRectangle(int x, int y) {
        GeometricShapeFactory gsf = new GeometricShapeFactory();
        gsf.setSize(100);
        gsf.setNumPoints(4);
        gsf.setBase(new Coordinate(x, y)); // pretend these are lat/longs
        return gsf.createRectangle();
    }

    private static TsEvent makeSearchTsEvent(Geometry geometry, TimeRange timeRange, String summary, String description) {
        TsEvent tsEvent = tsEventUtil.createTsEvent(geometry, timeRange, summary, description);
        tsEventDao.save(tsEvent);
        return tsEvent;
    }

    private static Date makeDate(int year, int month, int day) {
        return new GregorianCalendar(year, month, day).getTime();
    }

}
