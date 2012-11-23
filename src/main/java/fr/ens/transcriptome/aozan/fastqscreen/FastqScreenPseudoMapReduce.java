/*                  Aozan development code 
 * 
 * 
 * 
 */

package fr.ens.transcriptome.aozan.fastqscreen;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.google.common.math.DoubleMath;

import fr.ens.transcriptome.eoulsan.Globals;
import fr.ens.transcriptome.eoulsan.bio.FastqFormat;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.AbstractBowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.bio.readsmappers.BowtieReadsMapper;
import fr.ens.transcriptome.eoulsan.util.PseudoMapReduce;
import fr.ens.transcriptome.eoulsan.util.Reporter;

public class FastqScreenPseudoMapReduce extends PseudoMapReduce {

	/** Logger */
	private static final Logger LOGGER = Logger.getLogger(Globals.APP_NAME);
	protected static final String COUNTER_GROUP = "reads_mapping";

	private final int NB_STAT_VALUES = 5;

	private String genomeReference;
	private Map<String, float[]> percentHitsPerGenome = new HashMap<String, float[]>();
	private float readsprocessed;

	private boolean succesMapping = false;
	private boolean tableStatisticExist = false;
	private int nbReadMapped = 0;
	private Pattern pattern = Pattern.compile("\t");

	
	
	private final AbstractBowtieReadsMapper bowtie;;
	private final Reporter reporter;
	
	
	// TODO : override method doMap() of PseudoMapReduce
	public void doMap(File readsFile, List<File[]> listGenome)
			throws IOException {

		final int mapperThreads = 1;

		// Mapper change defaults arguments
		// seed use by bowtie already define to 40
		final String newArgumentsMapper = "-l 40 --chunkmbs 512 ";
		FastsqScreenSAMParser parser;
		
		for (File[] genome : listGenome) {
			parser = new FastsqScreenSAMParser(this.getMapOutputTempFile(),
					genome[1].getName());
			this.setGenomeReference(genome[1].getName());
			
			// System.out.println("nom genome : "+genome[0].getName()+" chemin genome "+genome[1].getAbsolutePath());

			bowtie.init(false, FastqFormat.FASTQ_SANGER, genome[0], genome[1],
					reporter, COUNTER_GROUP);
			bowtie.setMapperArguments(newArgumentsMapper);
			bowtie.setThreadsNumber(mapperThreads);

			bowtie.map(readsFile, parser);

			parser.closeMapOutputFile();
		}
	} // doMap

	/**
	 * Mapper Receive value in SAM format, only the read mapped are added in
	 * output with genome reference used
	 * 
	 * @param value
	 *            input of the mapper
	 * @param output
	 *            List of output of the mapper
	 * @param reporter
	 *            reporter
	 * @throws IOException
	 *             if an error occurs while executing the mapper
	 */
	public void map(final String value, final List<String> output,
			final Reporter reporter) throws IOException {

		if (value == null || value.length() == 0 || value.charAt(0) == '@')
			return;

		succesMapping = true;
		String[] tokens = pattern.split(value, 3);
		String nameRead = null;

		// flag of SAM format are in case 2 and flag = 4 for read unmapped
		if (Integer.parseInt(tokens[1]) != 4)
			nameRead = tokens[0];

		if (nameRead == null)
			return;
		output.add(nameRead + "\t" + genomeReference);
		// System.out.println("output map " + output);

	}// map

	/**
	 * Reducer Receive for each read list mapped genome
	 * 
	 * @param key
	 *            input key of the reducer
	 * @param values
	 *            values for the key
	 * @param output
	 *            list of output values of the reducer : here not use
	 * @param reporter
	 *            reporter
	 * @throws IOException
	 *             if an error occurs while executing the reducer
	 */
	public void reduce(final String key, Iterator<String> values,
			final List<String> output, final Reporter reporter)
			throws IOException {

		boolean oneHit = true;
		boolean oneGenome = true;
		String genomeCourant = null;
		String genomePrecedent = values.next();

		nbReadMapped++;

		//System.out.println("reduce : value of key "+ key);
		
		while (values.hasNext()) {

			genomeCourant = values.next();
			//System.out.println(genomeCourant+" idem "+genomePrecedent+" : "+
			// genomeCourant.equals(genomePrecedent));

			if (genomeCourant.equals(genomePrecedent)) {
				oneHit = false;
			} else {
				oneGenome = false;
				countHitPerGenome(genomePrecedent, oneHit, oneGenome);
				oneHit = true;
			}
			genomePrecedent = genomeCourant;
		}// while

		// last genome
		countHitPerGenome(genomePrecedent, oneHit, oneGenome);

	} // reduce

	/**
	 * Called by method reduce for each read and filled intermediate table
	 * 
	 * @param genome
	 * @param oneHit
	 * @param oneGenome
	 */
	void countHitPerGenome(String genome, boolean oneHit, boolean oneGenome) {
		// indices for table tabHitsPerLibraries
		// position 0 of the table for UNMAPPED ;

//		System.out.println("genome : "
//		+ genome + " hit " + oneHit + " gen " + oneGenome);

		final int ONE_HIT_ONE_LIBRARY = 1;
		final int MULTIPLE_HITS_ONE_LIBRARY = 2;
		final int ONE_HIT_MULTIPLE_LIBRARIES = 3;
		final int MUTILPLE_HITS_MULTIPLE_LIBRARIES = 4;
		float[] tab;
		// genome must be contained in map
		if (!(percentHitsPerGenome.containsKey(genome)))
			return;

		if (oneHit && oneGenome) {
			tab = percentHitsPerGenome.get(genome);
			tab[ONE_HIT_ONE_LIBRARY] += 1.0;

		} else if (!oneHit && oneGenome) {
			tab = percentHitsPerGenome.get(genome);
			tab[MULTIPLE_HITS_ONE_LIBRARY] += 1.0;

		} else if (oneHit && !oneGenome) {
			tab = percentHitsPerGenome.get(genome);
			tab[ONE_HIT_MULTIPLE_LIBRARIES] += 1.0;

		} else if (!oneHit && !oneGenome) {
			tab = percentHitsPerGenome.get(genome);
			tab[MUTILPLE_HITS_MULTIPLE_LIBRARIES] += 1.0;
		}
	}// countHitPerGenome

	/**
	 * calculating as a percentage, without rounding
	 */
	void getStatisticalTable() {

		if (nbReadMapped > readsprocessed)
			return;

		tableStatisticExist = true;

		for (Map.Entry<String, float[]> e : percentHitsPerGenome.entrySet()) {
			float unmapped = 100.f;
			float[] tab = e.getValue();

			for (int i = 1; i < tab.length; i++) {
				float n = tab[i] * 100.f / readsprocessed;
				tab[i] = n;
				unmapped -= n;
				// System.out.println("genome "
				// + e.getKey() + " i : " + i + " val : " + n + " unmap " +
				// unmapped);
			}
			tab[0] = unmapped;
		}
	}

	/**
	 * print table percent in format use by fastqscreen program
	 * 
	 * @return
	 */
	public String statisticalTableToString() {

		if (!succesMapping) {

			//return "ERROR mapping : no value receive ! (in method statisticalTableToString)";
		}

		if (!tableStatisticExist)
			getStatisticalTable();

		StringBuilder s = new StringBuilder(
				"Library \t %Unmapped \t %One_hit_one_library"
						+ "\t %Multiple_hits_one_library \t %One_hit_multiple_libraries \t "
						+ "%Multiple_hits_multiple_libraries");

		double percentHitNoLibraries = (readsprocessed - nbReadMapped)
				/ readsprocessed * 100.0;
		percentHitNoLibraries = ((int) (percentHitNoLibraries * 100)) / 100.0;
		for (Map.Entry<String, float[]> e : percentHitsPerGenome.entrySet()) {
			float[] tab = e.getValue();
			s.append("\n" + e.getKey());

			for (double n : tab) {
				// n = ((int) (n * 100.0)) / 100.0;
				// n = Math.ceil(n);
				n = DoubleMath.roundToInt((n * 100.0), RoundingMode.HALF_DOWN) / 100.0;
				s.append("\t" + n);
			}
		}

		s.append("\n\n% Hit_no_libraries : " + percentHitNoLibraries + "\n");
		return s.toString();
	}

	//
	// SETTERS
	//

	public void setGenomeReference(String genome) {
		this.genomeReference = genome;

		// update list genomeReference
		percentHitsPerGenome.put(genome, new float[NB_STAT_VALUES]);
	}

	//
	// CONSTRUCTOR
	//

	public FastqScreenPseudoMapReduce(int readsprocessed) {
		this.bowtie = new BowtieReadsMapper();
		this.reporter = new Reporter();

		this.readsprocessed = (float) readsprocessed;
	}
}
