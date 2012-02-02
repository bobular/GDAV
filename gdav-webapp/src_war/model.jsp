<%@ include file="/includes/headers.jsp"%>

<div class=mainpanel>
	<table>
    <c:if test="${not empty query}">
    <tr><td style="text-align: center; background: #eee;">Your search for <b><c:out value="${query}"/></b> returned one unique
entry:</td></tr>
    </c:if>

		<tr>
			<td class=modelDetails width=35% valign=top>
				<h1>${model.modelName}</h1>
				<p class=modelDetails>
					<i>${model.spp}</i><br>
					${model.description}
				</p>
			</td>
		</tr>
		<tr>
			<td class=modelDetails>
        <h2>Sequence:</h2>
        <div class=seq>${model.formatSeq}</div>
      </td>
		</tr>


		<c:choose>
      <c:when test="${fn:length(model.alignments) > 0}">
  		  <tr>
  			 <td class=modelDetails>
  				 <h2>Alignments:</h2>
  				 <table class=alignment>
  					 <tr><th class=alignment>Species/assembly</th><th class=alignment>Method</th><th class=alignment>Location</th></tr>
  					 <c:forEach var="alignment" items="${model.alignments}" varStatus="counter"  >
  						 <tr>
  							 <td class=alignment><i>${alignment.spp}</i></td>
  							 <td class=alignment>${alignment.method}</td>
  							 <td class=alignment>
  								 <c:choose>
  									 <c:when test="${configInfo.sppLinks[alignment.spp] != null}">
  										 <a  class=inpage href="${fn:replace(
  																configInfo.sppLinks[alignment.spp],
  																"####",
  																alignment.location)}">${alignment.location}</a>
  									 </c:when>
  									 <c:otherwise>
  										 ${alignment.location}
  									 </c:otherwise>
  								 </c:choose>
  								
  								 ${configInfo.sppLinks[alignment.spp] != null ? "</a>" : ""}
  								
  							 </td>
  						 </tr>
  					 </c:forEach>
  				 </table>
  
  			 </td>
   		  </tr>
  		</c:when>
  		<c:otherwise>
  		  <tr>
  			  <td class=modelDetails>
  				  <h2>No alignments.</h2>
          </td>
        </td>
   		</c:otherwise>
		</c:choose>
		<tr>
			<td class=modelDetails>
				<h2>Annotations:</h2>
				<c:forEach var="annotationTable" items="${model.annotationTables}" varStatus="counter"  >
					<h3>${annotationTable.description}</h3>
					<table class=annotation>
						<tr>
							<c:forEach var="col" items="${annotationTable.cols}" varStatus="counter"  >
							    <c:if test='${annotationTable.columnDisplays[col]}'>
										<th <c:if test='${not empty annotationTable.columnClasses[col]}'> class="${annotationTable.columnClasses[col]}"</c:if> >${col}</th>
        						</c:if>
							</c:forEach>
						</tr>
						
						<c:forEach var="row" items="${annotationTable.rows}" varStatus="counter"  >
							<tr>
								<c:forEach var="col" items="${annotationTable.cols}" varStatus="counter"  >
							        <c:if test='${annotationTable.columnDisplays[col]}'>
										<td <c:if test='${not empty annotationTable.columnClasses[col]}'> class="${annotationTable.columnClasses[col]}"</c:if> >
											<c:if test="${annotationTable.columnLinks[col] != null}">
												<a class=inpage href="${fn:replace(
																annotationTable.columnLinks[col],
																"####",
																row.annotations[col])}
															">
											
											</c:if>
											${row.annotations[col]}
											${annotationTable.columnLinks[col] != null ? "</a>" : ""}
										</td>
        							</c:if>
								</c:forEach>
							</tr>
						</c:forEach>
						
					</table>
				</c:forEach>
			</td>
		</tr>
	</table>

</div>
</body>
</html>
