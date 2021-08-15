package com.ariseontech.joindesk.auth.util;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ApiUtil {

	@Value("api.key")
	private String apiKey;

	@Value("api.secret")
	private String apiSecret;

	@Value("api.endpoint")
	private String apiEndpoint;

	private RestTemplate rest;
	private HttpHeaders headers;
	private HttpStatus status;

	public ApiUtil() {
		this.rest = new RestTemplate();
		this.headers = new HttpHeaders();
		headers.add("Content-Type", "application/json");
		headers.add("Accept", "*/*");
	}

	public String get(String uri) {
		HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
		ResponseEntity<String> responseEntity = rest.exchange(apiEndpoint + uri, HttpMethod.GET, requestEntity,
				String.class);
		this.setStatus(responseEntity.getStatusCode());
		JSONObject response = new JSONObject();
		response.put("status", responseEntity.getStatusCodeValue());
		response.put("body", responseEntity.getBody());
		return response.toString();
	}

	public String post(String uri, String json) {
		HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
		ResponseEntity<String> responseEntity = rest.exchange(apiEndpoint + uri, HttpMethod.POST, requestEntity,
				String.class);
		this.setStatus(responseEntity.getStatusCode());
		JSONObject response = new JSONObject();
		response.put("status", responseEntity.getStatusCodeValue());
		response.put("body", responseEntity.getBody());
		return response.toString();
	}

	public HttpStatus getStatus() {
		return status;
	}

	public void setStatus(HttpStatus status) {
		this.status = status;
	}

}
