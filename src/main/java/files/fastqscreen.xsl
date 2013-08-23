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
  <h5>Project : <xsl:value-of select="/ReportFastqScreen/projectName"/></h5>
  <h5>Sample : <xsl:value-of select="/ReportFastqScreen/sampleName"/></h5>
  <h5>Genome sample : <xsl:value-of select="/ReportFastqScreen/genomeSample"/></h5>
  <h5>Creation date : <xsl:value-of select="/ReportFastqScreen/dateReport"/></h5>

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
   
   <p><xsl:value-of select="/ReportFastqScreen/Report/ReadsUnmapped/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsUnmapped,'#0.00','aozan')"/> %</p>
   <p><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedOneGenome/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedOneGenome,'#0.00','aozan')"/> %</p>
   <p><xsl:value-of select="/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample/@name"/> : <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMappedExceptGenomeSample,'#0.00','aozan')"/> %</p>
   
   <p>
    <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsMapped,'# ##0','thousand')"/>  
    <xsl:value-of select="/ReportFastqScreen/Report/ReadsMapped/@name"/>  
     / 
    <xsl:value-of select="format-number(/ReportFastqScreen/Report/ReadsProcessed,'# ##0','thousand')"/>  
    <xsl:value-of select="/ReportFastqScreen/Report/ReadsProcessed/@name"/>  </p>
</body>
</html>
</xsl:template>
</xsl:stylesheet>