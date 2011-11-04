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

package fr.ens.transcriptome.aozan.runsummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import fr.ens.transcriptome.aozan.runsummary.impl.RunSummaryImpl;
import fr.ens.transcriptome.eoulsan.EoulsanException;
import fr.ens.transcriptome.eoulsan.bio.BadBioEntryException;

public final class RunSummaryFactory {

  RunSummary createRunSummary(final File casavaDesign,
      final File casavaOutputDir, final File RTAOutputDir) throws IOException,
      ParserConfigurationException, SAXException, EoulsanException,
      BadBioEntryException {

    if (casavaDesign == null)
      throw new NullPointerException("casavaDesign is null.");
    if (casavaOutputDir == null)
      throw new NullPointerException("casavaOutputDir is null.");
    if (RTAOutputDir == null)
      throw new NullPointerException("RTAOutputDir is null.");

    if (!casavaDesign.exists())
      throw new FileNotFoundException("casavaDesign does not exists.");
    if (!casavaOutputDir.exists())
      throw new FileNotFoundException("casavaDesign does not exists.");
    if (!RTAOutputDir.exists())
      throw new FileNotFoundException("casavaDesign does not exists.");

    if (!casavaDesign.isFile())
      throw new FileNotFoundException("casavaDesign is not a standard file.");
    if (!casavaOutputDir.isDirectory())
      throw new FileNotFoundException("casavaDesign is not a directory.");
    if (!RTAOutputDir.isDirectory())
      throw new FileNotFoundException("RTAOutputDir is not a directory.");

    return new RunSummaryImpl(casavaDesign, casavaOutputDir, RTAOutputDir);
  }

}
