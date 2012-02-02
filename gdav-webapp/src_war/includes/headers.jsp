<html>
<head>
	<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core-rt" %>
	<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
  <%@ taglib prefix="display" uri="http://displaytag.sf.net" %>
	<link rel="stylesheet" type="text/css" href="${root}/stylesheets/stylesheet.css" />
<title><c:out value="${configInfo.title}${query != null || model.modelName != null ? \" - \" : \"\"}${query != null ? query : model.modelName != null ? model.modelName : \"\"}" /></title>
<%@ include file="/includes/logoPanel.jsp"%>
<%@ include file="/includes/menu.jsp"%>

