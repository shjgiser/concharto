package com.tech4d.tsm.web;

import com.tech4d.tsm.model.TsEvent;
import com.tech4d.tsm.model.geometry.TimeRange;
import com.tech4d.tsm.service.EventSearchService;
import com.tech4d.tsm.util.JSONFormat;
import com.tech4d.tsm.web.util.PointPropertyEditor;
import com.tech4d.tsm.web.util.TimeRangePropertyEditor;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.util.GeometricShapeFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractFormController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

public class EventSearchController extends AbstractFormController {
    private static final double LONGITUDE_180 = 180d;
    private static final String MODEL_EVENTS = "events";
    private EventSearchService eventSearchService;
    private String formView;
    private String successView;
    

    public String getFormView() {
        return formView;
    }


    public void setFormView(String formView) {
        this.formView = formView;
    }


    public String getSuccessView() {
        return successView;
    }


    public void setSuccessView(String successView) {
        this.successView = successView;
    }


    public EventSearchService getEventSearchService() {
        return eventSearchService;
    }


    public void setEventSearchService(EventSearchService eventSearchService) {
        this.eventSearchService = eventSearchService;
    }

    @Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder)
            throws Exception {
        binder.registerCustomEditor(TimeRange.class, new TimeRangePropertyEditor());
        binder.registerCustomEditor(Point.class, new PointPropertyEditor());
        super.initBinder(request, binder);
    }

    /**
     * Calculate the bounding box, given the north east and south west coordinates of 
     * a box.  Take into account boxes that span the dateline where longitude changes
     * from 180 to -180
     * 
     * TODO put the longitude logic in a utility class
     */
    private Set<Geometry> getBoundingBox(EventSearchForm se) {
        //longitudes have to contend with the international date line where it switches from -180 to +180
        //so we mod 180.  We assume the bounding box is less than 360 degrees.  If you want to figure
        //this out, you might want to draw it on paper
        Set<Geometry> polygons = new HashSet<Geometry>();
        Point base = se.getBoundingBoxSW();
        Double height = se.getBoundingBoxNE().getY() - se.getBoundingBoxSW().getY();
        Double east = se.getBoundingBoxNE().getX();
        Double west = se.getBoundingBoxSW().getX();
        if (east < west) {
            //System.out.println("East = " + east +", West = " + west);
            //ok this box spans the date line.  We need two bounding boxes.
            Double westWidth = LONGITUDE_180 + east;
            Double eastWidth = LONGITUDE_180 - west;
            
            Geometry westmost = makeRectangle(height, westWidth,  -LONGITUDE_180, base.getY()); 
            Geometry eastmost = makeRectangle(height, eastWidth,  west, base.getY()); 

            polygons.add(westmost);
            polygons.add(eastmost);
            //System.out.println(gf.buildGeometry(polygons).toText());
        } else {
            polygons.add(makeRectangle(height, east-west,  base.getX(), base.getY()));
        }
        return polygons;
    }


    private Geometry makeRectangle(Double height, Double width, Double eastMost, Double southMost) {
        GeometricShapeFactory gsf = new GeometricShapeFactory();
        gsf.setNumPoints(4);
        gsf.setBase(new Coordinate(eastMost, southMost));
        gsf.setHeight(height);
        gsf.setWidth(width);
        //System.out.println(gsf.createRectangle().toText());
        return gsf.createRectangle();
    }


    @SuppressWarnings("unchecked")
    @Override
    protected ModelAndView processFormSubmission(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) throws Exception {
        /*
         * 1. Geocode "where" and get the lat-long bounding box of whatever zoom
         * level we are at. 2. Parse the time field to extract a time range 3.
         * Do a searcg to find the count of all events within that text filter,
         * time range and bounding box
         */
        EventSearchForm eventSearchForm = (EventSearchForm) command;

        Map model = errors.getModel();
        //TODO set max results from somewhere?
        if (eventSearchForm.getMapCenter() != null) {
            List<TsEvent> events = new ArrayList<TsEvent>(); 
            Set<Geometry> boxes = getBoundingBox(eventSearchForm);  //there may be two 
            for (Geometry geometry : boxes) {
                List results = eventSearchService.search(10, eventSearchForm.getWhat(), eventSearchForm.getWhen(), geometry);
                events.addAll(results);
            }

            model.put(MODEL_EVENTS, events);
            //NOTE: we are putting the events into the command so that the page javascript
            //functions can properly display them using google's mapping API
            eventSearchForm.setSearchResults(JSONFormat.toJSON(events));
        }
        if (errors.hasErrors()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Data binding errors: " + errors.getErrorCount());
            }
            return showForm(request, response, errors);
        }
        else {
            logger.debug("No errors -> processing submit");
            return new ModelAndView(getSuccessView(), model);
        }
    }

    @Override
    protected ModelAndView showForm(
            HttpServletRequest request, HttpServletResponse response, BindException errors)
            throws Exception {

        return showForm(request, errors, getFormView());
    }
    
}
