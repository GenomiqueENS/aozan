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
 *      http://outils.genomique.biologie.ens.fr/aozan
 *
 */

package fr.ens.biologie.genomique.aozan.fastqc;

import fr.ens.biologie.genomique.aozan.AozanException;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
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
   * Add code at the beginning on the method.
   * ContaminentFinder.findContaminantHit to call the version Aozan of this
   * method which had access to the contaminant list in fastqc jar.
   * @param useBlast the as blast to use
   * @throws CannotCompileException thrown when bytecode transformation has
   *           failed.
   */
  public static void rewriteContaminantFinderMethod(final boolean useBlast)
      throws CannotCompileException {

    try {

      // Get the class to modify
      final CtClass cc = ClassPool.getDefault()
          .get("uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminentFinder");

      // Check class not frozen
      if (cc != null && !cc.isFrozen()) {
        // Retrieve the method to modify
        final CtBehavior cb = cc.getDeclaredMethod("findContaminantHit");

        // Add code at the beginning of the method
        final String codeToAdd;

        if (useBlast) {
          codeToAdd =
              "return fr.ens.biologie.genomique.aozan.fastqc.ContaminantFinder.findContaminantHit(sequence);";

        } else {
          codeToAdd = "if (contaminants == null) {\n contaminants = "
              + "fr.ens.biologie.genomique.aozan.fastqc.ContaminantFinder.makeContaminantList();\n}";
        }
        cb.insertBefore(codeToAdd);

        // Load the class by the ClassLoader
        cc.toClass();

      }
    } catch (final NotFoundException e) {
      // Nothing to do

    }
  }

  /**
   * Change superclass of Overrepresented Module from FastQC v0.11.2 to replace
   * a new class, it add a link html to ncbi website in result table.
   * @param useBlast true if use blast to search source on overepresented
   *          sequences
   * @throws CannotCompileException thrown when bytecode transformation has
   *           failed.
   */
  public static void changeSuperClassOverrepresentedModule(
      final boolean useBlast) throws CannotCompileException {

    if (!useBlast) {
      return;
    }

    try {
      // Get the class to modify
      final CtClass cc = ClassPool.getDefault()
          .get("uk.ac.babraham.FastQC.Modules.OverRepresentedSeqs");

      // Check class not frozen
      if (cc != null && !cc.isFrozen()) {

        final CtClass newSuperClazz = ClassPool.getDefault().get(
            "fr.ens.biologie.genomique.aozan.fastqc.AbstractQCModuleAozan");
        cc.setSuperclass(newSuperClazz);

        // cc.writeFile();

        // Load the class by the ClassLoader
        cc.toClass();
      }

    } catch (final NotFoundException e) {
      // Nothing to do
    }
  }

  /**
   * Execute method who patch code from FastQC before call in Aozan.
   * @throws AozanException throw an error occurs during modification bytecode
   *           fastqc
   */
  public static void runPatchFastQC() throws AozanException {
    runPatchFastQC(false);
  }

  /**
   * Execute method who patch code from FastQC before call in Aozan.
   * @param useBlast if blast will be used else false
   * @throws AozanException throw an error occurs during modification bytecode
   *           fastqc
   */
  public static void runPatchFastQC(final boolean useBlast)
      throws AozanException {

    try {
      rewriteContaminantFinderMethod(useBlast);

      changeSuperClassOverrepresentedModule(useBlast);

    } catch (final CannotCompileException e) {
      throw new AozanException(e);

    }

  }
}
