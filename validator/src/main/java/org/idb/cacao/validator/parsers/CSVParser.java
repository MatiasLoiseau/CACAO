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
package org.idb.cacao.validator.parsers;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentInput;
import org.idb.cacao.api.templates.DocumentInputFieldMapping;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

/**
 * Implements {@link FileParser} interface to parse CSV files. <br>
 * <br>
 * IMPORTANT: CSV file must use comma "," ou TAB '\t' as delimiter. <br> 
 * 			  None of these characters can be used in field values.
 *			  Fields can be enclosed in double quotes: aa,bb,"cc,cd",dd 
 *			  Lines must be separated br CR LF ("\r\n")
 *			  Empty lines are accepted
 *  
 * @author Rivelino Patrício
 * 
 * @since 15/11/2021
 *
 */
public class CSVParser implements FileParser {
	
	/**
	 * Path for file in system storage
	 */
	private Path path;
	
	/**
	 * Document with field specifications
	 */
	private DocumentInput documentInputSpec;
	
	/**
	 * Scanner to iterate over file lines
	 */
	private Scanner scanner;
	
	/**
	 * Field positions relative to column positions
	 */
	private Map<String,Integer> fieldPositions;
	
	private static CsvParser csvParser;
	
	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#setPath(java.nio.file.Path)
	 */
	@Override
	public void setPath(Path path) {
		this.path = path;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#getDocumentInputSpec()
	 */
	@Override
	public DocumentInput getDocumentInputSpec() {
		return documentInputSpec;
	}

	/*
	 * (non-Javadoc)
	 * @see org.idb.cacao.validator.parsers.FileParser#setDocumentInputSpec(org.idb.cacao.api.templates.DocumentInput)
	 */
	@Override
	public void setDocumentInputSpec(DocumentInput inputSpec) {
		this.documentInputSpec = inputSpec;
	}

	@Override
	public void start() {
		if ( path == null || !path.toFile().exists() ) {		
			return;			
		}			
		
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
			}
		}
		
		try {
			scanner = new Scanner(path.toFile());
			
			//Read first line and set field positions according with field mapping atributtes
			String firstLine = scanner.nextLine();
			if ( firstLine != null && !firstLine.isEmpty() ) {
				
				String[] parts = readLine(firstLine);
			
				//Get original column positions
				Map<String,Integer> columnPositions = new LinkedHashMap<>();
				fieldPositions = new LinkedHashMap<>();
				
				for ( int i = 0; i < parts.length; i++ )
					columnPositions.put(parts[i], i);
				
				//Check all field mappings and set it's corresponding column
				for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
					
					if ( fieldMapping.getColumnIndex() != null && fieldMapping.getColumnIndex() >= 0 ) {
						fieldPositions.put(fieldMapping.getFieldName(), fieldMapping.getColumnIndex());
						continue;
					}
						
					String expression = fieldMapping.getColumnNameExpression();
					if ( expression != null && !expression.isEmpty() ) {							
						Integer position = ValidationContext.matchExpression(columnPositions.entrySet(), Map.Entry::getKey, expression).map(Map.Entry::getValue).orElse(null);
						if ( position != null )
							fieldPositions.put(fieldMapping.getFieldName(), position);
					}					
					
				}
				
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	
	@Override
	public DataIterator iterator() {
		
		if ( path == null || !path.toFile().exists() ) {		
			return null;			
		}			
		
		if ( scanner == null ) {
			start();
		}
		
		if ( scanner == null )
			return null;
		
		try {			
			
			return new DataIterator() {
				
				@Override
				public Map<String, Object> next() {
					String line = scanner.nextLine();
					
					if ( line != null ) {					
						
						String[] parts = readLine(line);
						
						Map<String,Object> toRet = new LinkedHashMap<>();
						
						for ( DocumentInputFieldMapping fieldMapping : documentInputSpec.getFields() ) {
							
							int position =  fieldPositions.getOrDefault(fieldMapping.getFieldName(), -1);
							
							if ( position < 0 ) {
								toRet.put(fieldMapping.getFieldName(), null);
							}
							else {
								String value = parts.length > position ? parts[position] : null;
								toRet.put(fieldMapping.getFieldName(), value);
							}
							
						}
						
						return toRet;
						
					}
					return null;
				}
				
				@Override
				public boolean hasNext() {					
					return scanner.hasNextLine();
				}
				
				@Override
				public void close() {
					scanner.close();					
				}
			}; 
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();			
		}
		
		return null;
	}

	@Override
	public void close() {
		if ( scanner != null ) {
			try {
				scanner.close();
			} catch (Exception e) {
			}
		}
	}
	
	/**
	 * Parse a CSV line and returns an array of String with line values
	 * @param line	Line to be parsed
	 * @return	An array of String with line values
	 */
	private static String[] readLine(String line) {
		
		if ( csvParser == null ) {
			// creates a CSV parser
			CsvParserSettings settings = new CsvParserSettings();
			settings.detectFormatAutomatically();
			csvParser = new CsvParser(settings);
		}
		
		return csvParser.parseLine(line);
	}

}
