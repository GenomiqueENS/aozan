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

import fr.ens.transcriptome.aozan.AozanException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.NotFoundException;

/**
 * This class redefine methods or constructors from FastQC to provide access to
 * files in fastqc jar. Fix bug with FastQC version 0.10.1
 * @since 1.1
 * @author Laurent Jourdren
 * @author Sandrine Perrin
 */
public class RuntimePatchFastQC {

  /**
   * Add code at the beginning on the method
   * ContaminentFinder.findContaminantHit to call the version Aozan of this
   * method which had access to the contaminant list in fastqc jar.
   * @throws NotFoundException it occurs when receive signals that something
   *           could not be found.
   * @throws CannotCompileException thrown when bytecode transformation has
   *           failed.
   */
  public static void rewriteContaminantFinderMethod(final boolean asBlastToUse)
      throws NotFoundException, CannotCompileException {

    // Get the class to modify
    CtClass cc =
        ClassPool.getDefault().get(
            "uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminentFinder");

    // Check class not frozen
    if (!cc.isFrozen()) {
      // Retrieve the method to modify
      CtBehavior cb = cc.getDeclaredMethod("findContaminantHit");

      // Add code at the beginning of the method
      final String codeToAdd;

      if (asBlastToUse) {
        codeToAdd =
            "return fr.ens.transcriptome.aozan.fastqc.ContaminantFinder.findContaminantHit(sequence);";

      } else {
        codeToAdd =
            "if (contaminants == null) {\n contaminants = "
                + "fr.ens.transcriptome.aozan.fastqc.ContaminantFinder.makeContaminantList();\n}";
      }
      cb.insertBefore(codeToAdd);

      // Load the class by the ClassLoader
      cc.toClass();
    }
  }

  /**
   * Remove code from Report.HTMLReportArchive in constructor. Can also redefine
   * code in a heritage class HTMLReportArchiveAozan which had access the files
   * included in fastqc jar.
   * @throws NotFoundException it occurs when receive signals that something
   *           could not be found.
   * @throws CannotCompileException thrown when bytecode transformation has
   *           failed.
   */
  public static void modifyConstructorHtmlReportArchive()
      throws NotFoundException, CannotCompileException {

    // Get the class to modify
    CtClass cc =
        ClassPool.getDefault().get(
            "uk.ac.babraham.FastQC.Report.HTMLReportArchive");

    // Check class not frozen
    if (!cc.isFrozen()) {

      // Retrieve the constructor
      CtConstructor[] constructors = cc.getConstructors();

      // Modify constructor, it does nothing
      constructors[0].setBody(null);

      // Load the class by the ClassLoader
      cc.toClass();
    }
  }

  /**
   * Execute method who patch code from FastQC before call in Aozan
   * @throws AozanException throw an error occurs during modification bytecode
   *           fastqc
   */

  public static void runPatchFastQC() throws AozanException {
    runPatchFastQC(false);
  }

  /**
   * Execute method who patch code from FastQC before call in Aozan
   * @param asBlastToUse if blast will be used else false
   * @throws AozanException throw an error occurs during modification bytecode
   *           fastqc
   */
  public static void runPatchFastQC(final boolean asBlastToUse)
      throws AozanException {

    try {
      rewriteContaminantFinderMethod(asBlastToUse);

      // modifyConstructorHtmlReportArchive();

    } catch (NotFoundException e) {
      throw new AozanException(e);

    } catch (CannotCompileException e) {
      throw new AozanException(e);

    }

  }
}
