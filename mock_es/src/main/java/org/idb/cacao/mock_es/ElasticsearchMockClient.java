/*******************************************************************************
 * Copyright © [2021]. Banco Interamericano de Desarrollo ("BID"). Uso autorizado.
 * Los procedimientos y resultados obtenidos en base a la ejecución de este software son los programados por los desarrolladores y no necesariamente reflejan el punto de vista del BID, de su Directorio Ejecutivo ni de los países que representa.
 *
 * This software uses third-party components, distributed accordingly to their own licenses.
 *******************************************************************************/
package org.idb.cacao.mock_es;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This is a 'mocked' ElasticSearch client. It may be used for unit testing
 * simulating a ElasticSearch server without actually using one.<BR>
 * <BR>
 * All the responses will be mocked according to the expectations programmed.
 *
 */
public class ElasticsearchMockClient {
	
	public static final String PROP_SIGNAL_MOCK = "MOCKED_ELASTIC_SEARCH";

	private final ClientAndServer mockServer;

	public String version = "7.14.1";
	public String clusterName = "mock";
	private static final String MOCK_SERVER_LOG_LEVEL = "WARNING";
	// private static final String MOCK_SERVER_LOG_LEVEL = "INFO";
	// private static final String MOCK_SERVER_LOG_LEVEL = "DEBUG";

	// Setup LOG level for MockServer
	static {
		ConfigurationProperties.logLevel(MOCK_SERVER_LOG_LEVEL);
	}

	private final Map<String, MockedIndex> map_indices;

	public ElasticsearchMockClient(final int port) {
		this.mockServer = startClientAndServer(port);
		this.map_indices = new HashMap<>();
		init();
	}
	
	public void clearAllIndices() {
		map_indices.clear();
	}

	public static Integer findRandomPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}
	
	/**
	 * Set a system property telling we just started a mocked version of Elastic Search
	 */
	public static void setSignalMockedES() {
		System.setProperty(PROP_SIGNAL_MOCK, String.valueOf(System.currentTimeMillis()));

	}
	
	/**
	 * Removes the system property informed with 'setSignalMockedES'
	 */
	public static void clearSignalMockedES() {
		System.clearProperty(PROP_SIGNAL_MOCK);
		
	}

	public void stop() {
		mockServer.stop();
		clearSignalMockedES();
	}

	/**
	 * Initializes the mocked server with a couple of rules regarding the HTTP
	 * requests according to Elastic Search API specification (just a few of them)
	 */
	protected void init() {
		
		setSignalMockedES();

		// Program all the expectations for each request pattern

		this.mockServer.when(HttpRequest.request().withPath(".*/_count"))
				.respond(toHttpResponse(new JSONObject(map("count", 0))));

		this.mockServer.when(HttpRequest.request().withPath(".*/_search")).respond(searchDocument());

		this.mockServer.when(HttpRequest.request().withPath(".*/_refresh")).respond(
				toHttpResponse(new JSONObject(map("_shards", map("total", 100, "successful", 100, "failed", 0)))));

		this.mockServer.when(HttpRequest.request().withPath(".*/_bulk").withMethod("POST")).respond(bulkOperation());

		this.mockServer.when(HttpRequest.request().withPath(".*/_bulk"))
				.respond(toHttpResponse(new JSONObject(map("took", 10, "errors", false, "items", new JSONArray()))));
		
		// Update a document
		this.mockServer.when(HttpRequest.request().withPath(".*/_doc/.+").withMethod("PUT")).respond(putDocument());		

		// Create index
		this.mockServer.when(HttpRequest.request().withMethod("PUT")).respond(createIndex());

		// Add document to index
		this.mockServer.when(HttpRequest.request().withPath(".*/_doc").withMethod("POST")).respond(postDocument());

		// Get document from index
		this.mockServer.when(HttpRequest.request().withPath(".*/_doc/.+").withMethod("GET")).respond(getDocument());

		// Delete document from index
		this.mockServer.when(HttpRequest.request().withPath(".*/_doc/.+").withMethod("DELETE"))
				.respond(deleteDocument());

		// Check if index exists
		this.mockServer.when(HttpRequest.request().withMethod("HEAD")).respond(checkIndexExists());

		// Default: return cluster version information
		this.mockServer.when(HttpRequest.request()).respond(toHttpResponse(
				new JSONObject(map("name", "mock", 
						"cluster_name", clusterName, 
						"cluster_uuid", UUID.randomUUID(),
						"tagline", "You Know, for Search",
						"version", 
							map("number", version,
								"build_flavor","default",
								"build_type","docker",
							    "build_hash", "66b55ebfa59c92c15db3f69a335d500018b3331e",
							    "build_date", "2021-08-26T09:01:05.390870785Z",
							    "build_snapshot", "false",
							    "lucene_version", "8.9.0",
							    "minimum_wire_compatibility_version", "6.8.0",
							    "minimum_index_compatibility_version", "6.0.0-beta1")))));

	}

	/**
	 * Wrap the JSON response into a HTTP response
	 */
	private HttpResponse toHttpResponse(final JSONObject data) {
		return HttpResponse.response(data.toString()).withHeader("Content-Type", "application/json").withHeader("X-Elastic-Product","Elasticsearch");
	}

	/**
	 * Mocked response related to the API call for 'create a index'
	 */
	private ExpectationResponseCallback createIndex() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String index_name = request.getPath().toString().replace("/", "");
				MockedIndex mocked_index = new MockedIndex();
				mocked_index.setName(index_name);
				map_indices.put(index_name, mocked_index);
				return toHttpResponse(
						new JSONObject(map("acknowledged", true, "shards_acknowledged", true, "index", index_name)));
			}
		};
	}
	
	/**
	 * Mocked response related to the API call for 'bulk' inserts/updates/deletes
	 */
	private ExpectationResponseCallback bulkOperation() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				
				String[] bulkRequestLines = request.getBodyAsString().split("\r?\n");

				ObjectMapper mapper = new ObjectMapper();

				List<Map<String,Object>> responseBody = new LinkedList<>();
				
				Consumer<Map<String, Object>> nextOperation = null;

				for (String bulkRequestLine: bulkRequestLines) {
					@SuppressWarnings("unchecked")
					Map<String, Object> requestBody = (Map<String, Object>) mapper.readValue(bulkRequestLine,
							Map.class);
					
					if (nextOperation==null) {						
						@SuppressWarnings("unchecked")
						Map<String,Object> indexRequest = (Map<String, Object>) requestBody.get("index");
						if (indexRequest!=null) {
							String indexName = (String)indexRequest.get("_index");
							nextOperation = (record)->{
								JSONObject response = newDocument(indexName, record);	
								responseBody.add(map("index",response));
							};
						}
						@SuppressWarnings("unchecked")
						Map<String,Object> createRequest = (Map<String, Object>) requestBody.get("create");
						if (createRequest!=null) {
							String indexName = (String)createRequest.get("_index");
							nextOperation = (record)->{
								JSONObject response = newDocument(indexName, record);	
								responseBody.add(map("create",response));
							};
						}
						@SuppressWarnings("unchecked")
						Map<String,Object> updateRequest = (Map<String, Object>) requestBody.get("update");
						if (updateRequest!=null) {
							String indexName = (String)updateRequest.get("_index");
							String id = (String)updateRequest.get("_id");
							nextOperation = (record)->{
								JSONObject response = updateDocument(indexName, id, record);
								responseBody.add(map("update",response));
							};
						}
						@SuppressWarnings("unchecked")
						Map<String,Object> deleteRequest = (Map<String, Object>) requestBody.get("delete");
						if (deleteRequest!=null) {
							String indexName = (String)deleteRequest.get("_index");
							String id = (String)deleteRequest.get("_id");
							JSONObject response = deleteDocument(indexName, id);
							responseBody.add(map("delete",response));
						}						
					}
					else {
						nextOperation.accept(requestBody);
						nextOperation = null;
					}
				}
				
				JSONObject response = new JSONObject(map("took", 10, "errors", false, "items", new JSONArray(responseBody)));
				return toHttpResponse(response);
			}
		};
	}

	/**
	 * Mocked response related to the API call for 'put into the index a new
	 * document'
	 */
	private ExpectationResponseCallback postDocument() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String index_name = request.getPath().toString().split("/")[1];
				ObjectMapper mapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, Object> fields = (Map<String, Object>) mapper.readValue(request.getBodyAsJsonOrXmlString(),
						Map.class);
				JSONObject response = newDocument(index_name, fields);				
				return toHttpResponse(response);
			}
		};
	}
	
	public JSONObject newDocument(String index_name, Map<String, Object> fields) {
		MockedIndex mocked_index = map_indices.get(index_name);
		if (mocked_index == null) {
			mocked_index = new MockedIndex();
			mocked_index.setName(index_name);
			map_indices.put(index_name, mocked_index);
		}
		String id = UUID.randomUUID().toString();
		fields.put("_id", id);
		fields.put("_index", index_name);
		fields.put("_version", 1);
		mocked_index.getMapDocuments().put(id, fields);
		JSONObject response = new JSONObject(map("_shards",
				map("total", 1, "successful", 1, "skipped", 0, "failed", 0), "_index", index_name, "_type",
				"_doc", "_id", id, "_version", 1, "_seq_no", 0, "_primary_term", 1, "result", "created"));
		return response;
	}
	
	/**
	 * Mocked response related to the API call for 'put into the index an updated
	 * document'
	 */
	private ExpectationResponseCallback putDocument() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String uri = request.getPath().toString();
				if (uri.startsWith("/"))
					uri = uri.substring(1);
				String[] uri_parts = uri.split("/");
				String index_name = uri_parts[0];
				String id = uri_parts[2];
				ObjectMapper mapper = new ObjectMapper();
				@SuppressWarnings("unchecked")
				Map<String, Object> fields = (Map<String, Object>) mapper.readValue(request.getBodyAsJsonOrXmlString(),
						Map.class);
				JSONObject response = updateDocument(index_name, id, fields);
				return toHttpResponse(response);
			}
		};
	}	

	public JSONObject updateDocument(String index_name, String id, Map<String, Object> fields) {
		MockedIndex mocked_index = map_indices.get(index_name);
		if (mocked_index == null) {
			mocked_index = new MockedIndex();
			mocked_index.setName(index_name);
			map_indices.put(index_name, mocked_index);
		}
		if ( id == null )
			id = (String) fields.get("id");
		
		fields.put("_id", id);
		fields.put("_index", index_name);
		fields.put("_version", 1);
		mocked_index.getMapDocuments().put(id, fields);
		JSONObject response = new JSONObject(map("_shards",
				map("total", 1, "successful", 1, "skipped", 0, "failed", 0), "_index", index_name, "_type",
				"_doc", "_id", id, "_version", 1, "_seq_no", 0, "_primary_term", 1, "result", "created"));
		return response;
	}
	
	/**
	 * Mocked response related to the API call for 'get from the index an existent
	 * document given its ID'
	 */
	private ExpectationResponseCallback getDocument() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String uri = request.getPath().toString();
				if (uri.startsWith("/"))
					uri = uri.substring(1);
				String[] uri_parts = uri.split("/");
				String index_name = uri_parts[0];
				String id = uri_parts[2];
				MockedIndex mocked_index = map_indices.get(index_name);
				if (mocked_index != null) {
					Map<?, ?> doc = mocked_index.getMapDocuments().get(id);
					if (doc != null) {
						return toHttpResponse(new JSONObject(
								map("_index", index_name, "_type", "_doc", "_id", id, "found", true, "_source", doc, "_version", 1)));
					}
				}
				return toHttpResponse(
						new JSONObject(map("_index", index_name, "_type", "_doc", "_id", id, "found", false)));
			}
		};
	}

	/**
	 * Mocked response related to the API call for 'delete from the index an
	 * existent document given its ID'
	 */
	private ExpectationResponseCallback deleteDocument() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String uri = request.getPath().toString();
				if (uri.startsWith("/"))
					uri = uri.substring(1);
				String[] uri_parts = uri.split("/");
				String index_name = uri_parts[0];
				String id = uri_parts[2];
				JSONObject response = deleteDocument(index_name, id);
				if (response!=null) {
					return toHttpResponse(response);
				}
				return HttpResponse.notFoundResponse();
			}
		};
	}
	
	public JSONObject deleteDocument(String index_name, String id) {
		MockedIndex mocked_index = map_indices.get(index_name);
		if (mocked_index != null) {
			Map<?, ?> doc = mocked_index.getMapDocuments().remove(id);
			if (doc != null) {
				return new JSONObject(map("_index", index_name, "_type", "_doc", "_id", id,
						"_version", 1, "_seq_no", 0, "result", "deleted", "_shards",
						map("total", 1, "successful", 1, "failed", 0)));
			}
		}		
		return null;
	}

	/**
	 * Mocked response related to the API call for 'search the index with a query
	 * expression'<BR>
	 * WARNING: just implemented a small part of the query syntax!<BR>
	 * E.g.:<BR>
	 * {"bool":{"must":[{"query_string":{"query":"something",
	 * "fields"=["login"]}}]}}
	 */
	private ExpectationResponseCallback searchDocument() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String uri = request.getPath().toString();
				if (uri.endsWith("/_search"))
					uri = uri.substring(0, uri.length() - "/_search".length());
				String index_name = uri.replace("/", "");
				MockedIndex index = map_indices.get(index_name);
				if (index == null)
					return HttpResponse.notFoundResponse();

				ObjectMapper mapper = new ObjectMapper();
				Map<?, ?> request_body = mapper.readValue(request.getBodyAsJsonOrXmlString(), Map.class);
				Map<?, ?> query = (Map<?, ?>) request_body.get("query");

				if (query.containsKey("match_all")) {
					return toHttpResponse(new JSONObject(map("took", 10, "timed_out", false, "_shards",
							map("total", index.getMapDocuments().size(), "successful", index.getMapDocuments().size(),
									"skipped", 0, "failed", 0),
							"hits", map("total", map("value", index.getMapDocuments().size(), "relation", "eq"), "hits",
									index.getMapDocuments().values()))));
				}

				Predicate<Map<?, ?>> compiledQuery = parseQuery(query);
				List<Map<?, ?>> hits = new LinkedList<>();
				for (Map.Entry<String, Map<?, ?>> doc : index.getMapDocuments().entrySet()) {
					if (compiledQuery.test(doc.getValue())) {
						Map<String,Object> hit = map(
								"_index", index_name,
								"_type", "_doc",
								"_id", doc.getKey(),
								"_score", 1.0,
								"_source", doc.getValue());
						hits.add(hit);
					}
				}

				return toHttpResponse(new JSONObject(map("took", 10, "timed_out", false, "_shards",
						map("total", hits.size(), "successful", hits.size(), "skipped", 0, "failed", 0), "hits",
						map("total", map("value", hits.size(), "relation", "eq"), "hits", hits))));
			}
		};
	}

	/**
	 * Mocked response related to the API call for 'check if an index exists'
	 */
	private ExpectationResponseCallback checkIndexExists() {
		return new ExpectationResponseCallback() {
			@Override
			public HttpResponse handle(HttpRequest request) throws Exception {
				String index_name = request.getPath().toString().replace("/", "");
				if (map_indices.containsKey(index_name))
					return HttpResponse.response();
				else
					return HttpResponse.notFoundResponse();
			}
		};
	}

	/**
	 * Utility method for creating a MAP out of an array of objects (key-value
	 * pairs)
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> map(Object... args) {
		Map<K, V> res = new HashMap<>();
		K key = null;
		for (Object arg : args) {
			if (key == null) {
				key = (K) arg;
			} else {
				res.put(key, (V) arg);
				key = null;
			}
		}
		return res;
	}

	/**
	 * Internal representation of an 'index'. Just for the purpose of tests
	 */
	public static class MockedIndex {
		private String name;

		private final Map<String, Map<?, ?>> mapDocuments;

		public MockedIndex() {
			mapDocuments = new HashMap<>();
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Map<String, Map<?, ?>> getMapDocuments() {
			return mapDocuments;
		}

		public String toString() {
			return name;
		}
	}

	/**
	 * Parse a query according to Elastic Search syntax.<BR>
	 * WARNING: just implemented a small part of the query syntax!<BR>
	 * E.g.(1):<BR>
	 * {"bool":{"must":[{"query_string":{"query":"something",
	 * "fields"=["login"]}}]}}<BR>
	 * E.g.(2):<BR>
	 * {"wrapper":{"query":"eyJtYXRjaCI6IHsibmFtZS5rZXl3b3JkIjogeyJxdWVyeSI6ICJURVNUIn19fQ=="}}<BR>
	 * E.g.(3):<BR>
	 * {"match": {"name.keyword": {"query": "TEST"}}}<BR>
	 */
	public static Predicate<Map<?, ?>> parseQuery(Map<?, ?> query) throws IOException {
		Object bool_query = query.get("bool");
		if (bool_query != null) {
			if (!(bool_query instanceof Map))
				throw new UnsupportedOperationException(
						"Unexpected value type for boolean query operation for TESTING purpose: "
								+ bool_query.getClass().getName());
			List<Predicate<Map<?, ?>>> must_predicates = new LinkedList<>();
			List<Predicate<Map<?, ?>>> should_predicates = new LinkedList<>();
			Map<?, ?> boolean_params = (Map<?, ?>) bool_query;
			Object must_criteria = boolean_params.get("must");
			if (must_criteria != null) {
				if (must_criteria instanceof List) {
					for (Object must_crit : ((List<?>) must_criteria)) {
						if (!(must_crit instanceof Map))
							throw new UnsupportedOperationException(
									"Unexpected value type for 'must' criteria at query operation for TESTING purpose: "
											+ must_crit.getClass().getName());
						Map<?, ?> crit_as_map = (Map<?, ?>) must_crit;
						must_predicates.add(getCondition(crit_as_map));
					}
				} else {
					throw new UnsupportedOperationException(
							"Unexpected value type for 'must' criteria at query operation for TESTING purpose: "
									+ must_criteria.getClass().getName());
				}
			}
			Object should_criteria = boolean_params.get("should");
			if (should_criteria != null) {
				if (should_criteria instanceof List) {
					for (Object should_crit : ((List<?>) should_criteria)) {
						if (!(should_crit instanceof Map))
							throw new UnsupportedOperationException(
									"Unexpected value type for 'should' criteria at query operation for TESTING purpose: "
											+ should_crit.getClass().getName());
						Map<?, ?> crit_as_map = (Map<?, ?>) should_crit;
						should_predicates.add(getCondition(crit_as_map));
					}
				} else {
					throw new UnsupportedOperationException(
							"Unexpected value type for 'should' criteria at query operation for TESTING purpose: "
									+ should_criteria.getClass().getName());
				}
			}
			return new Predicate<Map<?, ?>>() {
				@Override
				public boolean test(Map<?, ?> doc) {
					return (must_predicates.isEmpty() || must_predicates.stream().allMatch(c -> c.test(doc))
							&& (should_predicates.isEmpty() || should_predicates.stream().anyMatch(c -> c.test(doc))));
				}
			};
		}
		Object wrapper_query = query.get("wrapper");
		if (wrapper_query != null) {
			if (!(wrapper_query instanceof Map))
				throw new UnsupportedOperationException(
						"Unexpected value type for wrapper query operation for TESTING purpose: "
								+ wrapper_query.getClass().getName());
			String query_b64 = (String)((Map<?,?>)wrapper_query).get("query");
			if (query_b64==null) {
				throw new UnsupportedOperationException(
						"Missing 'query' inside 'wrapper query': "
								+ new ObjectMapper().writeValueAsString(query));
			}
			String query_decoded_as_json = new String(Base64.getDecoder().decode(query_b64), StandardCharsets.UTF_8);
			Map<?,?> query_decoded = new ObjectMapper().readValue(query_decoded_as_json, Map.class);
			return parseQuery(query_decoded);
		}
		Object match_query = query.get("match");
		if (match_query != null) {
			return getCondition(query);
		}
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(query);
		throw new UnsupportedOperationException("Unsupported query: " + json);
	}

	/**
	 * Create a condition to be testing during query according to the query
	 * properties
	 */
	public static Predicate<Map<?, ?>> getCondition(Map<?, ?> query_properties) throws IOException {
		Object query_string = query_properties.get("query_string");
		if (query_string instanceof Map) {
			Map<?, ?> query_string_as_map = (Map<?, ?>) query_string;
			final String query = unescape( query_string_as_map.get("query") );
			if (query == null) {
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(query_properties);
				throw new UnsupportedOperationException("Missing 'query' at query properties: " + json);
			}
			final List<?> fields = (List<?>) query_string_as_map.get("fields");
			if (fields == null) {
				ObjectMapper mapper = new ObjectMapper();
				String json = mapper.writeValueAsString(query_properties);
				throw new UnsupportedOperationException("Missing 'fields' at query properties: " + json);
			}
			return new Predicate<Map<?, ?>>() {
				@Override
				public boolean test(Map<?, ?> doc) {
					for (Object field : fields) {
						Object value = doc.get(treatFieldName(field));
						String s_value = (value instanceof String) ? (String)value 
								: (value==null) ? null 
								: value.toString();
						if (s_value != null && s_value.equalsIgnoreCase(query)) {
							return true;
						}
					}
					return false;
				}
			};
		} 
		else {
			
			Object match_query = query_properties.get("match");
			if (match_query != null) {
				if (!(match_query instanceof Map))
					throw new UnsupportedOperationException(
							"Unexpected value type for match query operation for TESTING purpose: "
									+ match_query.getClass().getName());
				Map.Entry<?,?> match_query_entry = ((Map<?,?>)match_query).entrySet().iterator().next();
				final String field_name_to_match = (String)match_query_entry.getKey();
				final String value_to_match = toStringValue(((Map<?,?>)match_query_entry.getValue()).get("query"));
				return new Predicate<Map<?, ?>>() {
					@Override
					public boolean test(Map<?, ?> doc) {
						String value = toStringValue( doc.get(treatFieldName(field_name_to_match)) );
						if (value != null && value.equalsIgnoreCase(value_to_match)) {
							return true;
						}
						return false;
					}
				};
			}

			Object term_query = query_properties.get("term");
			if (term_query != null) {
				if (!(term_query instanceof Map))
					throw new UnsupportedOperationException(
						"Unexpected value type for 'term' query: "
									+ term_query.getClass().getName());
				Map.Entry<?,?> term_query_entry = ((Map<?,?>)term_query).entrySet().iterator().next();
				String field_name_to_match = (String)term_query_entry.getKey();
				final String value_to_match = toStringValue(((Map<?,?>)term_query_entry.getValue()).get("value"));
				return new Predicate<Map<?, ?>>() {
					@Override
					public boolean test(Map<?, ?> doc) {
						String value = toStringValue( doc.get(treatFieldName(field_name_to_match)) );
						if (value != null && value.equalsIgnoreCase(value_to_match)) {
							return true;
						}
						return false;
					}
				};
			}

			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(query_properties);
			throw new UnsupportedOperationException("Unexpected query properties: " + json);
		}
	}

	/**
	 * Do some minor treatments over field names for this simple test cases
	 * (discards unused and unsupported features like 'fuzzyness')
	 */
	public static String treatFieldName(Object n) {
		return ((String) n)
				.replaceAll("\\.keyword$", "")    // something like "name.keyword" becomes "name"
				.replaceAll("\\^[\\d\\.]+$", ""); // something like "fieldname^1.0" becomes "fieldname"
	}

	public static String toStringValue(Object o) {
		if (o==null)
			return null;
		if (o instanceof String)
			return (String)o;
		return String.valueOf(o);
	}
	
	public static String unescape(Object o) {
		String s = toStringValue(o);
		if (s==null)
			return null;
		return StringEscapeUtils.unescapeJava(s);
	}
}
