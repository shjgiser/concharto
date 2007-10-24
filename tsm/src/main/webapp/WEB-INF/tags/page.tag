<%@tag description="page layout" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@attribute name="head" fragment="true"%>
<%@attribute name="title" required="true" type="java.lang.String"%>
<%@attribute name="script" required="false" type="java.lang.String"%>
<%@attribute name="stylesheet" required="false" type="java.lang.String"%>
<%@attribute name="bodyattr" required="false" type="java.lang.String"%>
<%@attribute name="stripped" required="false" type="java.lang.String"%>
<%@tag import="com.tech4d.tsm.auth.AuthConstants" %>
<%
String path = request.getContextPath();
String basePath = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+path+"/";
request.setAttribute("basePath", basePath);
String roles = (String)request.getSession().getAttribute(AuthConstants.SESSION_AUTH_ROLES);
request.setAttribute("roles", roles);
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd" >
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
<head>
<title>Time Space Map - ${title} </title>
<style type="text/css">
    @import url("<c:url value="/css/style.css"/>");
	<c:forTokens var="item" items="${stylesheet}" delims=",">
    @import url("<c:url value="/css/"/>${item}");
	</c:forTokens>    
</style>

<c:forTokens var="item" items="${script}" delims=",">
<script type="text/javascript" src="<c:url value="/javascript/"/>${item}">
</script>
</c:forTokens>    

<jsp:invoke fragment="head" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
</head>

<body ${bodyattr}>

	<c:if test="${stripped != 'true'}">
		<jsp:include flush="true" page="/WEB-INF/jsp/include/topmenu.jsp"/>
		<jsp:include flush="true" page="/WEB-INF/jsp/include/head.jsp"/>
  </c:if>
	<div id="content">
		<jsp:doBody />
	</div>

	<c:if test="${stripped != 'true'}">
	  <div id="footer">
		  <div id="clearFloat"/>  
			Time Space Map
	  </div>
  </c:if>
  </body>
</html>
