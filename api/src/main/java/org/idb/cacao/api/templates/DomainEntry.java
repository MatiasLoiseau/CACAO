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
package org.idb.cacao.api.templates;

import static org.springframework.data.elasticsearch.annotations.FieldType.Keyword;
import static org.springframework.data.elasticsearch.annotations.FieldType.Text;

import java.io.Serializable;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.idb.cacao.api.DomainLanguage;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

/**
 * Entry (value) of a Domain Table. This is related to a 'DomainTable'
 * (one DomainTable refers to multiple DomainEntry's).<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class DomainEntry implements Serializable, Cloneable, Comparable<DomainEntry> {

	private static final long serialVersionUID = 1L;

	/**
	 * Reference key for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(min=2, max=120)
	private String key;
	
	/**
	 * Language for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	@Enumerated(EnumType.STRING)
	@NotBlank
	@NotNull
	@NotEmpty
	@Field(type=Text)
	private DomainLanguage language;

	/**
	 * Description associated to this domain entry according to a specific language.
	 */
	@MultiField(
		mainField = @Field(type=Text, fielddata=true),
		otherFields = {
			@InnerField(suffix = "keyword", type=Keyword)
		}
	)
	@NotBlank
	@NotNull
	@NotEmpty
	@Size(max=1024)
	private String description;
	
	public DomainEntry() { }
	
	/**
	 * This constructor should only be used by static 'built-in' definitions. In runtime it should
	 * be resolved to specific languages according to message.property files.
	 */
	public DomainEntry(String key, String messagePropertyReference) { 
		this.key = key;
		this.description = messagePropertyReference;
	}

	public DomainEntry(String key, DomainLanguage language, String description) {
		this.key = key;
		this.language = language;
		this.description = description;
	}

	/**
	 * Reference key for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Reference key for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	public void setKey(String key) {
		this.key = key;
	}
	
	public DomainEntry withKey(String key) {
		setKey(key);
		return this;
	}

	/**
	 * Language for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	public DomainLanguage getLanguage() {
		return language;
	}

	/**
	 * Language for this domain entry. The pair (key,language) should be unique in the same DomainTable.
	 */
	public void setLanguage(DomainLanguage language) {
		this.language = language;
	}
	
	public DomainEntry withLanuage(DomainLanguage language) {
		setLanguage(language);
		return this;
	}

	/**
	 * Description associated to this domain entry according to a specific language.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Description associated to this domain entry according to a specific language.
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	public DomainEntry withDescription(String description) {
		setDescription(description);
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof DomainEntry))
			return false;
		DomainEntry ref = (DomainEntry)o;
		if (key!=ref.key) {
			if (this.key==null || ref.key==null)
				return false;
			if (!this.key.equalsIgnoreCase(ref.key))
				return false;
		}
		if (language!=ref.language) {
			if (this.language==null || ref.language==null)
				return false;
			if (!this.language.equals(ref.language))
				return false;			
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return 17 + (int) ( 37 * ( (key==null?0:key.hashCode()) + 37 * (language==null?0:language.hashCode())) );
	}

	public DomainEntry clone() {
		try {
			return (DomainEntry)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

    @Override
    public String toString() {
        return "DomainEntry{key=" + key + '}';
    }

	@Override
	public int compareTo(DomainEntry o) {
		if (key==null)
			return -1;
		if (o.key==null)
			return 1;
		int comp = String.CASE_INSENSITIVE_ORDER.compare(key, o.key);
		if (comp!=0)
			return comp;

		if (language==null)
			return -1;
		if (o.language==null)
			return 1;
		comp = language.compareTo(o.language);
		if (comp!=0)
			return comp;
		
		return comp;
	}
	
}
