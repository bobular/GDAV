<%@ include file="/includes/headers.jsp"%>

<div class=mainpanel>
	<table class=submission>
		<tr>
			<th class=submission>Date submitted: </th>
			<th class=submission>No. sequences</th>
			<th class=submission>Description</th>
		</tr>
		<c:forEach var="sub" items="${submissions}"  
		      varStatus="counter"  >
		      <tr>
			<td class=submission>${sub.submitted}</td>
			<td class=submission><a href="${root}/?s=${sub.submissionID}">${sub.noModels}</a></td>
			<td class=submission><a href="${root}/?s=${sub.submissionID}">${sub.description}</a></td>
		      </tr>
		   </c:forEach>

</table>
</div></body>
</html>
