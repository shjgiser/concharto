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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="display" uri="http://displaytag.sf.net" %>
<div class="cleanTable">
	<display:table id="dt" 
		name="event.flags" 
		sort="list" 
		requestURI="${requestURI}"
		pagesize="5"
		defaultsort="1"
		>
		<display:setProperty name="paging.banner.item_name">Flag</display:setProperty>
		<display:setProperty name="paging.banner.items_name">Flags</display:setProperty>
		<display:setProperty name="paging.banner.onepage">&nbsp;</display:setProperty>
		<display:setProperty name="paging.banner.no_items_found">&nbsp;</display:setProperty>
		<display:setProperty name="paging.banner.one_item_found">&nbsp;</display:setProperty>
		<display:setProperty name="paging.banner.all_items_found">
			<span class="pagebanner"><b>{0}</b> {1} found.</span>
		</display:setProperty>
		<display:setProperty name="paging.banner.some_items_found">
			<span class="pagebanner"><b>{0}</b> {1} found.</span>
		</display:setProperty>

		<%-- TODO put col widths in CSS --%>						
		<display:column defaultorder="descending" sortable="true" sortName="created" style="width:12em" title="Date" >
		 	<fmt:formatDate value="${dt.created}" pattern="MMM dd, yyyy hh:mm a"/>
		</display:column>
		<display:column sortable="true" title="User"  style="width:10.8em" >
			<jsp:include page="../../include/userlinks.jsp">
				<jsp:param name="user" value="${dt.username}"/>
			</jsp:include>
		</display:column>
		<display:column title="Comment"><c:out value="${dt.comment}"/>&nbsp;</display:column>
		<display:column title="Reason" >
			<spring:message code="flag.reason.${dt.reason}"/>
		</display:column>
		<display:column sortable="true" style="width:14em" title="Disposition">
			<c:choose>
				<c:when test="${dt.disposition != null}">
					${dt.disposition} 
					<%-- Only admins can resolve flags --%>
					<c:if test="${fn:contains(roles, 'admin')}">
						<span style="align:right" class="adminBox">ADMIN
						(<a class="adminLinks" href="${basePath}admin/flagdisposition.htm?id=${dt.id}"
						>Reopen</a>) 
						</span>
					</c:if>
				</c:when>
				<c:otherwise>
					<%-- Only admins can resolve flags --%>
					<c:if test="${fn:contains(roles, 'admin')}">
						<div class="adminBox">ADMIN<br/>
							<c:forEach items="${dispositions}" var="dispositionCode">										
								<a class="errorlinks" href="#" onclick="confirmAndSubmit(${dt.id}, '${dispositionCode}')">
									<spring:message code="flag.disposition.${dispositionCode}"/>
								</a><br/>
							</c:forEach>
						</div>
					</c:if>
				</c:otherwise> 
			</c:choose>
		</display:column>
	</display:table>
</div>
