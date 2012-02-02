<%@ include file="/includes/headers.jsp"%>

<div class=mainpanel>
	<table class=submission>
		<tr>
			<th class=submission>Date submitted: </th>
			<th class=submission>No. sequences</th>
			<th class=submission>Description</th>
		</tr>
		<c:forEach var="sub" items="${submission}"  
		      varStatus="counter"  >
		      <tr>
			<td class=submission>${sub.submitted}</td>
			<td class=submission><a href="${root}/?s=${sub.submissionID}">${sub.noModels}</a></td>
			<td class=submission>${sub.description}</td>
		      </tr>
		   </c:forEach>
	</table>

  <display:table name="models" pagesize="50" id="model" class="modelSummary" requestURI="${root}">
     <display:setProperty name="paging.banner.placement" value="both" />
     <display:column title="ID"><a href="${root}/?m=${model.modelID}">${model.modelName}</a></display:column>
     <display:column title="Species"><i>${model.spp}</i></display:column>
     <display:column property="description"/>
  </display:table><br><br>

</div></body>
</html>
