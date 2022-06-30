/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.web.dto;

/**
 * Data Transfer Object for listing Kibana Spaces<BR>
 * View: dashboards_list.html<BR>
 * Controller: DashboardsUIController<BR>
 * 
 * @author Gustavo Figueiredo
 *
 */
public class Space implements Comparable<Space> {

	private String id;
	
	private String name;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return getName();
	}

	@Override
	public int compareTo(Space o) {
		return String.CASE_INSENSITIVE_ORDER.compare(name, o.name);
	}
}
