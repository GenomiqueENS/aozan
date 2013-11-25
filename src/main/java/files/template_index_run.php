<html>
<head>
<title>Run <? echo $run_id?></title>
<style>
body {
	font-family: sans-serif;
}

div {
	margin: 10px;
}

img {
	width: 8%;
}

h1 {
	
}

h2 {
	
}
</style>
</head>
<body>

	<?php
	echo "<div id=\"banner\">\n";
	echo "<h1>\n";
	echo "<a href=\"http://www.transcriptome.ens.fr\" id=\"bannerLeft\"><img src=\"http://www.transcriptome.ens.fr/aozan/images/logo_genomicpariscentre-90pxh.png\" alt=\"logo genomic paris centre\"/></a>\n";

	echo "<center>Run ". $run_id ." reports</center>\n";
	echo "</h1>\n";
	echo "</div>\n";

	// Section sync if
	if (file_exists('./report_' . $run_id)){
		echo "<h2>HiSeq reports</h2>\n";

		echo "<ul>\n";
		echo "<li><a href=\"report_". $run_id ."/First_Base_Report.htm\">First base report</a></li>\n";

		// Section sync if report_run_id directory exists, add links
		if (file_exists('./report_' . $run_id .'/reports')){

			echo "<li><a href=\"report_". $run_id ."/Status.htm\">Run info</a></li>\n";
			echo "<li><a href=\"report_". $run_id .".tar.bz2\">All reports(compressed archive)</a></li>\n";
		}

		echo "<li><a href=\"hiseq_log_". $run_id .".tar.bz2\">HiSeq log (compressed archive)</a></li>\n";
		echo "</ul>\n";
	}


	if (file_exists('./basecall_stats_' . $run_id)){
		echo "<h2>Demultiplexing reports</h2>\n";

		echo "<ul>\n";
		echo "<li><a href=\"basecall_stats_". $run_id ."/All.htm\">All</a></li>\n";
		echo "<li><a href=\"basecall_stats_". $run_id ."/IVC.htm\">IVC</a></li>\n";
		echo "<li><a href=\"basecall_stats_". $run_id ."/Demultiplex_Stats.htm\">Demultiplex stats</a></li>\n";
		echo "<li><a href=\"basecall_stats_". $run_id .".tar.bz2\">All reports(compressed archive)</a></li>\n";
		echo "</ul>\n";
	}

	if (file_exists('./qc_' . $run_id)){
		echo "<h2>Quality control reports</h2>\n";

		echo "<ul>\n";
		echo "<li><a href=\"qc_". $run_id ."/". $run_id .".html\">QC report</a></li>\n";
		echo "</ul>\n";
	}
	?>
</body>
</html>
