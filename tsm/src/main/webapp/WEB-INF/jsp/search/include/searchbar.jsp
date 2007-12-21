<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<table id="headerbar">
 	<tr>
   	<td>
	    <div id="addbox">
        <span class="biglink"><a href="#" onclick="editEvent('')">Add to the Map...</a></span>
      </div>      
      <div id="searchbox">
        <table>
          <col id="left"/>
          <col id="right"/>
          <tr>
	          <td>
			  			<form:errors path="where"><span class="errorLabel"></form:errors>
			        Where <span class="eg">e.g., Gettysburg, PA</span>
			  			<form:errors path="where"></span></form:errors>
		  			</td>
            <!--  <td>Where <span class="eg">e.g., Gettysburg, PA</span></td>  -->
            <td>
            	<form:errors path="when"><span class="errorLabel"></form:errors>
            	When <span class="eg">e.g., 1962; Oct 14,1066; 1880-1886</span>
            	<form:errors path="when"></span></form:errors>
            </td>
            <td>
            	What <span class="eg">e.g., Battle</span>
            </td>
            <td></td>
          </tr>
          <tr>
            <td><form:input path="where" size="22"/></td>
            <td><form:input path="when" size="35"/></td>
            <td><form:input path="what" size="22" htmlEscape="true"/></td>
            <td><input type="submit" name="Search" value="Search" /></td>
          </tr>
				  <c:choose>
				  	<c:when test="${param.showSearchOptions == 'true'}">
		          <tr>
		            <td>
		            	<span class="options">
		            		<form:checkbox path="limitWithinMapBounds" value="true"/>
		            		Search current map shown.
		            	</span>
		            </td>
		            <td colspan="2">
		              <span class="options">
		              	<form:checkbox path="excludeTimeRangeOverlaps"/>
		              	Find events that occurred only <em>within</em> the time frame specified.
		              </span>
		            </td>
		            <td></td>
		          </tr>
				  	</c:when>
				  	<c:otherwise>
				 		<form:hidden path="limitWithinMapBounds"/> 
				  	</c:otherwise>
				  </c:choose>
        </table>
      </div>
    </td>
  </tr>
</table>
<c:if test="${fn:contains(roles, 'admin') && param.showAdminBar == 'true'}">
	<div class="adminBox">
	 	<span class="label">ADMIN</span>
	 	<span class="adminField">
	 		<form:radiobutton value="normal" path="show"/> <span class="label">Normal </span>
	 	</span>
 		<span class="adminAction">
 			<form:radiobutton value="hidden" path="show"/>
 			<span class="label">Removed </span>
 		</span>
 		<span class="adminAction">
 			<form:radiobutton value="flagged" path="show"/>
 			<span class="label">Flagged </span>
 		</span>
  </div> 
</c:if>  

<div class="clearfloat"></div>