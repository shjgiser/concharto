<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tsm"%>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
request.setAttribute("basePath", basePath);
%>

<tsm:page title="Map Thumbnail">
	<jsp:attribute name="head">
		<script
			src="http://maps.google.com/maps?file=api&amp;v=2.x&amp;key=<spring:message code='map.key'/>"
			type="text/javascript">
		</script>		
		<script type="text/javascript">
		//<![CDATA[

	<%-- the main initialize function --%>
	function initialize() {
		initializeMap(new GSmallMapControl());
		addEvent();
	}		
	
	function addEvent() {
		var eventJSON = document.getElementById("mapForm").event.value;
		var event = eventJSON.parseJSON();
		if (event.gtype == 'point') {
		  createMarker(event);
		} else if ((event.gtype == 'line') || (event.gtype == 'polygon')) {
			createPoly(event);					
		}
	}
	
		<%-- called by createOverlay --%>
	function createMarker(event) {
		var point = new GLatLng(event.geom.lat, event.geom.lng);
		var marker = new GMarker(point);
		map.addOverlay(marker);
		map.setCenter(point);
	}
	
	<%-- called by createOverlay --%>
	function createPoly(event) {
		var points = [];
		var line = event.geom.line;
		
		for (i=0; i<line.length; i++) {
			var vertex = new GLatLng(line[i].lat, line[i].lng);
			points.push(vertex);
		}
		var poly = newPoly(points, event.geom.gtype);
		if (poly) {
			map.addOverlay(poly);
		}
		fitToOverlay(poly);
		map.setCenter(poly.getBounds().getCenter());
	}
		//]]>
		</script>
	</jsp:attribute>
	<jsp:attribute name="script">map.js,json.js</jsp:attribute>
	<jsp:attribute name="bodyattr">onload="initialize()" onunload="GUnload();" class="mapedit" onresize="setMapExtent();"</jsp:attribute>
	<jsp:attribute name="stripped">true</jsp:attribute>

	<jsp:body>
		<form id="mapForm">
			<input type="hidden" name="event" value="<c:out escapeXml="true" value='${event}'/>" >
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
