<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">

<xsl:template match="/">
<xsl:decimal-format name="aozan" decimal-separator="." grouping-separator=" "/>
<xsl:decimal-format name="thousand" decimal-separator="." grouping-separator=" "/>

<html>
<head>
  <title>fastqscreen <xsl:value-of select="/ReportFastqScreen/sampleName"/></title>
  <style TYPE="text/css">
    td {
      text-align: center;
      width: 30px;
    }
  </style>
</head>

<body>

  <h3>Detection contamination report</h3>
  <ul>
    <li><b>Run Id : </b> <xsl:value-of select="/ReportFastqScreen/RunId"/></li>
    <li><b>Project : </b> <xsl:value-of select="/ReportFastqScreen/projectName"/></li>
    <li><b>Sample : </b> <xsl:value-of select="/ReportFastqScreen/sampleName"/></li>
    <li><b>Genome sample : </b> <xsl:value-of select="/ReportFastqScreen/genomeSample"/></li>
    <li><b>Creation date : </b> <xsl:value-of select="/ReportFastqScreen/dateReport"/></li>
  </ul>

  <table border="1">
    <tr>
      <xsl:for-each select="/ReportFastqScreen/Report/Columns/Column">
        <th><xsl:value-of select="@name"/></th>
      </xsl:for-each>
    </tr>
   
   <xsl:for-each select="/ReportFastqScreen/Report/Genomes/Genome">
   <tr>
      <td><xsl:value-of select="@name"/></td>
      <xsl:for-each select="Value">
         <td><xsl:value-of select="format-number(.,'#0.00','aozan')"/> %</td>
      </xsl:for-each>
   </tr>
   </xsl:for-each>
   </table>
   
   <p>
   <ul>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsUnmapped/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsUnmapped,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedOneGenome/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedOneGenome,'#0.00','aozan')"/> %</li>
     <li><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample,'#0.00','aozan')"/> %</li>
   
     <li><xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMapped,'# ##0','thousand')"/>&#160;  
       <xsl:value-of select="/ReportFastqScreen/Report/ReadsMapped/@name"/>  / 
       <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsProcessed,'# ##0','thousand')"/>&#160;
       <xsl:value-of select="/ReportFastqScreen/Report/ReadsProcessed/@name"/>  </li>
   </ul>
   </p>
</body>
</html>
</xsl:template>
</xsl:stylesheet>