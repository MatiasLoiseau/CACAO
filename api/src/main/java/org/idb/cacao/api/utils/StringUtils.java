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
package org.idb.cacao.api.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Utility methods for String manipulation
 * 
 * @author Gustavo Figueiredo
 *
 */
public class StringUtils {

    public static final Pattern pOnlyNumbers = Pattern.compile("^\\d+$");
    public static final Pattern pInteger = Pattern.compile("^[+-]?\\s*\\d+$");
    public static final Pattern pDecimal = Pattern.compile("^[+-]?\\s*\\d+\\.\\d+$");
    public static final Pattern pBoolean = Pattern.compile("^(?>true|false)$",Pattern.CASE_INSENSITIVE);

	public static Date toDate(int day, int month, int year) {
		Calendar cal = Calendar.getInstance();
		cal.set(year, month - 1, day, 0, 0, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	/**
	 * Presents memory information formatted as String
	 */
	public static String formatMemory(Long amount, NumberFormat format) {
		if (amount==null)
			return null;
		if (amount<1024)
			return format.format(amount)+" bytes";
		double kbytes = ((double)amount)/1024.0;
		if (kbytes<1024)
			return format.format(kbytes)+" Kilobytes";
		double mbytes = ((double)kbytes)/1024.0;
		if (mbytes<1024)
			return format.format(mbytes)+" Megabytes";
		double gbytes = ((double)mbytes)/1024.0;
		return format.format(gbytes)+" Gigabytes";
	}

	/**
	 * Given a field value indexed in Elastic Search, returns the same or another value to be presented to user
	 */
	public static String formatValue(MessageSource messageSource, Object value) {
		if (value==null)
			return "";
		if (((value instanceof String) && "true".equalsIgnoreCase((String)value)) || Boolean.TRUE.equals(value))
			return messageSource.getMessage("yes", null, LocaleContextHolder.getLocale());
		if (((value instanceof String) && "false".equalsIgnoreCase((String)value)) || Boolean.FALSE.equals(value))
			return messageSource.getMessage("no", null, LocaleContextHolder.getLocale());
		if ((value instanceof String) && ParserUtils.isTimestamp((String)value))
			return text(messageSource, ParserUtils.parseTimestamp((String)value)).replace(" 00:00:00", "");
		if ((value instanceof String) && ParserUtils.isDecimal((String)value))
			return text(messageSource, Double.parseDouble((String)value));
		if (value instanceof Date)
			return text(messageSource, (Date)value).replace(" 00:00:00", "");
		if (value instanceof Double)
			return text(messageSource, (Double)value);
		if (value instanceof Float)
			return text(messageSource, (Float)value);
		return String.valueOf(value);
	}

	/**
	 * Translates a message according to the language configured for current user session.
	 */
	public static String text(MessageSource messages, String key, Object... arguments) {
		return messages.getMessage(key, arguments, LocaleContextHolder.getLocale());
	}

	/**
	 * Formats a date/time according to the regional defaults
	 */
	public static String text(MessageSource messages, Date timestamp) {
		return new SimpleDateFormat(messages.getMessage("timestamp.format", null, LocaleContextHolder.getLocale())).format(timestamp);
	}
	
	/**
	 * Formats a decimal number according to the regional defaults
	 */
	public static String text(MessageSource messages, Number number) {
		DecimalFormatSymbols sym = new DecimalFormatSymbols();
		sym.setDecimalSeparator(messages.getMessage("decimal_char", null, LocaleContextHolder.getLocale()).charAt(0));
		sym.setGroupingSeparator(messages.getMessage("decimal_grouping_separator", null, LocaleContextHolder.getLocale()).charAt(0));
		return new DecimalFormat("###,###.#############", sym).format(number.doubleValue());

	}
}
