<?xml version="1.0"?>
<!DOCTYPE html>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" version="1.0" encoding="UTF-8" indent="yes" omit-xml-declaration="no" 
doctype-system="about:legacy-compat"/>

<xsl:template match="/">
<xsl:decimal-format name="aozan" decimal-separator="." grouping-separator=" "/>
<xsl:decimal-format name="thousand" decimal-separator="." grouping-separator=" "/>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>fastqscreen <xsl:value-of select="/ReportFastqScreen/sampleName"/></title>
   <style type="text/css">
   
    #genomeSample{
    	color:#9B1319;
    	font-style : bold;
    }
    
    tr.sampleDataTab:hover {
      z-index:2;
      box-shadow:0 0 12px rgba(0, 0, 0, 1);
      background:#F6F6B4;
    }
   	
   	td.sampleDataTab {
   		width:90px
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
    div.report {
	    display:block;
	    position:absolute;
	    width:100%;
	    top:5.2em;
	    bottom:5px;
	    left:0;
	    right:0;
	    padding:0 1em 0 1em;
	    background-color: white;
  	}
    div.projectReport {
	    display:block;
	    position:absolute;
	    width:80%;
	    top:18em;
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
   	
  	.report_left{
      display:inline-block;
      float:left;
      clear:left;
      text-align:left;
      vertical-align:middle;
      width:auto;
	  width:60%;
      padding-bottom:3em;
  	}

  	.report_right{
      display:inline-block;
      float:right;
      clear:right;
      text-align:left;
      vertical-align:middle;
      width:auto;
   	  width:40%;
      padding-bottom:3em;
    }

   	
   	.linkSample {
    border-top-left-radius:24px;
    border-top-right-radius:24px;
    border-bottom-right-radius:24px;
    border-bottom-left-radius:24px;

    background-color:#ffffff;
    text-indent:0px;
    border:3px solid #000000;
    display:inline-block;
    font-family:Arial;
    font-size:12px;
    font-weight:bold;
    font-style:normal;
    line-height:29px;
    text-decoration:none;
    text-align:center;
    padding-left:5px;
    padding-right:5px;
	white-space:nowrap;
    }

    #goToSample td{
    border:#ffffff;
    border-style:hidden;
    }
   	  	
   	div.runReport{
    border:0;
   	clear:inherit;
    margin:0;
    width:100%;
    height: 17em;
    background-color: white;
    position: fixed;
    z-index:2;	
   	}
  </style>
  
    <script type="text/javascript">
	<xsl:comment><![CDATA[	
		
        function goto(){
            // Decallage de 2 pour corriger l'affichage
            var index = document.forms.sampleName.nom.options.selectedIndex - 2;
            var anchor = document.forms.sampleName.nom.options[index].text;

            window.location.href = "#"+anchor;
        }
		
	]]></xsl:comment>
	</script>
	
</head>

<body>
  <div class="header">
  	<div id="header_title">
		<img src="http://www.transcriptome.ens.fr/aozan/images/logo_aozan_qc.png" alt="Aozan"/>  
		Detection contamination report for project <b><xsl:value-of select="/ReportFastqScreen/projectName"/></b>
	</div>   
  </div>
<a name="top"></a>
 
  <div class="report">
 
  <div class="runReport">
  	  	
  <ul>
    <li><b>Run Id: </b> <xsl:value-of select="/ReportFastqScreen/RunId"/></li>
    <li><b>Flow cell: </b> <xsl:value-of select="/ReportFastqScreen/FlowcellId"/></li>
    <li><b>Date: </b> <xsl:value-of select="/ReportFastqScreen/RunDate"/></li>
    <li><b>Instrument S/N: </b> <xsl:value-of select="/ReportFastqScreen/InstrumentSN"/></li>
    <li><b>Instrument run number: </b> <xsl:value-of select="/ReportFastqScreen/InstrumentRunNumber"/></li>
    <li><b>Generated by: </b> <xsl:value-of select="/ReportFastqScreen/GeneratorName"/> version <xsl:value-of select="/ReportFastqScreen/GeneratorVersion"/></li>
    <li><b>Creation date: </b> <xsl:value-of select="/ReportFastqScreen/ReportDate"/></li>
    <li><b>Project : </b> <xsl:value-of select="/ReportFastqScreen/projectName"/></li>
    <li><b>Genome : </b> <xsl:value-of select="/ReportFastqScreen/genomeSample"/></li>
  </ul>

  	<form name="sampleName">
		<label>Display a sample : </label>
		<select name="nom" size="1" onchange="goto()">
  			<xsl:for-each select="/ReportFastqScreen/project/sample">
				<option><xsl:value-of select="sampleName"/></option>
			</xsl:for-each>
		</select>
  	</form>
  	
	<!-- table id="goToSample">
	<tr class="linkRow">
  		<xsl:for-each select="/ReportFastqScreen/project/sample">
		<td><a class="linkSample" href="#{sampleName}"><xsl:value-of select="sampleName"/></a></td>
  		</xsl:for-each>
  	</tr>
  </table -->
  <!-- end run report -->
  </div>

  <div class="projectReport">  
  
    <xsl:for-each select="/ReportFastqScreen/project/sample">
    <div class="report_left">
    <h3>Sample: <xsl:value-of select="sampleName"/></h3>
   		<a id="{sampleName}"></a>
 
 <table border="1" class="sampleDataTab">
    <tr>
      <xsl:for-each select="Report/Columns/Column">
        <th><xsl:value-of select="@name"/></th>
      </xsl:for-each>
    </tr>
   
    
   <xsl:for-each select="Report/Genomes/Genome">
   
    <xsl:choose>
   	  <xsl:when test="boolean(@name=/ReportFastqScreen/genomeSample)">
		   <tr id="genomeSample">
		   	  <td><xsl:value-of select="@name"/></td>
		      <xsl:for-each select="Value">
		       	 <td><xsl:value-of select="format-number(.,'#0.00','aozan')"/> %</td>
		      </xsl:for-each>
		  	</tr>
   		</xsl:when>
   		<xsl:otherwise>
		   <tr>
		      <td><xsl:value-of select="@name"/></td>
		      <xsl:for-each select="Value">
		         <td><xsl:value-of select="format-number(.,'#0.00','aozan')"/> %</td>
		      </xsl:for-each>
		   </tr>
   		</xsl:otherwise>
   	</xsl:choose>
   
   </xsl:for-each>
   </table>
    <!-- end report left -->    
    </div>

   <!-- a href="#top">TOP</a -->
  <div class="report_right">
  <ul>
    <li><b>Description: </b> <xsl:value-of select="/ReportFastqScreen/description"/></li>
  </ul>   
   <p>
   <ul>
     <li><xsl:value-of select="Report/ReadsUnmapped/@name"/> : <xsl:value-of select="format-number(Report/ReadsUnmapped,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="Report/ReadsMappedOneGenome/@name"/> : <xsl:value-of select="format-number(Report/ReadsMappedOneGenome,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="Report/ReadsMappedExceptGenomeSample/@name"/> : <xsl:value-of select="format-number(Report/ReadsMappedExceptGenomeSample,'#0.00','aozan')"/> %</li>
   
     <li><xsl:value-of select="format-number(Report/ReadsMapped,'# ##0','thousand')"/>&#160;  
       <xsl:value-of select="Report/ReadsMapped/@name"/>  / 
       <xsl:value-of select="format-number(Report/ReadsProcessed,'# ##0','thousand')"/>&#160;
       <xsl:value-of select="Report/ReadsProcessed/@name"/>  </li>
   </ul>
   </p>

        
    </div><!-- end report right -->
    </xsl:for-each>

  </div><!-- end project report -->
  </div><!-- end report -->
  
  <div class="footer">
	Generated by
	<xsl:element name="a">
		<xsl:attribute name="href"><xsl:value-of select="/ReportFastqScreen/GeneratorWebsite"/></xsl:attribute>Aozan</xsl:element>
	(version <xsl:value-of select="/ReportFastqScreen/GeneratorVersion"/>)

</div>
  
</body>
</html>
</xsl:template>
</xsl:stylesheet>
