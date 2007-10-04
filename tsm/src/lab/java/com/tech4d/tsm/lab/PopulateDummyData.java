package com.tech4d.tsm.lab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.tech4d.tsm.dao.TsEventTesterDao;
import com.tech4d.tsm.dao.TsEventUtil;
import com.tech4d.tsm.model.TsEvent;
import com.tech4d.tsm.model.geometry.TimeRange;
import com.tech4d.tsm.util.ContextUtil;
import com.tech4d.tsm.util.LapTimer;
import com.tech4d.tsm.util.TimeRangeFormat;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class PopulateDummyData {
    
    private static final String TEXT_FILE = "src/lab/data/a-tale-of-two-cities.txt";
    private static TsEventTesterDao tsEventTesterDao;
    private static TsEventUtil tsEventUtil;
    private GeometryFactory gf = new GeometryFactory();
    protected static final Log logger = LogFactory.getLog(PopulateDummyData.class);

    private static final Double LONGITUDE_180 = 180d;
    private static final Double LATITUDE_90 = 90d;
    private Random rand = new Random(System.currentTimeMillis());
    private static BufferedReader textFileReader;

    @BeforeClass
    public static void setUpClass() throws FileNotFoundException {
        ApplicationContext appCtx = ContextUtil.getCtx();
        
        tsEventTesterDao = (TsEventTesterDao) appCtx.getBean("tsEventTesterDao");
        SessionFactory sessionFactory = (SessionFactory) appCtx.getBean("myTestDbSessionFactory");
        //replace the default with the test db
        tsEventTesterDao.setSessionFactory(sessionFactory);

        tsEventUtil = new TsEventUtil(tsEventTesterDao.getSessionFactory());
        logger.debug("deleting");
        LapTimer timer = new LapTimer(logger);
        tsEventTesterDao.deleteAll();
        timer.timeIt("delete").logDebugTime();
        
        openTextFileReader();
    }
    
    private static void openTextFileReader() throws FileNotFoundException {
        File textFile = new File(TEXT_FILE);
        textFileReader = new BufferedReader(new FileReader(textFile));
    }

    @AfterClass
    public static void tearDown() throws IOException {
        textFileReader.close();
    }
    
    private static final int NUM_EVENTS = 10000;
    private static final int COLLECTION_SIZE = 10000;
    @Test
    public void makeData() throws ParseException, IOException {
        Set<TsEvent> events = new HashSet<TsEvent>();
        LapTimer timer = new LapTimer(this); 
        for (int i = 0; i < NUM_EVENTS; i++) {
            TsEvent event = tsEventUtil.createTsEvent(null, null, getNextPoint(), getNextTimeRange(), null,
                    getNextText(50), getNextText(240));
            event.setSnippet(null);
            event.setSourceUrl(getNextText(100));
            event.setWhere(getNextText(80));
            event.setUserTagsAsString(getNextText(70));
            events.add(event);
            
            if (i % COLLECTION_SIZE == 0) {
                timer.timeIt("create " + i);
                tsEventTesterDao.save(events);
                timer.timeIt("save").logDebugTime();
                events.clear();
                timer.init();
            }
        }
        timer.timeIt("create");
        tsEventTesterDao.save(events);
        timer.timeIt("save").logDebugTime();

        System.out.println(minLng +", " + minLat + ", " + maxLng + ", " + maxLat );
    }

    private String getNextText(int fieldSize) throws IOException {
        int summarySize = rand.nextInt(fieldSize);
        char[] chars = new char[summarySize];
        int b;
        for (int i=0; i<summarySize; i++) {
            if (-1 != (b = textFileReader.read())) {
                chars[i] = (char)b ;            
            } else {
                System.out.println("end of file reached.  Starting over again");
                textFileReader.close();
                openTextFileReader();
            }
        }
        return String.valueOf(chars);
    }

    private static final int YEARS = 1000;
    private static final int MONTH = 12;
    private static final int HRS = 24;
    private static final int MIN = 60;
    private static final int SEC = 60;
    private static final int DAYS = 27;
    private TimeRange getNextTimeRange() throws ParseException {
        //first decide which precision to use
        int precision = rand.nextInt(2);
        if (precision == 0) {
            //let's do year precision from 1000ad to 2000ad
            int year = rand.nextInt(YEARS) + YEARS;
            return TimeRangeFormat.parse(String.valueOf(year));
        } else {
            //two date/times within the same year betweeo 1000 and 2000
            int year = rand.nextInt(YEARS) + YEARS;
            return makeDayRange(
                     rand.nextInt(MONTH), //month 
                     rand.nextInt(DAYS+1),//d1
                     year,              //y1
                     rand.nextInt(HRS), //hh1 
                     rand.nextInt(MIN), //mm1, 
                     rand.nextInt(SEC), //ss1, 
                     rand.nextInt(MONTH), //m2 
                     rand.nextInt(DAYS+1),//d2
                     year,              //y2
                     rand.nextInt(HRS), //hh2 
                     rand.nextInt(MIN), //mm2, 
                     rand.nextInt(SEC)  //ss2, 
                    );
        }
        
    }
    

    //roughly the US region
    private static final double WESTMOST = -127.353515;
    private static final double EASTMOST = -59.765625;
    private static final double NORTHMOST = 57.231502;
    private static final double SOUTHMOST = 23.725011;
    private static double maxLat = -LATITUDE_90;
    private static double maxLng = -LONGITUDE_180;
    private static double minLat = LATITUDE_90;
    private static double minLng = LONGITUDE_180;
    
    private Geometry getNextPoint() {
        double lng = rand.nextDouble() * (EASTMOST-WESTMOST) + WESTMOST;
        double lat = rand.nextDouble() * (NORTHMOST-SOUTHMOST) + SOUTHMOST;
        minLat = checkMin(lat, minLat);
        minLng = checkMin(lng, minLng);
        maxLat = checkMax(lat, maxLat);
        maxLng = checkMax(lng, maxLng);
        return gf.createPoint(new Coordinate(lng, lat));
    }

    private double checkMax(double val, double maxval) {
        if (val > maxval) {
            return val;
        } else {
            return maxval;
        }
    }

    private double checkMin(double val, double minval) {
        if (val < minval) {
            return val;
        } else {
            return minval;
        }
    }
    
    public TimeRange makeDayRange(int m1, int d1, int y1, int hh1, int mm1, int ss1, 
            int m2, int d2, int y2, int hh2, int mm2, int ss2) {
        return new TimeRange(makeDate(m1, d1, y1, hh1, mm1, ss1), makeDate(m2, d2, y2, hh2, mm2, ss2));
    }

    private Date makeDate(int month, int day, int year, int hour, int minute, int second) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}