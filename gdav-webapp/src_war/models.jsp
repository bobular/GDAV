<%@ include file="/includes/headers.jsp"%>

<div class=mainpanel>
	<c:if test="${query!=null}">
		<span class=searchTerm>You searched for: <b><c:out value="${query}"/></b></span><br>
	</c:if>
  <c:if test="${noModels > 1}">
    The most relevant results are returned first.<br>
  </c:if>

  <display:table name="models" pagesize="50" id="model" class="modelSummary" requestURI="${root}">
     <display:setProperty name="paging.banner.placement" value="both" />
     <display:column title="ID"><a href="${root}/?m=${model.modelID}">${model.modelName}</a></display:column>
     <display:column title="Species"><i>${model.spp}</i></display:column>
     <display:column property="description"/>
  </display:table><br><br>

</div></body>
</html>
