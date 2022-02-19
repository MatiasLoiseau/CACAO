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
package org.idb.cacao.web.controllers.rest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.idb.cacao.web.controllers.services.AnalysisService;
import org.idb.cacao.web.dto.AggregatedAccountingFlow;
import org.idb.cacao.web.dto.AnalysisData;
import org.idb.cacao.web.dto.StatementIncomeItem;
import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller class for all endpoints related to 'analysis' from taxpayers
 * 
 * @author Rivelino Patrício
 *
 */
@Api(value = "REST API for analysis over taxpayers information")
@RestController
@RequestMapping("/api")
@Tag(name="analysis-api-controller", description="Controller class for all endpoints related to 'TaxPayer' analysis interacting by a REST interface.")
public class AnalysisAPIController {

	private static final Logger log = Logger.getLogger(AnalysisAPIController.class.getName());
	
	private static final int VERTICAL = 1;
	private static final int HORIZONTAL = 2;
	private static final int BOTH = 3;

	@Autowired
	private AnalysisService analysisService;
	
	@Autowired
	private MessageSource messageSource;
	
	@ApiOperation(value = "Get values of vertical and horizontal analysis ", response = List.class, tags = "vertical_horizontal_analysis")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/vertical_horizontal_analysis"})
	public ResponseEntity<Object> getVerticalHorizontalAnalysis(
			@ApiParam(name = "taxpayerId", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@RequestParam("taxpayerId") String taxpayerId,
			@ApiParam(name = "finalDate", allowEmptyValue = false, allowMultiple = false, example = "Wed Dec 01 2021", required = true, type = "LocalDate")
			@RequestParam("finalDate") @DateTimeFormat(iso = ISO.DATE) LocalDate finalDate, 
			@ApiParam(name = "zeroBalance", allowEmptyValue = false, allowMultiple = false, example = "true", required = true, type = "Boolean")
			@RequestParam("zeroBalance") String zeroBalance, 
			@ApiParam(name = "comparisonPeriods", allowEmptyValue = false, allowMultiple = false, example = "1", required = true, type = "Integer", 
				allowableValues = "1=One month before, 2=Three months before, 3=Six months before, 4=Twelve months before, " +
						"11=One year before, 12=Two year before, 13=Three year before, 14=Four year before, 15=Five year before, " +
						"21=One month after, 22=Three months after, 32=Six months after, 24=Twelve months after, " +
						"31=One year after, 32=Two year after, 33=Three year after, 34=Four year after, 35=Five year after")
			@RequestParam("comparisonPeriods") int comparisonPeriods) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( finalDate == null /*|| finalDate.length() < 15*/ ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'finalDate'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}

		YearMonth period = YearMonth.from(finalDate); //ParserUtils.parseDayMonthDayYear(finalDate);
		
		boolean fetchZeroBalance = ( zeroBalance == null ? false : "true".equalsIgnoreCase(zeroBalance) );
		
		List<YearMonth> additionalPeriods = getAdditionalPeriods(period,comparisonPeriods);
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<Map<String,Object>> mapOfAccounts = analysisService.getMapOfAccounts(
    			taxpayerId,period,fetchZeroBalance,additionalPeriods);
	
    	return ResponseEntity.ok().body(mapOfAccounts);    	
	}

	/**
	 * Given a month and information about comparison periods, creates and returns a {@link List}
	 * of {@link YearMonth} 	 
	 * 
	 * @param basePeriod	The base period
	 * @param comparisonPeriods	The comparison periods information
	 * 		Parameter comparisonPeriods can assume a value as described in {@link AnalysisAPIController#getVerticalHorizontalAnalysis(String, String, String, int)} method
	 * 
	 * @return	A {@link List} of {@link YearMonth}
	 */
	private List<YearMonth> getAdditionalPeriods(YearMonth basePeriod, int comparisonPeriods) {
		
		List<YearMonth> periods = new LinkedList<>();
		
		if( comparisonPeriods < 10 ) { //Months before
			
			int numMonths = 1;
			switch (comparisonPeriods) {
			case 2:
				numMonths = 3;
				break;
			case 3:
				numMonths = 6;
				break;
			case 4:
				numMonths = 12;
				break;
			default:
				break;
			}			
			
			for ( int i = 1; i <= numMonths; i++ ) {
				YearMonth period = basePeriod.minusMonths(i);
				periods.add(period);	
			}			
			
		}
		
		else if( comparisonPeriods > 10 && comparisonPeriods < 20) { //Years before			
			
			for ( int i = 1; i <= (comparisonPeriods-10); i++ ) {
				YearMonth period = basePeriod.minusYears(i);
				periods.add(period);	
			}	
			
		}
		
		else if( comparisonPeriods > 20 && comparisonPeriods < 30 ) { //Years after
			
			int numMonths = 1;
			switch (comparisonPeriods) {
			case 22:
				numMonths = 3;
				break;
			case 23:
				numMonths = 6;
				break;
			case 24:
				numMonths = 12;
				break;
			default:
				break;
			}			
			
			for ( int i = 1; i <= numMonths; i++ ) {
				YearMonth period = basePeriod.plusMonths(i);
				periods.add(period);	
			}	
			
		}
		
		else if( comparisonPeriods > 30 ) { //Years after
			for ( int i = 1; i <= (comparisonPeriods-30); i++ ) {
				YearMonth period = basePeriod.plusYears(i);
				periods.add(period);	
			}	
		}
		
		return periods;
	}
	
	@ApiOperation(value = "Get vertical and horizontal additional columns to show in table data ", response = List.class, tags = "vertical_horizontal_view_columns")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/vertical_horizontal_view_columns"})
	public ResponseEntity<Object> getAnalysisViewColumns(
			@ApiParam(name = "finalDate", allowEmptyValue = false, allowMultiple = false, example = "Wed Dec 01 2021", required = true, type = "LocalDate")
			@RequestParam("finalDate") @DateTimeFormat(iso = ISO.DATE) LocalDate finalDate,
			@ApiParam(name = "comparisonPeriods", allowEmptyValue = false, allowMultiple = false, example = "1", required = true, type = "Integer", 
					allowableValues = "1=One month before, 2=Three months before, 3=Six months before, 4=Twelve months before, " +
					"11=One year before, 12=Two year before, 13=Three year before, 14=Four year before, 15=Five year before, " +
					"21=One month after, 22=Three months after, 32=Six months after, 24=Twelve months after, " +
					"31=One year after, 32=Two year after, 33=Three year after, 34=Four year after, 35=Five year after")
			@RequestParam("comparisonPeriods") int comparisonPeriods,
			@ApiParam(name = "analysisType", allowEmptyValue = false, allowMultiple = false, example = "1", required = true, type = "Integer", 
						allowableValues = "1=VERTICAL, 2=HORIZONTAL, 3=BOTH")
			@RequestParam("analysisType") int analysisType ) {
		
		if ( finalDate == null /*|| finalDate.length() < 15*/ ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'finalDate'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}

		YearMonth period = YearMonth.from(finalDate); ///ParserUtils.parseDayMonthDayYear(finalDate);

		List<YearMonth> allPeriods = getAdditionalPeriods(period, comparisonPeriods);
		
		String horizontal = " " + messageSource.getMessage("horizontal", null, LocaleContextHolder.getLocale());
		String vertical = " " + messageSource.getMessage("vertical", null, LocaleContextHolder.getLocale());
		
		allPeriods.add(0,period);
		
		String titleValue = messageSource.getMessage("account.finalBalance", null, LocaleContextHolder.getLocale());
		String decimalChar = messageSource.getMessage("decimal.char", null, LocaleContextHolder.getLocale());
		String decimalGroupSeparator = messageSource.getMessage("decimal.grouping.separator", null, LocaleContextHolder.getLocale());
		String leftSymbol = messageSource.getMessage("currency.symbol.prefix", null, LocaleContextHolder.getLocale());
		String rightSymbol = messageSource.getMessage("currency.symbol.suffix", null, LocaleContextHolder.getLocale());
		String currencySymbol = leftSymbol + rightSymbol;
		String symbolAfter = ( null == leftSymbol || leftSymbol.isEmpty() ) ? "true" : "false";
		String monthFormat = messageSource.getMessage("month.format", null, LocaleContextHolder.getLocale());
		DateTimeFormatter simpleFormat = DateTimeFormatter.ofPattern(monthFormat);	

		List<Map<String,Object>> allColumns = new LinkedList<>();
		int i = 0;
		
		Map<String,Object> column = new HashMap<>();		
//		column.put("title", messageSource.getMessage("account.level", null, LocaleContextHolder.getLocale()));
//		column.put("field", "level");
//		column.put("hozAlign", "center");
//		column.put("headerSort", false);
//		allColumns.add(column);
//		
//		String nullConvert = "function(value, data, type, component){\n" +
//								"if ( value == null )\n" +
//								"return '';\n'"+
//								"return value;\n"+ 
//								"}";
//		
//		column = new HashMap<>();		
//		column.put("title", messageSource.getMessage("account.category", null, LocaleContextHolder.getLocale()));
//		column.put("field", "category");
//		column.put("hozAlign", "left");
//		column.put("headerSort", false);
//		column.put("accessorDownload", nullConvert);		
//		allColumns.add(column);
//		
//		column = new HashMap<>();		
//		column.put("title", messageSource.getMessage("account.subcategory", null, LocaleContextHolder.getLocale()));
//		column.put("field", "subcategory");
//		column.put("hozAlign", "left");
//		column.put("headerSort", false);
//		column.put("accessorDownload", nullConvert);		
//		allColumns.add(column);
//		
//		column = new HashMap<>();		
//		column.put("title", messageSource.getMessage("account.code", null, LocaleContextHolder.getLocale()));
//		column.put("field", "code");
//		column.put("hozAlign", "left");
//		column.put("headerSort", false);
//		column.put("accessorDownload", nullConvert);		
//		allColumns.add(column);		
//		
//		column = new HashMap<>();		
//		column.put("title", messageSource.getMessage("account.name", null, LocaleContextHolder.getLocale()));
//		column.put("field", "name");
//		column.put("hozAlign", "left");
//		column.put("headerSort", false);
//		column.put("accessorDownload", nullConvert);		
//		allColumns.add(column);			
		
		Map<String,Object> formatCurrency = new HashMap<>();
		formatCurrency.put("decimal", decimalChar);
		formatCurrency.put("thousand", decimalGroupSeparator);
		formatCurrency.put("symbol", currencySymbol);
		formatCurrency.put("symbolAfter", symbolAfter);
		formatCurrency.put("precision", "2");
		
		Map<String,Object> formatPercentage = new HashMap<>();
		formatPercentage.put("decimal", decimalChar);
		formatPercentage.put("thousand", decimalGroupSeparator);
		formatPercentage.put("symbol", "%");
		formatPercentage.put("symbolAfter", true);
		formatPercentage.put("precision", "2");
		
		for ( YearMonth p : allPeriods ) {
			
			Map<String,Object> group = new HashMap<>();
			allColumns.add(group);
			String title = StringUtils.capitalize(simpleFormat.format(p));
			group.put("title", title);
			group.put("headerHozAlign", "center");
			
			List<Map<String,Object>> columns = new LinkedList<>();
			group.put("columns",columns);
			
			String field = "B" + i;
			
			column = new HashMap<>();
			column.put("formatterParams", formatCurrency);
			column.put("title", titleValue);
			column.put("field", field);
			column.put("hozAlign", "right");
			column.put("headerSort", false);
			column.put("headerHozAlign", "center");
			column.put("formatter", "money");			
			columns.add(column);
			
			if ( analysisType == VERTICAL || analysisType == BOTH ) { //Vertical OR both
				column = new HashMap<>();
				column.put("formatterParams", formatPercentage);
				column.put("title", vertical);
				column.put("field", "V" + i);
				column.put("hozAlign", "right");
				column.put("headerSort", false);
				column.put("headerHozAlign", "center");
				column.put("formatter", "money");
				columns.add(column);
			}
			
			if ( i > 0 && ( analysisType == HORIZONTAL || analysisType == BOTH ) ) { //Horizontal OR both
				column = new HashMap<>();
				column.put("formatterParams", formatPercentage);
				column.put("title", horizontal);
				column.put("field", "H" + i);
				column.put("hozAlign", "right");
				column.put("headerSort", false);
				column.put("headerHozAlign", "center");
				column.put("formatter", "money");
				columns.add(column);	
			}			
			
			i++;

		}
		
    	return ResponseEntity.ok().body(allColumns);    	
    	
	}

	@ApiOperation(value = "Get general analysis data for a specified qualifier and period", response = AnalysisData.class, tags = "general_analysis")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/general_analysis"})
	public ResponseEntity<Object> getGeneralAnalysis(
			@ApiParam(name = "qualifier", allowEmptyValue = false, allowMultiple = false, example = "qualifier1", required = true, type = "String", 
				allowableValues = "qualifier1, qualifier2, qualifier3, qualifier4, qualifier5")
			@RequestParam("qualifier") String qualifier,
			@ApiParam(name = "qualifierValue", allowEmptyValue = false, allowMultiple = false, example = "Economic sector", required = true, type = "String")
			@RequestParam("qualifierValue") String qualifierValue,
			@ApiParam(name = "sourceData", allowEmptyValue = false, allowMultiple = false, example = "Economic sector", required = true, type = "String", 
					allowableValues = "1 - SOURCE_JOURNAL, 2 - SOURCE_DECLARED_INCOME_STATEMENT,  3 - SOURCE_BOOTH_INCOME_STATEMENT, 4 - SOURCE_SHAREHOLDERS") 
			@RequestParam("sourceData") int sourceData,
			@ApiParam(name = "year", allowEmptyValue = false, allowMultiple = false, example = "2021", required = true, type = "Integer")
			@RequestParam("year") String year) {
		
		if ( qualifier == null || qualifier.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifier'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( qualifierValue == null || qualifierValue.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifierValue'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( sourceData == 0 ) {
			log.log(Level.WARNING, "Missing parameter 'sourceData'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	AnalysisData analysisData = analysisService.getGeneralAnalysisValues(qualifier, qualifierValue, sourceData, Integer.valueOf(year));
	
    	return ResponseEntity.ok().body(analysisData);    	
	}
	
	@ApiOperation(value = "Get qualifiers values for a specified qualifier", response = List.class, tags = "qualifiers")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/qualifiers"})
	public ResponseEntity<Object> getQualifierValues(
			@ApiParam(name = "qualifier", allowEmptyValue = false, allowMultiple = false, example = "qualifier1", required = true, type = "String", 
					allowableValues = "qualifier1, qualifier2, qualifier3, qualifier4, qualifier5")			
			@RequestParam("qualifier") String qualifier) {
		
		if ( qualifier == null || qualifier.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'qualifier'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<String> values = analysisService.getQualifierValues(qualifier);
    	
    	return ResponseEntity.ok().body(values);    
		
	}
	
	@ApiOperation(value = "Get years with available data", response = List.class, tags = "years")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/years"})
	public ResponseEntity<Object> getYears(
			@ApiParam(name = "sourceData", allowEmptyValue = false, allowMultiple = false, example = "Economic sector", required = true, type = "String", 
						allowableValues = "1 - SOURCE_JOURNAL, 2 - SOURCE_DECLARED_INCOME_STATEMENT,  3 - SOURCE_BOOTH_INCOME_STATEMENT, 4 - SOURCE_SHAREHOLDERS")
			@RequestParam("sourceData") int sourceData) {
		
		if ( sourceData == 0 ) {
			log.log(Level.WARNING, "Missing parameter 'sourceData'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}		
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<Integer> values = analysisService.getYears(sourceData);
    	
    	return ResponseEntity.ok().body(values);    
		
	}	
	
	@ApiOperation(value = "Get accountin flows data", response = List.class, tags = "accounting_flows")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/accounting_flows"})
	public ResponseEntity<Object> getAccountingFlows(
			@ApiParam(name = "taxpayerId", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@RequestParam("taxpayerId") String taxpayerId,
			@ApiParam(name = "startDate", allowEmptyValue = false, allowMultiple = false, example = "Wed Dec 01 2021", required = true, type = "LocalDate")
			@RequestParam("startDate") @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
			@ApiParam(name = "finalDate", allowEmptyValue = false, allowMultiple = false, example = "Wed Dec 01 2021", required = true, type = "LocalDate")
			@RequestParam("finalDate") @DateTimeFormat(iso = ISO.DATE) LocalDate finalDate) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<AggregatedAccountingFlow> values = analysisService.getAccountingFlow(taxpayerId, 
    			Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
    			Date.from(finalDate.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()));
    	
    	return ResponseEntity.ok().body(values);    
		
	}
	
	@ApiOperation(value = "Get statement income analysis data", response = List.class, tags = "statement_income_analysis")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/statement_income_analysis"})
	public ResponseEntity<Object> getStatementIncomeAnalysis(
			@ApiParam(name = "taxpayerId", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@RequestParam("taxpayerId") String taxpayerId,
			@ApiParam(name = "year", allowEmptyValue = false, allowMultiple = false, example = "2021", required = true, type = "Integer")
			@RequestParam("year") String year ) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<StatementIncomeItem> statements = analysisService.getStatementIncomeDeclaredAndCalculated(
    			taxpayerId,Integer.valueOf(year));
	
    	return ResponseEntity.ok().body(statements);    	
	}
	
	@ApiOperation(value = "Get taxpayer view analysis data, according with searchType parameter value", response = List.class, tags = "taxpayer_general_view")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/taxpayer_general_view"})
	public ResponseEntity<Object> getTaxpayerGeneralView(
			@ApiParam(name = "searchType", allowEmptyValue = false, allowMultiple = false, example = "1", required = true, type = "String", 
					allowableValues = "1 - SHAREHOLDINGS, 2 - SHAREHOLDERS, 3 - REVENUE_NET_AND_GROSS_PROFIT_DECLARED, " + 
						"4 - REVENUE_NET_AND_GROSS_PROFIT_COMPUTED, 5 - TAX_PROVISION, 6 - ANALYTICS_ACCOUNTS, " + 
						"7 - CUSTOMERS, 8 - SUPPLIERS")
			@RequestParam("searchType") int searchType,
			@ApiParam(name = "taxpayerId", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@RequestParam("taxpayerId") String taxpayerId,
			@ApiParam(name = "year", allowEmptyValue = false, allowMultiple = false, example = "2021", required = true, type = "Integer")
			@RequestParam("year") String year ) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( searchType == 0 ) {
			searchType = AnalysisService.SEARCH_SHAREHOLDINGS;
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<?> result = analysisService.getTaxpayerData(taxpayerId,Integer.valueOf(year),searchType);
    	
    	if ( result == null || result.isEmpty() )  
    		result = Collections.emptyList();
    	
    	return ResponseEntity.ok().body(result);    	
	}
	
	@ApiOperation(value = "Get customers vs suppliers values for reconciliation analysis", response = List.class, tags = "customers_vs_suppliers_analysis")
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/analysis/customers_vs_suppliers_analysis"})
	public ResponseEntity<Object> getCustomersVsSuppliersView(		
			@ApiParam(name = "taxpayerId", allowEmptyValue = false, allowMultiple = false, example = "1234567890", required = true, type = "String")
			@RequestParam("taxpayerId") String taxpayerId,
			@ApiParam(name = "year", allowEmptyValue = false, allowMultiple = false, example = "2021", required = true, type = "Integer")
			@RequestParam("year") String year ) {
		
		if ( taxpayerId == null || taxpayerId.isEmpty() ) {
			log.log(Level.WARNING, "Missing parameter 'taxpayerId'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		if ( year == null || year.isEmpty() || Integer.valueOf(year) == 0 ) {
			log.log(Level.WARNING, "Missing or invalid parameter 'year'");
			return ResponseEntity.ok().body(Collections.emptyList());
		}
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	List<?> result = analysisService.getCustomersVsSuppliers(taxpayerId,Integer.valueOf(year));
    	
    	if ( result == null || result.isEmpty() )  
    		result = Collections.emptyList();
    	
    	return ResponseEntity.ok().body(result);    	
	}		
}
