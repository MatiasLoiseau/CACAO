/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.etl.repositories;

import org.idb.cacao.api.DocumentUploaded;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * DAO for DocumentUploaded objects (history of all uploads from each user)
 * 
 * @author Gustavo Figueiredo
 *
 */
@Repository
public interface DocumentUploadedRepository extends ElasticsearchRepository<DocumentUploaded, String> {
	
	Page<DocumentUploaded> findByTemplateNameAndTemplateVersionAndTaxPayerIdAndTaxPeriodNumber(String templateName, String templateVersion, String taxPayerId, Integer taxPeriodNumber, Pageable pageable);
	
}
