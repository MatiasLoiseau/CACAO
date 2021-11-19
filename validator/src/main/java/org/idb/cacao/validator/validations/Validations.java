/**
 * 
 */
package org.idb.cacao.validator.validations;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.idb.cacao.api.ValidationContext;
import org.idb.cacao.api.templates.DocumentField;
import org.idb.cacao.api.templates.DomainEntry;
import org.idb.cacao.api.templates.DomainTable;
import org.idb.cacao.api.templates.FieldType;
import org.idb.cacao.validator.repositories.DomainTableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.idb.cacao.api.utils.ParserUtils.*;

/**
 * A set of utilities for file content validation purpose.
 * 
 * @author Rivelino Patrício
 * 
 * @since 17/11/2021
 *
 */
@Component
public class Validations {

	private static final String FIELD_VALUE_NOT_FOUND = "Field value not found";

	private static final String FIELD_DOMAIN_VALUE_NOT_FOUND = "Field domain value not found";

	private static final String DOMAIN_TABLE_NOT_FOUND = "Domain table not found";
	
	/**
	 * Maximum length of field size saved to elasticsearch database
	 */
	private static final int MAX_ES_FIELD_SIZE = 1_000;

	@Autowired
	private DomainTableRepository domainTableRepository;

	/**
	 * Check for required fields in document uploaded. <br>
	 * 
	 * All validations error will be inserted on {@link ValidationContext#addAlert(String)}. <br>
	 * 
	 * @param validationContext
	 * 
	 */
	public void checkForRequiredFields(ValidationContext validationContext) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Get a list of required fields
		List<String> requiredFields = allFields.stream().filter(field -> field.getRequired())
				.map(field -> field.getFieldName()).collect(Collectors.toList());

		if (requiredFields == null || requiredFields.isEmpty())
			return;

		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		parsedContents.parallelStream().iterator().forEachRemaining(values -> {

			for (String fieldName : requiredFields) {
				if (values.get(fieldName) == null)
					addLogError(validationContext, fieldName, FIELD_VALUE_NOT_FOUND);
			}

		});

	}

	private synchronized void addLogError(ValidationContext validationContext, String field, String message) {
		validationContext.addAlert(message + ":" + field);
	}

	/**
	 * Check for data types in all fields in document uploaded
	 * 
	 * @param validationContext
	 * @return Boolean.TRUE if all data are compatible with field types.
	 *         Boolean.FALSE if not.
	 */
	public void checkForFieldDataTypes(ValidationContext validationContext) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		//Remove fields from validation
		List<DocumentField> fields = allFields.stream().filter(field -> (!FieldType.DOMAIN.equals(field.getFieldType())
				&& !FieldType.NESTED.equals(field.getFieldType()))).collect(Collectors.toList());

		if (fields == null || fields.isEmpty())
			return;

		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		parsedContents.parallelStream().iterator().forEachRemaining(values -> {

			for (DocumentField field : fields) {

				Object fieldValue = values.get(field.getFieldName());

				// If field value is null, there is nothing to check
				if (fieldValue == null)
					continue;
				
				if (FieldType.BOOLEAN.equals(field.getFieldType()))
					fieldValue = checkBooleanValue(fieldValue, validationContext);
				
				if (FieldType.CHARACTER.equals(field.getFieldType()))
					fieldValue = checkCharacterValue(field, fieldValue, validationContext);

				if (FieldType.DATE.equals(field.getFieldType()))
					fieldValue = checkDateValue(fieldValue, validationContext);

				if (FieldType.DECIMAL.equals(field.getFieldType()))
					fieldValue = checkDecimalValue(fieldValue, validationContext);
				
				if (FieldType.GENERIC.equals(field.getFieldType()))
					fieldValue = checkGenericValue(field, fieldValue, validationContext);

				if (FieldType.INTEGER.equals(field.getFieldType()))
					fieldValue = checkIntegerValue(fieldValue, validationContext);

				if (FieldType.MONTH.equals(field.getFieldType()))
					fieldValue = checkMonthValue(fieldValue, validationContext);

				if (FieldType.TIMESTAMP.equals(field.getFieldType()))
					fieldValue = checkTimestampValue(fieldValue, validationContext);

				// Update field value to it's new representation
				values.replace(field.getFieldName(), fieldValue, validationContext);

			}

		});
	}

	/**
	 * Validate and transform field value from {@link FieldType.GENERIC} 
	 * @param field
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */
	private Object checkGenericValue(DocumentField field, Object fieldValue, ValidationContext validationContext) {
		
		if (fieldValue == null)
			return null;
		
		String value = ValidationContext.toString(fieldValue);
		
		Integer maxLength = field.getMaxLength();
		
		if ( maxLength != null && maxLength.intValue() > 0 ) {
			value = value.substring(0, maxLength);	
		}
		else {		
			if ( value.length() > MAX_ES_FIELD_SIZE )
				value = value.substring(0, MAX_ES_FIELD_SIZE);
		}
		
		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.CHARACTER} 
	 * @param field
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */	
	private Object checkCharacterValue(DocumentField field, Object fieldValue, ValidationContext validationContext) {
		if (fieldValue == null)
			return null;
		
		String value = ValidationContext.toString(fieldValue);
		
		Integer maxLength = field.getMaxLength();
		
		if ( maxLength != null && maxLength.intValue() > 0 ) {
			value = value.substring(0, maxLength);	
		}
		else {		
			if ( value.length() > MAX_ES_FIELD_SIZE )
				value = value.substring(0, MAX_ES_FIELD_SIZE);
		}
		
		return value;
	}

	/**
	 * Validate and transform field value from {@link FieldType.TIMESTAMP} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */		
	private Object checkTimestampValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	/**
	 * Validate and transform field value from {@link FieldType.MONTH} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */		
	private Object checkMonthValue(Object fieldValue, ValidationContext validationContext) {
		// TODO Auto-generated method stub
		return fieldValue;
	}

	/**
	 * Validate and transform field value from {@link FieldType.INTEGER} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */	
	private Object checkIntegerValue(Object fieldValue, ValidationContext validationContext) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Integer || fieldValue.getClass().isAssignableFrom(int.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		if (isInteger(value))
			return Long.parseLong(value);

		// TODO check other situations
		return fieldValue;

	}

	/**
	 * Validate and transform field value from {@link FieldType.DECIMAL} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */
	private Object checkDecimalValue(Object fieldValue, ValidationContext validationContext) {

		if (fieldValue == null)
			return null;

		String value = ValidationContext.toString(fieldValue);

		if (isDecimal(value))
			return Double.parseDouble(value);

		if (isDecimalWithComma(value))
			return Double.parseDouble(value.replace(".", "").replace(",", "."));

		// TODO check other situations
		return fieldValue;
	}

	/**
	 * Validate and transform field value from {@link FieldType.DATE} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */
	private Object checkDateValue(Object fieldValue, ValidationContext validationContext) {

		if (fieldValue == null)
			return null;

		String value = ValidationContext.toString(fieldValue);

		if (isMDY(value))
			return formatTimestamp(parseMDY(value));
		if (isDMY(value))
			return formatTimestamp(parseDMY(value));
		if (isYMD(value))
			return formatTimestamp(parseYMD(value));

		// TODO check other situations
		return fieldValue;
	}

	/**
	 * Validate and transform field value from {@link FieldType.BOOLEAN} 
	 * @param fieldValue
	 * @param validationContext
	 * @return	Validated and transformed field value
	 */	
	private Object checkBooleanValue(Object fieldValue, ValidationContext validationContext) {
		if (fieldValue == null)
			return null;

		if (fieldValue instanceof Boolean || fieldValue.getClass().isAssignableFrom(boolean.class))
			return fieldValue;

		String value = ValidationContext.toString(fieldValue);

		// TODO check other situations
		if (isBoolean(value))
			return Boolean.parseBoolean(value);

		return fieldValue;

	}

	/**
	 * Check if values provided on fields that points to a domain table are present
	 * on domain tables entries.
	 * 
	 * @param validationContext
	 */
	public void checkForDomainTableValues(ValidationContext validationContext) {

		// Get a list of fields
		List<DocumentField> allFields = validationContext.getDocumentTemplate().getFields();

		if (allFields == null || allFields.isEmpty())
			return;

		// Remove CHARACTER fields from validation
		List<DocumentField> fields = allFields.stream().filter(field -> FieldType.DOMAIN.equals(field.getFieldType()))
				.collect(Collectors.toList());

		if (fields == null || fields.isEmpty())
			return;

		// Get a list of previous parsed contents
		List<Map<String, Object>> parsedContents = validationContext.getParsedContents();

		if (parsedContents == null || parsedContents.isEmpty())
			return;

		// Keeps all needed domain tables in memory
		Map<String, DomainTable> domainTables = new LinkedHashMap<>();

		// Check all records
		parsedContents.stream().iterator().forEachRemaining(values -> {

			// Check all fields for a specific record
			for (DocumentField field : fields) {

				// Get field value
				Object fieldValue = values.get(field.getFieldName());

				// TODO - check this rule
				// If field value is null, there is nothing to check
				if (fieldValue == null)
					continue;

				// Get domain table
				DomainTable table = getDomainTable(field, domainTables);

				// If domain table wasn't not found, add an error message
				if (table == null) {
					addLogError(validationContext, field.getFieldName(), DOMAIN_TABLE_NOT_FOUND + ": "
							+ field.getDomainTableName() + " version " + field.getDomainTableVersion());
					continue;
				}

				// Check field value against domain table entries
				// If value is not present on table, add an error message
				Pair<Boolean, String> result = checkDomainValue(fieldValue, table);

				// If value is not present at domain table entries, add error message
				if (!result.getKey()) {
					addLogError(validationContext, field.getFieldName(),
							FIELD_DOMAIN_VALUE_NOT_FOUND + ": " + fieldValue);
				} else {
					String newValue = result.getValue();
					// If value need to be updated, chave value in record
					if (newValue != null)
						values.replace(field.getFieldName(), newValue);
				}

			}
		});
	}

	/**
	 * Check if a specific given value is present on a given table. <br>
	 * 
	 * @param fieldValue
	 * @param table
	 * @return	A {@link Pair} where key is a boolean that indicates if value is present in domain table, and the value 
	 * 			is a key value for a given description, in case the provided value is not the key. 
	 */
	private Pair<Boolean, String> checkDomainValue(Object fieldValue, DomainTable table) {

		if (fieldValue == null || table == null)
			return Pair.of(Boolean.TRUE, null);

		String value = ValidationContext.toString(fieldValue);

		if (table.getEntry(value) == null) {
			DomainEntry entry = table.getEntries().parallelStream()
					.filter(e -> e.getDescription().equalsIgnoreCase(value)).findFirst().orElse(null);
			if (entry != null) {
				return Pair.of(Boolean.TRUE, entry.getKey());
			}
		} else {
			return Pair.of(Boolean.TRUE, null);
		}

		return Pair.of(Boolean.FALSE, null);
	}

	/**
	 * Find and return a {@link DomainTable} for a given {@link DocumentField}. <br>
	 * 
	 * @param field        The {@link DocumentField} with information about name and version. <br>
	 * @param domainTables A {@link Map} with all domain tables needed. <br>
	 * @return A domain table for name and version specifieds in field or null if table doesn't exist. <br>
	 */
	private synchronized DomainTable getDomainTable(DocumentField field, Map<String, DomainTable> domainTables) {

		if (domainTables == null)
			domainTables = new LinkedHashMap<>();

		DomainTable table = domainTables.get(field.getFieldName());

		if (table != null)
			return table;

		String tableName = field.getDomainTableName();
		String tableVersion = field.getDomainTableVersion();

		table = domainTableRepository.findByNameAndVersion(tableName, tableVersion).orElse(null);
		domainTables.put(field.getFieldName(), table);
		return table;

	}
}
