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

package fr.ens.biologie.genomique.aozan.illumina.samplesheet.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.google.common.math.DoubleMath;

import fr.ens.biologie.genomique.aozan.illumina.samplesheet.SampleSheet;

/**
 * This class reads a Bcl2fastq samplesheet file in xls format.
 * @since 0.1
 * @author Laurent Jourdren
 */
public class SampleSheetXLSReader implements SampleSheetReader {

  private final InputStream is;
  private int version = -1;

  @Override
  public SampleSheet read() throws IOException {

    final SampleSheetParser parser;

    switch (this.version) {

    case -1:
      parser = new SampleSheetDiscoverFormatParser();
      break;

    case 1:
      parser = new SampleSheetV1Parser();
      break;

    case 2:
      parser = new SampleSheetV2Parser();
      break;

    default:
      throw new IOException(
          "Unknown bcl2fastq samplesheet format version: " + this.version);
    }

    // create a POIFSFileSystem object to read the data
    final POIFSFileSystem fs = new POIFSFileSystem(this.is);

    // Create a workbook out of the input stream
    final HSSFWorkbook wb = new HSSFWorkbook(fs);

    // Get a reference to the worksheet
    final HSSFSheet sheet = wb.getSheetAt(0);

    // When we have a sheet object in hand we can iterator on
    // each sheet's rows and on each row's cells.
    final Iterator<Row> rows = sheet.rowIterator();
    final List<String> fields = new ArrayList<>();

    while (rows.hasNext()) {
      final HSSFRow row = (HSSFRow) rows.next();
      final Iterator<Cell> cells = row.cellIterator();

      while (cells.hasNext()) {
        final HSSFCell cell = (HSSFCell) cells.next();
        while (fields.size() != cell.getColumnIndex()) {
          fields.add("");
        }

        // Convert cell value to String
        fields.add(parseCell(cell));
      }

      // Parse the fields
      if (!isFieldsEmpty(fields)) {
        parser.parseLine(fields);
      }
      fields.clear();

    }

    wb.close();
    this.is.close();

    return parser.getSampleSheet();
  }

  /**
   * Parse the content of a cell.
   * @param cell cell to parse
   * @return a String with the cell content
   */
  private static String parseCell(final HSSFCell cell) {

    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      final double doubleValue = cell.getNumericCellValue();

      if (DoubleMath.isMathematicalInteger(doubleValue)) {
        return Long.toString((long) doubleValue);
      }
    }

    return cell.toString();
  }

  /**
   * Test if all the elements of a list are empty.
   * @param list the list to test
   * @return true if all the elements of the list are empty
   */
  private static boolean isFieldsEmpty(final List<String> list) {

    if (list == null) {
      return true;
    }

    for (final String e : list) {
      if (e != null && !"".equals(e.trim())) {
        return false;
      }
    }

    return true;
  }

  /**
   * Set the version of the samplesheet file to read.
   * @param version the version of the samplesheet file to read
   */
  public void setVersion(final int version) {

    this.version = version;
  }

  /**
   * Get the version of the samplesheet file to read.
   * @return the version of the samplesheet file to read
   */
  public int getVersion() {

    return this.version;
  }

  //
  // Constructors
  //

  /**
   * Public constructor.
   * @param is InputStream to use
   */
  public SampleSheetXLSReader(final InputStream is) {

    if (is == null) {
      throw new NullPointerException("InputStream is null");
    }

    this.is = is;
  }

  /**
   * Public constructor.
   * @param file File to use
   */
  public SampleSheetXLSReader(final File file) throws FileNotFoundException {

    if (file == null) {
      throw new NullPointerException("File is null");
    }

    if (!file.isFile()) {
      throw new FileNotFoundException(
          "File not found: " + file.getAbsolutePath());
    }

    this.is = new FileInputStream(file);
  }

  /**
   * Public constructor.
   * @param filename Filename to use
   */
  public SampleSheetXLSReader(final String filename)
      throws FileNotFoundException {

    this(new File(filename));
  }

}
