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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="tsm"%>


<tsm:page title="Spotlight Edit">
	<jsp:attribute name="bodyattr">onload="document.getElementById('spotlightEditForm').label.focus()"</jsp:attribute>
	<jsp:attribute name="stylesheet">textpanel.css,spotlight.css,header.css</jsp:attribute>

	<jsp:body>
	  <form:form id="spotlightEditForm" name="spolight" commandName="spotlight"> 
	  	<div class="textpanel">
				<h2>Spotlight</h2>

				<div class="inputcell">
		        <span class="inputlabel">Label</span> 
		        <span class="errorLabel"><form:errors path="label" element="div"/></span>
		        <br/>
		        <form:input cssClass="textInput" path="label"/>
   		   </div>
				<div class="inputcell">
		        <span class="inputlabel">Link</span> 
		        <span class="errorLabel"><form:errors path="link" element="div"/></span>
		        <br/>
		        <form:input cssClass="textInput" path="link" htmlEscape="true"/>
   		   </div>
				<div class="inputcell">
		        <form:checkbox path="visible" htmlEscape="true"/> 
		        <span class="inputlabel">Visible</span>
   		   </div>
					
				<div class="inputcell">
				  <input type="submit" value="Submit" />
				  <input type="button" value="Cancel" onclick="document.location='spotlightlist.htm'"/>
				</div>
		  </div>
	  </form:form>
	  
	</jsp:body>
</tsm:page>

