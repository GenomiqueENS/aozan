package fr.ens.transcriptome.aozan.util;

import com.google.common.base.Splitter;

public class StringUtils {

  /** Splitter. */
  public static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults()
      .omitEmptyStrings();
}
