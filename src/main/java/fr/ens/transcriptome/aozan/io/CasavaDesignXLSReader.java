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

package fr.ens.transcriptome.aozan.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import fr.ens.transcriptome.eoulsan.illumina.CasavaDesign;
import fr.ens.transcriptome.eoulsan.illumina.io.AbstractCasavaDesignTextReader;
import fr.ens.transcriptome.eoulsan.util.Utils;

public class CasavaDesignXLSReader extends AbstractCasavaDesignTextReader {

  private final InputStream is;

  public CasavaDesign read() throws IOException {

    // create a POIFSFileSystem object to read the data
    final POIFSFileSystem fs = new POIFSFileSystem(this.is);

    // Create a workbook out of the input stream
    final HSSFWorkbook wb = new HSSFWorkbook(fs);

    // Get a reference to the worksheet
    final HSSFSheet sheet = wb.getSheetAt(0);

    // When we have a sheet object in hand we can iterator on
    // each sheet's rows and on each row's cells.
    final Iterator<Row> rows = sheet.rowIterator();
    final List<String> fields = Utils.newArrayList();

    while (rows.hasNext()) {
      final HSSFRow row = (HSSFRow) rows.next();
      final Iterator<Cell> cells = row.cellIterator();

      while (cells.hasNext()) {
        final HSSFCell cell = (HSSFCell) cells.next();
        while (fields.size() != cell.getColumnIndex())
          fields.add("");

        fields.add(cell.toString());
      }

      // Parse the fields
      if (!isFieldsEmpty(fields))
        parseLine(fields);
      fields.clear();

    }

    this.is.close();

    return getDesign();
  }

  /**
   * Test if all the elements of a list are empty.
   * @param list the list to test
   * @return true if all the elements of the list are empty
   */
  private static final boolean isFieldsEmpty(final List<String> list) {

    if (list == null)
      return true;

    for (String e : list)
      if (e != null && !"".equals(e.trim()))
        return false;

    return true;
  }

  //
  // Contructors
  //

  /**
   * Public constructor
   * @param is InputStream to use
   */
  public CasavaDesignXLSReader(final InputStream is) {

    if (is == null)
      throw new NullPointerException("InputStream is null");

    this.is = is;
  }

  /**
   * Public constructor
   * @param file File to use
   */
  public CasavaDesignXLSReader(final File file) throws FileNotFoundException {

    if (file == null)
      throw new NullPointerException("File is null");

    if (!file.isFile())
      throw new FileNotFoundException("File not found: "
          + file.getAbsolutePath());

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor
   * @param filename Filename to use
   */
  public CasavaDesignXLSReader(final String filename)
      throws FileNotFoundException {

    this(new File(filename));
  }

}
