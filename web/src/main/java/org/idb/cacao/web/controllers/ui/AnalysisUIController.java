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
package org.idb.cacao.web.controllers.ui;

import java.util.logging.Logger;

import org.idb.cacao.web.entities.User;
import org.idb.cacao.web.errors.UserNotFoundException;
import org.idb.cacao.web.utils.UserUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller class for all endpoints related to 'analysis view' from tax payers
 * 
 * @author Rivelino Patrício
 *
 */
@Controller
public class AnalysisUIController {
	
	static final Logger log = Logger.getLogger(AnalysisUIController.class.getName());

	@Autowired
	private MessageSource messages;
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/vertical_horizontal_analysis"})
	public String getVerticalHorizontalAnalysis(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
		
        return "analysis/vertical_horizontal_analysis";
	}	
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/general_analysis"})
	public String getGeneralAnalysis(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	model.addAttribute("decimalChar", messages.getMessage("decimal.char", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("decimalGroupSeparator", messages.getMessage("decimal.grouping.separator", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorYearsNotFound", 
    			messages.getMessage("years.missing", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorQualifierValuesNotFound", 
    			messages.getMessage("qualifier.values.missing", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorNoDataAvailable", 
    			messages.getMessage("no.data.available", null, LocaleContextHolder.getLocale()));
		
        return "analysis/general_analysis";
	}		
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/statement_income_analysis"})
	public String getStatementIncomeAnalysis(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	model.addAttribute("decimalChar", messages.getMessage("decimal.char", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("decimalGroupSeparator", messages.getMessage("decimal.grouping.separator", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorYearsNotFound", 
    			messages.getMessage("years.missing", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorNoDataAvailable", 
    			messages.getMessage("no.data.available", null, LocaleContextHolder.getLocale()));
		
        return "analysis/statement_income_analysis";
	}
	
	@Secured({"ROLE_TAX_REPORT_READ"})
	@GetMapping(value= {"/taxpayer_general_view"})
	public String getTaxpayerGeneralView(Model model) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	if (auth==null)
    		throw new UserNotFoundException();
    	User user = UserUtils.getUser(auth);
    	if (user==null)
    		throw new UserNotFoundException();
    	
    	model.addAttribute("decimalChar", messages.getMessage("decimal.char", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("decimalGroupSeparator", messages.getMessage("decimal.grouping.separator", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorYearsNotFound", 
    			messages.getMessage("years.missing", null, LocaleContextHolder.getLocale()));
    	model.addAttribute("errorNoDataAvailable", 
    			messages.getMessage("no.data.available", null, LocaleContextHolder.getLocale()));
		
        return "analysis/taxpayer_general_view";
	}
}
