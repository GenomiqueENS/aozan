<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" 
doctype-system="about:legacy-compat"/>

<xsl:decimal-format name="aozan" decimal-separator="." grouping-separator=" "/>
<xsl:template match="/">

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title><xsl:value-of select="/QCReport/RunId"/> run quality report</title>
  <style type="text/css">

    .score-1 {
    }
    .score0 {
      background: #F34545;
    }
    .score1 {
      background: #F34545;
    }
    .score2 {
      background: #F34545;
    }
    .score3 {
      background: #F34545;
    }
    .score4 {
      background: #F8F848;
    }
    .score5 {
      background: #F8F848;
    }
    .score6 {
      background: #55D486;
    }
    .score7 {
      background: #55D486;
    }
    .score8 {
      background: #55D486;
    }
    .score9 {
      background: #55D486;
    }

   .sampleData tr:hover, .data tr:hover {
      z-index:2;
      box-shadow:0 0 20px rgba(0, 0, 0, 1);
      background:#F6F6B4;
    }
    
    td {
      text-align: center;
      width: 100px;
      border: 1px solid black;

      padding-left:3px;
      padding-right:3px;
      padding-top:1px;
      padding-bottom:1px;
      position : static;
      background-clip: padding-box;
    }

<!--     table.sampleData td:last-child tr:last-child { -->
	.sampleData td:last-child, .sampleData tr:last-child{
      background: WhiteSmoke ;
      color: Gray;
      font-style: italic;
      font-size: 80%;
    }

    th {
        background-color:#E7EAF1;
        color:black;
        border: thin solid black;
        border-bottom-width: 2px;
        width: 50px;
        font-size: 100%;
    }
    table {
        border: medium solid #000000;
        font-size: 95%;
        border-collapse:collapse;
    }


    body{
        font-family: sans-serif;
        font-size: 80%;
        margin-left:0px;
        margin-right:0px;
        margin-top: 0px;
        margin-bottom: 0px;
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
     a:link {
        color:#1B65AF;
        font-weight: bold;
    }
    a:visited {
        color:#1B65AF;
        font-weight: bold;
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


    div.header {
		background-color: #C1C4CA;
        border:0;
        margin:0;
        padding: 0.5em;
        font-size: 175%;
        font-weight: bold;
        width:100%;
        height: 2em;
        position: fixed;
        vertical-align: middle;
        z-index:2;

    }

    #header_title {
        display:inline-block;
        float:left;
        clear:left;
    }
    #header_filename {
        display:inline-block;
        float:right;
        clear:right;
        font-size: 55%;
        margin-right:2em;
        text-align: right;
        vertical-align:middle;
    }

    div.header h3 {
        font-size: 50%;
        margin-bottom: 0;
    }
  	img {
  		width: 150px;
  	}

	a.linkFilterActivate:link{
		color:#ffffff;
	}
	 a.linkFilterInactivate:link{
	 	color:#000000;
	 }
  	.linkFilterActivate {
		border-top-left-radius:24px;
		border-top-right-radius:24px;
		border-bottom-right-radius:24px;
		border-bottom-left-radius:24px;

		background-color:#002447;
		text-indent:0px;
		border:3px solid #000000;
		display:inline-block;
		font-family:Arial;
		font-size:15px;
		font-weight:bold;
		font-style:normal;
		line-height:29px;
		text-decoration:none;
		text-align:center;
		padding-left:5px;
		padding-right:5px;
		white-space:nowrap;
	}

	.linkFilterInactivate {
		border-top-left-radius:24px;
		border-top-right-radius:24px;
		border-bottom-right-radius:24px;
		border-bottom-left-radius:24px;

		background-color:#ffffff;
		text-indent:0px;
		border:3px solid #000000;
		display:inline-block;
		font-family:Arial;
		font-size:15px;
		font-weight:bold;
		font-style:normal;
		line-height:29px;
		text-decoration:none;
		text-align:center;
		padding-left:5px;
		padding-right:5px;
		white-space:nowrap;
	}

	.linkFilterInactivate:hover, .linkFilterActivate:hover {
		background-color:#9A1319;
		color:#000000;
	}

	#filterProject td{
		border:#ffffff;
		border-style:hidden;
	}
	
  </style>
  <script type="text/javascript">
	<xsl:comment><![CDATA[

			
        // Retrieve list lane number related to projet
        function filterRow(lanes_related_project, project_name, elemLink) {

            init_all('none');

            //Split
            var lanes = lanes_related_project[0].split(',');

            alert('project name ' + project_name);

            if (project_name == "Undetermined"){
                alert('cas Undetermined')
                var table_read = document.getElementsByClassName('sampleData');

                // Parsing read
                for (var read = 0; read < table_read.length ; read++){
                    // Parsing lanes set in parameters
                    for (var j=0; j < lanes.length; j++){

                        // Get rows number
                        var node_lane = table_read[read].getElementsByClassName(lanes[j]);
                        var last_row = node_lane.length -1 ;
                        // Display last rows from selected lane
                        node_lane[last_row].style.display ="table-row";
                    }
                }
            } else {
              // Filter sample table data
              for (var n=0; n < lanes.length; n++){
                  var node = document.getElementsByClassName(lanes[n]);
                  for (var i=0; i < node.length; i++){
                      node[i].style.display ="table-row";

                  }
              }

              // Filter project table data
              var projects = document.getElementsByClassName('projectData')[0];
              var project_row = projects.getElementsByClassName(project_name);

              // One row by project
              project_row[0].style.display ="table-row";
            }

            update_link_css(elemLink)
        }
					   
		function update_link_css(elemLink){
			// Change class link
	       // Inactivate link
	       document.getElementsByClassName('linkFilterActivate')[0].setAttribute('class', 'linkFilterInactivate');
	       // Activate new element
	       elemLink.setAttribute('class', 'linkFilterActivate');

			// Change position
			window.location = '#project';
		}
			

		function init_all(display_val){
			init_table(display_val,'sampleData')
			init_table(display_val,'projectData')
					
			var header = document.getElementsByClassName('headerColumns');
			for (var i=0; i < header.length; i++){
				header[i].style.display = "table-row";
			}
		}
					
		function init_table(display_val, class_name){
			var tab = document.getElementsByClassName(class_name);

			for (var n=0; n < tab.length; n++){
				var rows = tab[n].getElementsByTagName('tr');

				for (var i=0; i < rows.length; i++){
					rows[i].style.display = display_val;
				}
			}
		}					
		]]></xsl:comment>
		</script>
</head>
<body>

<div class="header">
	<div id="header_title">
		<span><img src="http://www.transcriptome.ens.fr/aozan/images/logo_aozan_qc.png" alt="Aozan"/></span>
		<span>Quality report on run <xsl:value-of select="/QCReport/RunId"/></span>
	</div>

	<div id="header_filename">
		Generated <xsl:value-of select="/QCReport/ReportDate"/><br/>
		<a href="#global">Global</a> / <a href="#lane">Lanes</a> / <a href="#project">Projects</a> / <a href="#sample">Samples</a>
	</div>

</div>

<div class="report">
  <ul>
    <li><b>Run Id: </b> <xsl:value-of select="/QCReport/RunId"/></li>
    <li><b>Flow cell: </b> <xsl:value-of select="/QCReport/FlowcellId"/></li>
    <li><b>Run started: </b> <xsl:value-of select="/QCReport/RunDate"/></li>
    <li><b>Instrument S/N: </b> <xsl:value-of select="/QCReport/InstrumentSN"/></li>
    <li><b>Instrument run number: </b> <xsl:value-of select="/QCReport/InstrumentRunNumber"/></li>
    <li><b>Generated by: </b> <xsl:value-of select="/QCReport/GeneratorName"/> version <xsl:value-of select="/QCReport/GeneratorVersion"/>
    	(revision <xsl:value-of select="/QCReport/GeneratorRevision"/>)</li>
    <li><b>Creation date: </b> <xsl:value-of select="/QCReport/ReportDate"/></li>
  </ul>

  <xsl:if test="/QCReport[GlobalReport]">
  <a id="global"/>
  <h2>Global report</h2>
  <table class="data">
  <tr>
    <xsl:for-each select="/QCReport/GlobalReport/Columns/Column">
      <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
    </xsl:for-each>
  </tr>
  <tr>
    <xsl:for-each select="/QCReport/GlobalReport/Run/Test">
      <td class="score{@score}">
        <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
        <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
        <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
        <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
        <xsl:if test="@type='url'"><a href="{.}">link</a></xsl:if>
      </td>
    </xsl:for-each>
  </tr>
  </table>
  </xsl:if>

  <xsl:if test="/QCReport[ReadsReport]">
  <a id="lane"></a>
  <h2>Lanes Quality report</h2>
<div>
  <xsl:for-each select="/QCReport/ReadsReport/Reads/Read">
    <h3>Read <xsl:value-of select="@number"/> (<xsl:value-of select="@cycles"/> cycles<xsl:if test="@indexed='true'">, index</xsl:if>)</h3>
    <table class="data">
    <tr>
      <th>Lane</th>
      <xsl:for-each select="/QCReport/ReadsReport/Columns/Column">
        <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
      </xsl:for-each>
    </tr>
    <xsl:for-each select="Lane">
      <tr>
       <td><xsl:value-of select="@number"/></td>
       <xsl:for-each select="Test">
         <td class="score{@score}">
           <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
           <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
           <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
           <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
           <xsl:if test="@type='url'"><a href="{.}">link</a></xsl:if>
         </td>
       </xsl:for-each>
     </tr>
    </xsl:for-each>

   </table>
  </xsl:for-each>
</div>
  </xsl:if>
  
  <xsl:if test="/QCReport[ProjectsReport]">
  <a id="project"></a>
  <h2>Projects statistics report</h2>
   <table class="projectData">
  <tr class="headerColumns">
	<th>Project</th>
    <xsl:for-each select="/QCReport/ProjectsReport/Columns/Column">
      <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
    </xsl:for-each>
  </tr>
  <xsl:for-each select="/QCReport/ProjectsReport/Projects/Project">
  <tr class="{@name}">
  	<td><xsl:value-of select="@name"/></td>
    <xsl:for-each select="Test">
      <td class="score{@score}">
        <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
        <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
        <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
        <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
        <xsl:if test="@type='url'"><a href="{.}">link</a></xsl:if>
      </td>
    </xsl:for-each>
  </tr>
    </xsl:for-each>
  
  </table>
	</xsl:if>

  <a id="sample"></a>
  <xsl:if test="/QCReport[SamplesReport]">
  <h2>Samples Quality report</h2>

  <a id="project"></a>
  <xsl:if test="/QCReport[ProjectsReport]">
	  <div>
	  <h4>Filter samples by projects</h4>
	  <table id="filterProject">
	    <tr>
	      <td><a href="javascript:void(0);" class="linkFilterActivate" onclick="window.location.reload(true);">ALL</a></td>

	      <xsl:for-each select="/QCReport/ProjectsReport/ProjectName">
		    <td>
			  <xsl:element name="a">
			  <xsl:attribute name="class">linkFilterInactivate</xsl:attribute>
			  <xsl:attribute name="href">javascript:void(0);</xsl:attribute>
			  <xsl:attribute name="onclick">javascript:filterRow([<xsl:value-of select="@cmdJS"></xsl:value-of>], '<xsl:value-of select="." />', this);</xsl:attribute>
				<xsl:value-of select="." /></xsl:element>
			</td>
	      </xsl:for-each>
	    </tr>
	  </table>
	<!--   end filter by project -->
	  </div>
  </xsl:if>

  <xsl:for-each select="/QCReport/SamplesReport/Reads/Read">
    <h3>Read <xsl:value-of select="@number"/></h3>
    <table class="sampleData">
    <tr class="headerColumns">
      <th>Lane</th>
      <th>Sample name</th>
      <th>Description</th>
      <th>Index</th>
      <th>Project</th>
      <xsl:for-each select="/QCReport/SamplesReport/Columns/Column">
        <th><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></th>
      </xsl:for-each>
      <th>Sample name</th>
    </tr>
    <xsl:for-each select="Sample">
      <tr class="{@lane}">
       <td><xsl:value-of select="@lane"/></td>
       <td><xsl:value-of select="@name"/></td>
       <td><xsl:value-of select="@desc"/></td>
       <td><xsl:value-of select="@index"/></td>
       <td><xsl:value-of select="@project"/></td>
       <xsl:for-each select="Test">
         <td class="score{@score}">
           <xsl:if test="@type='int'"><xsl:value-of select="format-number(.,'### ### ### ### ###','aozan')"/></xsl:if>
           <xsl:if test="@type='float'"><xsl:value-of select="format-number(.,'### ### ### ##0.00','aozan')"/></xsl:if>
           <xsl:if test="@type='percent'"><xsl:value-of select="format-number(.,'#0.00%','aozan')"/></xsl:if>
           <xsl:if test="@type='string'"><xsl:value-of select="."/></xsl:if>
           <xsl:if test="@type='url'"><a href="{.}">link</a></xsl:if>
         </td>
       </xsl:for-each>

       <!-- Repeat Sample Name in last column -->
        <td class="repeat"><xsl:value-of select="@name"/><br/><xsl:value-of select="@project"/></td>
       </tr>

    </xsl:for-each>

	<!-- Repeat header columns at last row -->
	<tr>
      <td class="repeat">Lane</td>
      <td class="repeat">Sample name</td>
      <td class="repeat">Description</td>
      <td class="repeat">Index</td>
      <td class="repeat">Project</td>
      <xsl:for-each select="/QCReport/SamplesReport/Columns/Column">
        <td class="repeat"><xsl:value-of select="."/><xsl:if test="@unit!=''"> (<xsl:value-of select="@unit"/>)</xsl:if></td>
      </xsl:for-each>
      <td class="repeat">Sample_Project name</td>
    </tr>

   </table>
  </xsl:for-each>
  </xsl:if>

  <p>_</p>
<!-- End div report -->
</div>

<div class="footer">
	Generated by
	<xsl:element name="a">
		<xsl:attribute name="href"><xsl:value-of select="/QCReport/GeneratorWebsite"/></xsl:attribute>Aozan</xsl:element>
	(version <xsl:value-of select="/QCReport/GeneratorVersion"/>)

</div>
</body>
</html>
</xsl:template>
</xsl:stylesheet>
