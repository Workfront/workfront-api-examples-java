package com.attask.api;

/*
 * Copyright (c) 2011 AtTask, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HostnameVerifier; // located in jsee.jar which is located in your jre/lib directory
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class StreamClient {

	private static final String METH_DELETE = "DELETE";
	private static final String METH_GET    = "GET";
	private static final String METH_POST   = "POST";
	private static final String METH_PUT    = "PUT";

	private static final String PATH_LOGIN  = "/login";
	private static final String PATH_LOGOUT = "/logout";
	private static final String PATH_SEARCH = "/search";

	private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private String hostname;
	private String sessionID;

	public StreamClient (String hostname) {
		this.hostname = hostname;
	}

	public JSONObject login (String username, String password) throws StreamClientException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("username", username);
		params.put("password", password);
		return (JSONObject) request(PATH_LOGIN, params, null, METH_GET);
	}

	public boolean logout () throws StreamClientException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("sessionID", sessionID);
		JSONObject result = (JSONObject) request(PATH_LOGOUT, params, null, METH_GET);
		try {
			return result.getBoolean("success");
		}
		catch (JSONException e) {
			throw new StreamClientException(e);
		}
	}

	public JSONArray search (String objCode, Map<String, Object> query) throws StreamClientException {
		return search(objCode, query, (Set<String>) null);
	}

	public JSONArray search (String objCode, Map<String, Object> query, String[] fields) throws StreamClientException {
		return search(objCode, query, new HashSet<String>(Arrays.asList(fields)));
	}

	public JSONArray search (String objCode, Map<String, Object> query, Set<String> fields) throws StreamClientException {
		return (JSONArray) request("/"+objCode+PATH_SEARCH, query, fields, METH_GET);
	}

	public JSONObject get (String objCode, String objID) throws StreamClientException {
		return get(objCode, objID, (Set<String>) null);
	}

	public JSONObject get (String objCode, String objID, String[] fields) throws StreamClientException {
		return get(objCode, objID, new HashSet<String>(Arrays.asList(fields)));
	}

	public JSONObject get (String objCode, String objID, Set<String> fields) throws StreamClientException {
		return (JSONObject) request("/"+objCode+"/"+objID, null, fields, METH_GET);
	}

	public JSONObject post (String objCode, Map<String, Object> message) throws StreamClientException {
		return post(objCode, message, (Set<String>) null);
	}

	public JSONObject post (String objCode, Map<String, Object> message, String[] fields) throws StreamClientException {
		return post(objCode, message, new HashSet<String>(Arrays.asList(fields)));
	}

	public JSONObject post (String objCode, Map<String, Object> message, Set<String> fields) throws StreamClientException {
		return (JSONObject) request("/"+objCode, message, fields, METH_POST);
	}

	public JSONObject put (String objCode, String objID, Map<String, Object> message) throws StreamClientException {
		return put(objCode, objID, message, (Set<String>) null);
	}

	public JSONObject put (String objCode, String objID, Map<String, Object> message, String[] fields) throws StreamClientException {
		return put(objCode, objID, message, new HashSet<String>(Arrays.asList(fields)));
	}

	public JSONObject put (String objCode, String objID, Map<String, Object> message, Set<String> fields) throws StreamClientException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("updates", new JSONObject(message).toString());
		return (JSONObject) request("/"+objCode+"/"+objID, params, fields, METH_PUT);
	}
	
	public boolean delete (String objCode, String objID) throws StreamClientException {
		return delete(objCode, objID, false);
	}

	public boolean delete (String objCode, String objID, boolean force) throws StreamClientException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("force", force);
		JSONObject result = (JSONObject) request("/"+objCode+"/"+objID, params, null, METH_DELETE);
		try {
			return result.getBoolean("success");
		}
		catch (JSONException e) {
			throw new StreamClientException(e);
		}
	}

	private Object request (String path, Map<String, Object> params, Set<String> fields, String method) throws StreamClientException {
		HttpURLConnection conn = null;

		try {
			String query = "sessionID=" + sessionID + "&method=" + method;

			if (params != null) {
				for (String key : params.keySet()) {
					query += "&" + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(String.valueOf(params.get(key)), "UTF-8");
				}
			}

			if (fields != null) {
				query += "&fields=";
				for (String field : fields) {
					query += URLEncoder.encode(field, "UTF-8") + ",";
				}
				query = query.substring(0, query.lastIndexOf(","));
			}
			
			conn = createConnection(hostname + path, method);

			// Send request
			Writer out = new OutputStreamWriter(conn.getOutputStream());
			out.write(query);
			out.flush();
			out.close();

			// Read response
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;

			while ((line = in.readLine()) != null) {
				response.append(line);
			}

			in.close();

			// Decode JSON
			JSONObject result = new JSONObject(response.toString());

			// Verify result
			if (result.has("error")) {
				throw new StreamClientException(result.getJSONObject("error").getString("message"));
			}
			else if (!result.has("data")) {
				throw new StreamClientException("Invalid response from server");
			}

			// Manage the session
			if (path.equals(PATH_LOGIN)) {
				sessionID = result.getJSONObject("data").getString("sessionID");
			}
			else if (path.equals(PATH_LOGOUT)) {
				sessionID = null;
			}

			return result.get("data");
		}
		catch (Exception e) {
			throw new StreamClientException(e);
		}
		finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private HttpURLConnection createConnection (String spec, String method) throws IOException {
		URL url = new URL(spec);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		if (conn instanceof HttpsURLConnection) {
			((HttpsURLConnection) conn).setHostnameVerifier(HOSTNAME_VERIFIER);
		}

		conn.setAllowUserInteraction(false);
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setUseCaches(false);
		conn.setConnectTimeout(60000);
		conn.setReadTimeout(300000);
		conn.connect();

		return conn;
	}

}