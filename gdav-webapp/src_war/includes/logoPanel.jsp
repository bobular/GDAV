<c:choose>
<c:when test="${not empty remoteheader}">${remoteheader}<!-- see gdav.xml -->
</c:when>
<c:otherwise>
</head>
<body>
<div class=logopanel>
<a class=imgLink href="http://funcgen.vectorbase.org/gdav"><img class=GDAVlogo src="${root}/images/gdav-logo.png" ></a>
<h1><a class=gdavsitename href="${root}">${configInfo.title}</a></h1>
<a class=imgLink href="http://www.vectorbase.org"><img class=VBlogo src="${root}/images/VectorBase-logo_tx.png" ></a>
</div>
</c:otherwise>
</c:choose>
