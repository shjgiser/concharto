<%--
Copyright 2009 Time Space Map, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tsm"%>

<tsm:page title="Map Thumbnail">
  <jsp:attribute name="script">prototype.js,map.js</jsp:attribute>
	<jsp:attribute name="head">
		<jsp:include page="../include/mapkey.js.jsp"/>
		<script type="text/javascript">
		//<![CDATA[

	<%-- the main initialize function --%>
	function initialize_embedded() {
	  _mapManager.initializeMap(new GSmallMapControl());
    _overlayManager.initialize();
    var height = _mapManager.getHeight();
    document.getElementById("map").style.height=height+"px";
    map.checkResize();
		addEvent();
	}		

	function addEvent() {
		var eventJSON = document.getElementById("mapForm").event.value;
		var event = eventJSON.evalJSON();
		if (event.gtype == 'point') {
		  createMarker(event);
		} else if ((event.gtype == 'line') || (event.gtype == 'polygon')) {
			createPoly(event);					
		}
	}
	
	function createMarker(event, totalEvents) {
		var point = new GLatLng(event.geom.lat, event.geom.lng);
    markerIcon = _baseIcon;
    markerIcon.image = _basePath+"images/icons/marker-clk.png";
    var marker = new GMarker(point, {icon:markerIcon});  
    map.setCenter(point);
    map.setZoom(11);
		map.addOverlay(marker);
	}
	
	function createPoly(event, totalEvents) {
		var points = [];
		var line = event.geom.line;
		
		for (i=0; i<line.length; i++) {
			var vertex = new GLatLng(line[i].lat, line[i].lng);
			points.push(vertex);
		}
		var poly = _overlayManager.newPoly(points, event.geom.gtype);
		if (poly) {
			map.addOverlay(poly);
		}
		map.setCenter(poly.getBounds().getCenter());
    _overlayManager.fitToPoly(poly);
	}
		//]]>
		</script>
		<style type="text/css">
			body {
				background-image: none;
			}
		</style>
	</jsp:attribute>
	<jsp:attribute name="bodyattr">onload="initialize_embedded()" onunload="GUnload();" class="mapedit" </jsp:attribute>
	<jsp:attribute name="stripped">true</jsp:attribute>

	<jsp:body>
		<form id="mapForm">
			<input type="hidden" name="event" value="<c:out escapeXml="true" value='${event}'/>"> 
			<input type="hidden" id="basePath" value="${basePath}"/>
		</form>
		<div id="map">
			Map coming...
			<noscript>
			  <p>
			    JavaScript must be enabled to get the map.
			  </p>
			</noscript>
		</div>
		
	</jsp:body>
</tsm:page>

