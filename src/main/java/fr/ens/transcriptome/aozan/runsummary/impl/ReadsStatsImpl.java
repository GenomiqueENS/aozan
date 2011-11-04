/*
 *                      Nividic development code
 *
 * This code may be freely distributed and modified under the
 * terms of the GNU Lesser General Public Licence.  This should
 * be distributed with the code.  If you do not have a copy,
 * see:
 *
 *      http://www.gnu.org/copyleft/lesser.html
 *
 * Copyright for this code is held jointly by the microarray platform
 * of the École Normale Supérieure and the individual authors.
 * These should be listed in @author doc comments.
 *
 * For more information on the Nividic project and its aims,
 * or to join the Nividic mailing list, visit the home page
 * at:
 *
 *      http://www.transcriptome.ens.fr/nividic
 *
 */

package fr.ens.transcriptome.aozan.runsummary.impl;

import fr.ens.transcriptome.aozan.runsummary.ReadsStats;

class ReadsStatsImpl implements ReadsStats {

  private String name;

  int total;
  int passingFilters;
  int q30;
  int mapped;

  boolean mappedData = false;

  //
  // Getters
  //

  @Override
  public String getName() {

    return this.name;
  }

  @Override
  public int getTotalReads() {

    return this.total;
  }

  @Override
  public int getPassingFilterReads() {

    return this.passingFilters;
  }

  @Override
  public int getQ30Reads() {

    return this.q30;
  }

  @Override
  public int getMappedReads() {

    return this.mapped;
  }

  //
  // Other methods
  //

  public void add(final ReadsStatsImpl rs) {

    if (rs == null)
      return;

    this.total += rs.total;
    this.passingFilters += rs.passingFilters;
    this.q30 += rs.q30;
    this.mapped += rs.mapped;
  }

  public String header() {

    return name
        + "total\t" + name + "PF\t" + name + "%PF\t" + name + "Q30\t" + name
        + "% Q30"
        + (this.mappedData ? "\t" + name + "mapped\t" + name + "% mapped" : "");
  }

  public String values() {

    final StringBuilder sb = new StringBuilder();

    sb.append(total);
    sb.append('\t');

    sb.append(passingFilters);
    sb.append('\t');
    sb.append(String.format("%.02f%%", ((double) this.passingFilters)
        / (this.total) * 100.0));
    sb.append('\t');

    sb.append(q30);
    sb.append('\t');
    sb.append(String.format("%.02f%%", ((double) this.q30)
        / (this.total) * 100.0));

    if (this.mappedData) {

      sb.append('\t');

      sb.append(mapped);
      sb.append('\t');
      sb.append(String.format("%.02f%%", ((double) this.mapped)
          / (this.total) * 100.0));
    }

    return sb.toString();
  }

  //
  // Constructor
  //

  public ReadsStatsImpl(final String name) {

    if (name == null || name.trim().length() == 0)
      this.name = "";
    else
      this.name = name.trim() + " ";
  }

}