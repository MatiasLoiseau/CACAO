/*******************************************************************************
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without 
 * restriction, including without limitation the rights to use, copy, modify, merge, publish, 
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or 
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS 
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN 
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.validator.fileformats;

import static org.idb.cacao.validator.fileformats.FileFormat.*;

import org.idb.cacao.api.templates.DocumentFormat;
import org.idb.cacao.validator.parsers.CSVParser;
import org.idb.cacao.validator.parsers.FileParser;

/**
 * FileFormat for text files associated to the 'Comma Separated Value' format (not only 'commas' may be used
 * as a separator).
 * 
 * @author Gustavo Figueiredo
 *
 */
public class CSVFileFormat implements FileFormat {

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#getFormat()
	 */
	@Override
	public DocumentFormat getFormat() {
		return DocumentFormat.CSV;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFilename(java.lang.String)
	 */
	@Override
	public Boolean matchFilename(String filename) {
		final String ext = getFileExtension(filename);
		if (ExcelFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| PDFFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| WordFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find()
			|| XMLFileFormat.COMMON_FILE_EXTENSION.matcher(ext).find())
			return false;

		return null; // for any other extension, may be a CSV or may be not
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#matchFileHeader(byte[], int)
	 */
	@Override
	public Boolean matchFileHeader(byte[] firstBytes, int firstBytesLength) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.fileformats.FileFormat#createFileParser()
	 */
	@Override
	public FileParser createFileParser() {
		return new CSVParser();
	}

}
