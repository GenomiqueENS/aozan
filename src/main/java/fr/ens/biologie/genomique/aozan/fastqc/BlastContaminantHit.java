package fr.ens.biologie.genomique.aozan.fastqc;

import static fr.ens.biologie.genomique.aozan.util.StringUtils.stackTraceToString;

import java.io.IOException;

import fr.ens.biologie.genomique.aozan.AozanException;
import fr.ens.biologie.genomique.aozan.Common;
import uk.ac.babraham.FastQC.Sequence.Contaminant.ContaminantHit;

/**
 * This class define a Blast contiminant Hit. This class allow to postpone the
 * blast of sequences with no hit to reduce the number of execution of Blast.
 * @author Laurent Jourdren
 * @since 2.0
 */
public class BlastContaminantHit extends ContaminantHit {

  private final String sequence;

  public BlastContaminantHit(final String sequence) {

    super(null, 1, 0, 0);

    this.sequence = sequence;

    // Submit the sequence
    OverrepresentedSequencesBlast.getInstance().submitSequence(sequence);
  }

  @Override
  public String toString() {

    try {

      // Get the blast result
      final BlastResultHit result =
          OverrepresentedSequencesBlast.getInstance().getResult(this.sequence);

      // Return the result
      if (result != null) {
        return result.toContaminantHit().toString();
      }

    } catch (final IOException | AozanException e) {

      Common.getLogger().warning("Error during find contaminant with blast : "
          + e.getMessage() + "\n" + stackTraceToString(e));
    }

    // If an error occurs or if blast is disabled
    return "No Hit";
  }

}
