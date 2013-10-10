/*
 *                  Aozan development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU General Public License version 3 or later 
 * and CeCILL. This should be distributed with the code. If you 
 * do not have a copy, see:
 *
 *      http://www.gnu.org/licenses/gpl-3.0-standalone.html
 *      http://www.cecill.info/licences/Licence_CeCILL_V2-en.html
 *
 * Copyright for this code is held jointly by the Genomic platform
 * of the Institut de Biologie de l'École Normale Supérieure and
 * the individual authors. These should be listed in @author doc
 * comments.
 *
 * For more information on the Aozan project and its aims,
 * or to join the Aozan Google group, visit the home page at:
 *
 *      http://www.transcriptome.ens.fr/aozan
 *
 */

package fr.ens.transcriptome.aozan.fastqc;

import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToDouble;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToInt;
import static fr.ens.transcriptome.aozan.util.XMLUtilsParser.extractFirstValueToString;

import org.w3c.dom.Element;

import uk.ac.babraham.FastQC.Sequence.Contaminant.Contaminant;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

/**
 * This class define a object which handles result xml file blastn for one
 * sequence query.
 * @since 1.2
 * @author Sandrine Perrin
 */
class BlastResultHit {

  static final String LINK_NCBI_BLASTN =
      "\"http://blast.ncbi.nlm.nih.gov/Blast.cgi?CMD=Web&amp;PAGE=Nucleotides&amp;DATABASE=nr&amp;QUERY=";

  // Parameters for return only the best hit
  static final int MIN_IDENTITY_EXPECTED = 100;
  static final int MAX_QUERYCOVERT_EXPECTED = 0;

  // print version blast BlastOutput_version
  static final String tag_hitDef = "Hit_def";
  static final String tag_hspBitScore = "Hsp_bit-score";
  static final String tag_hspEValue = "Hsp_evalue";
  static final String tag_hspIdentity = "Hsp_identity";
  static final String tag_hspAlignLen = "Hsp_align-len";
  static final String tag_hspQseq = "Hsp_qseq";

  final int queryLength;
  final String result;
  final double hspBitScore;
  final String hspEValue;
  final int hspIdentity;
  final int hspAlignLen;
  final int countHits;
  final String qSeq;
  final int prcIdentity;
  final int queryCover;
  final String sequence;

  /**
   * Create an object contaminant hit.
   * @return object contaminantHit
   */
  public ContaminantHit getContaminantHit() {

    StringBuilder name = new StringBuilder();
    name.append("Search with Blastall+, <a href="
        + LINK_NCBI_BLASTN + this.sequence + "\""
        + " target=\"_blank\">more detail</a>");
    name.append(" First hit on "
        + (countHits > 100 ? "+100" : countHits) + " : ");
    name.append(" <br/>");
    name.append(this.result);
    name.append(" <br/>");
    name.append(" Evalue=" + this.hspEValue + ", ");
    name.append(" Ident=" + prcIdentity + "%,");
    name.append(" QueryCovergap=" + this.queryCover + "%");

    // TODO
    System.out
        .println((this.prcIdentity < MIN_IDENTITY_EXPECTED || this.queryCover > MAX_QUERYCOVERT_EXPECTED)
            + "  " + name.toString());

    // Return only the best hit
    if (this.prcIdentity < MIN_IDENTITY_EXPECTED
        || this.queryCover > MAX_QUERYCOVERT_EXPECTED)
      return null;

    final Contaminant cont = new Contaminant(name.toString(), "");

    // Override method
    return new ContaminantHit(cont, 1, this.queryLength, this.prcIdentity) {

      @Override
      public String toString() {

        return cont.name();
      }
    };
  }

  //
  // Constructor
  //

  /**
   * Public constructor. Object contains all informations for one blast response
   * to a query.
   * @param hit first hit retrieved by blast
   * @param countHits number hits retrieved by blast
   * @param queryLength number base in sequence query
   * @param sequence query blast
   */
  public BlastResultHit(final Element hit, final int countHits,
      final int queryLength, final String sequence) {

    this.sequence = sequence;

    this.queryLength = queryLength;
    this.countHits = countHits;

    this.result = extractFirstValueToString(hit, tag_hitDef);
    this.hspBitScore = extractFirstValueToDouble(hit, tag_hspBitScore);
    this.hspEValue = extractFirstValueToString(hit, tag_hspEValue);
    this.hspIdentity = extractFirstValueToInt(hit, tag_hspIdentity);
    this.hspAlignLen = extractFirstValueToInt(hit, tag_hspAlignLen);
    this.qSeq = extractFirstValueToString(hit, tag_hspQseq);
    this.prcIdentity = (int) ((double) hspIdentity / this.queryLength * 100);

    int countGap = queryLength - hspAlignLen;
    this.queryCover = (int) ((double) (countGap / this.queryLength * 100));

  }

}
