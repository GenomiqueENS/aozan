<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" version="1.0" encoding="UTF-8"
		indent="yes" doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
		doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd" />

	<xsl:template match="/">
		<xsl:decimal-format name="aozan" decimal-separator="."
			grouping-separator=" " />

		<html>
			<head>
				<title>
					<xsl:value-of select="/RecoveryClusterReport/sampleName" />
					Recovery clusters
				</title>
				<!-- <link rel="stylesheet" href="report.css"></link> -->
				<style TYPE="text/css">

img {
	width: 150px;
}
body{
	font-family: sans-serif;
	font-size: 80%;
	margin:0;
}
h1{
	color: #234CA5;
	font-style : italic;
	font-size : 20px;
}
h2{}
h3{
	color:black;
	font-style : italic;
}

table {
	border: medium solid #000000;
	font-size: 95%;
	border-collapse:collapse;
}

td {
	text-align: right;
	width: 100px;
	border: 1px solid black;
	
	padding-left:3px;
	padding-right:3px;
	padding-top:1px;
	padding-bottom:1px;
	position : static;
	background-clip: padding-box;
}
th {
	background-color:#E7EAF1;
	color:black;
	border: thin solid black;
	border-bottom-width: 2px;
	width: 50px;
	font-size: 100%;
}

#data tr:hover {
	z-index:2;
	box-shadow:0 0
	12px rgba(0, 0, 0, 1);
	background:#F6F6B4;
}
tr.total{
	background:#82D482;
	font-style : bold;
}
tr.demultiplexing{
	background:#FFFFA3;
	font-style : bold;
}
#data td:first-child{
	text-align:center;
	font-style : bold;
	background-color:#D7A1A3;
	font-family:Courier New;
	font-size:120%;
}
#data td:last-child{
	width: 200px;
	text-align: left;
	font-style : italic;
}

div.header {
	background-color: #C1C4CA;
	border:0;
	margin:0;
	padding: 0.5em;
	font-size: 175%;
	font-weight: bold;
	width:100%;
	height: 2em;
	position:
	fixed;
	vertical-align: middle;
	z-index:2;
}

#header_title {
	display:inline-block;
	float:left;
	clear:left;
	font-size: 85%;
}

#header_right {
     display:inline-block;
     float:right;
     clear:right;
     font-size: 55%;
     margin-right:2em;
     text-align: right;
     vertical-align:middle;
   }
div.report {
	display:block;
	position:absolute;
	width:100%;
	top:6em;
	bottom:5px;
	left:0;
	right:0;
	padding:0 0 0 1em;
	background-color: white;
}

div.footer {
	background-color: #C1C4CA;
	border:0;
	margin:0;
	padding:0.5em;
	height: 1.3em;
	overflow:hidden;
	font-size: 100%;
	font-weight: bold;
	position:fixed;
	bottom:0;
	width:100%;
	z-index:2;
}
#topPage{
	text-align: left;
}
					
a.linkFilterActivate:link, a.linkFilterUnactivate:link{
	color:#ffffff;
}  
 	.linkFilterActivate {
	-webkit-border-top-left-radius:24px;
	-moz-border-radius-topleft:24px;
	border-top-left-radius:24px;
	-webkit-border-top-right-radius:24px;
	-moz-border-radius-topright:24px;
	border-top-right-radius:24px;
	-webkit-border-bottom-right-radius:24px;
	-moz-border-radius-bottomright:24px;
	border-bottom-right-radius:24px;
	-webkit-border-bottom-left-radius:24px;
	-moz-border-radius-bottomleft:24px;
	border-bottom-left-radius:24px;

	background-color:#9A1319;
	text-indent:0px;
	border:2px solid #000000;
	display:inline-block;
	color:#ffffff;
	font-family:Arial;
	font-size:15px;
	font-weight:bold;
	font-style:normal;
	line-height:29px;
	text-decoration:none;
	text-align:center;
	text-shadow:1px 1px 0px #810e05;
	padding-left:5px;
	padding-right:5px;
}

.linkFilterUnactivate {
	-webkit-border-top-left-radius:24px;
	-moz-border-radius-topleft:24px;
	border-top-left-radius:24px;
	-webkit-border-top-right-radius:24px;
	-moz-border-radius-topright:24px;
	border-top-right-radius:24px;
	-webkit-border-bottom-right-radius:24px;
	-moz-border-radius-bottomright:24px;
	border-bottom-right-radius:24px;
	-webkit-border-bottom-left-radius:24px;
	-moz-border-radius-bottomleft:24px;
	border-bottom-left-radius:24px;

	background-color:#A7A9AF;
	text-indent:0px;
	border:2px solid #000000;
	display:inline-block;
	color:#ffffff;
	font-family:Arial;
	font-size:15px;
	font-weight:bold;
	font-style:normal;
	line-height:29px;
	text-decoration:none;
	text-align:center;
	text-shadow:1px 1px 0px #4E5C86;
	padding-left:5px;
	padding-right:5px;
}
.linkFilterUnactivate:hover, .linkFilterActivate:hover {
	background-color:#487DD6;
	font-size:150%;
}

#filterSample td{
	border:#ffffff;
	border-style:hidden;
}
#filterSample tr:hover{
}
					
				</style>
				<script language="javascript">
					<xsl:comment><![CDATA[
				
				function filterRow(samples, elemLink) {
					//alert('hello '+ samples.length);
					init_all('none');

					for (var n=0; n < samples.length; n++){
					var tab = document.getElementById('data').getElementsByClassName(samples[n]);

						for (var i=0; i < tab.length; i++){
							tab[i].style.display ="table-row";
						}
					}
							
		        	// Change class link
			        // Unactivate link
			        document.getElementsByClassName('linkFilterActivate')[0].setAttribute('class', 'linkFilterUnactivate');
			        // Activate new element
			        elemLink.setAttribute('class', 'linkFilterActivate');
		
				}

				function init_all(display_val){
					//alert('hello ' + display_val);
					var tab1 = document.getElementById('data').getElementsByTagName('tr');

					//alert('read1 : '+tab1.length);
					for (var i=0; i < tab1.length; i++){
						tab1[i].style.display = display_val;
					}

					var header = document.getElementsByClassName('headerColumns');
					//alert('header '+header.length);
					for (var i=0; i < header.length; i++){
						header[i].style.display = "table-row";
					}
					var total = document.getElementsByClassName('total');
					//alert('total'+total.length);
					for (var i=0; i < total.length; i++){
						total[i].style.display = "table-row";
					}
				}

				]]></xsl:comment>
				</script>

			</head>
			<body>
				<div class="header">
					<div id="header_title">
						<img
							src="http://www.transcriptome.ens.fr/aozan/images/logo_aozan_qc.png"
							alt="Aozan" />
						Clusters recoverable in <xsl:value-of select="/RecoveryClusterReport/sampleName" />
						<xsl:if test="/RecoveryClusterReport/projectName">
							- project <xsl:value-of select="/RecoveryClusterReport/projectName" />
						</xsl:if>
					</div>
					
					<div id="header_right">
						<a href="javascript:void(0);" onclick="window.scrollTo(0,0);">
							Top of page
						</a>
					</div>
				</div>

				<div class="report">
					<ul>
						<li>
							<b>Run Id: </b>
							<xsl:value-of select="/RecoveryClusterReport/RunId" />
						</li>
						<li>
							<b>Flow cell: </b>
							<xsl:value-of select="/RecoveryClusterReport/FlowcellId" />
						</li>
						<li>
							<b>Run started: </b>
							<xsl:value-of select="/RecoveryClusterReport/RunDate" />
						</li>
						<li>
							<b>Instrument S/N: </b>
							<xsl:value-of select="/RecoveryClusterReport/InstrumentSN" />
						</li>
						<li>
							<b>Instrument run number: </b>
							<xsl:value-of select="/RecoveryClusterReport/InstrumentRunNumber" />
						</li>
						<li>
							<b>Generated by: </b>
							<xsl:value-of select="/RecoveryClusterReport/GeneratorName" />
							version
							<xsl:value-of select="/RecoveryClusterReport/GeneratorVersion" />
							(revision
							<xsl:value-of select="/RecoveryClusterReport/GeneratorRevision" />
							)
						</li>
						<li>
							<b>Creation date: </b>
							<xsl:value-of select="/RecoveryClusterReport/ReportDate" />
						</li>
						<br />
						<xsl:if test="/RecoveryClusterReport/projectName">
							<li>
								<b>Project : </b>
								<xsl:value-of select="/RecoveryClusterReport/projectName" />
							</li>
						</xsl:if>
						<li>
							<b>Sample : </b>
							<xsl:value-of select="/RecoveryClusterReport/sampleName" />
						</li>
						<li>
							<b>Description : </b>
							<xsl:value-of select="/RecoveryClusterReport/description" />
						</li>
						<li>
							<b>Condition : </b>
							<xsl:value-of select="/RecoveryClusterReport/condition" />
						</li>
					</ul>

				<xsl:if test="/RecoveryClusterReport/Samples/Sample">
					<div id="filterSample">
						<table border="0">
							<tr>
								<td>Filter by sample</td>
								<td>
									<a href="javascript:void(0);" class="linkFilterActivate" onclick="window.location.reload(true);">ALL</a>
								</td>
								<xsl:for-each select="/RecoveryClusterReport/Samples/Sample">
									<td>
										<xsl:element name="a">
										<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
										<xsl:attribute name="class">linkFilterUnactivate</xsl:attribute>
										<xsl:attribute name="onclick">javascript:filterRow([<xsl:value-of
											select="@cmdJS"></xsl:value-of>], this);</xsl:attribute>
										<xsl:value-of select="." />
									</xsl:element>
									</td>
								</xsl:for-each>
								<td>
									<xsl:element name="a">
										<xsl:attribute name="href">javascript:void(0);</xsl:attribute>
										<xsl:attribute name="class">linkFilterUnactivate</xsl:attribute>
										<xsl:attribute name="onclick">javascript:filterRow([<xsl:value-of
											select="/RecoveryClusterReport/Samples/@cmdJS"></xsl:value-of>], this);</xsl:attribute>
										All samples
									</xsl:element>
								</td>
							</tr>
						</table>
					</div>
					</xsl:if>
					<p></p>

					<div id="table">
						<table id="data">
							<tr class="headerColumns">
								<xsl:for-each select="/RecoveryClusterReport/Columns/Column">
									<th>
										<xsl:value-of select="."></xsl:value-of>
									</th>
								</xsl:for-each>
							</tr>
							<xsl:for-each select="/RecoveryClusterReport/Results/Entry">
								<tr class="{@classValue}">
									<xsl:for-each select="Data">
										<td>
											<xsl:if test="@type='int'">
												<xsl:value-of
													select="format-number(.,'### ### ### ### ###','aozan')" />
											</xsl:if>
											<xsl:if test="@type='string'">
												<xsl:value-of select="." />
											</xsl:if>
										</td>
									</xsl:for-each>
								</tr>
							</xsl:for-each>
						</table>
					</div>
					<!-- end report -->
					<p>_</p>
				</div>

				<div class="footer">
					<span>
						Generated by
						<xsl:element name="a">
							<xsl:attribute name="href"><xsl:value-of
								select="/RecoveryClusterReport/GeneratorWebsite" /></xsl:attribute>
							Aozan
						</xsl:element>
						(version
						<xsl:value-of select="/RecoveryClusterReport/GeneratorVersion" />
						)
					</span>
				</div>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>