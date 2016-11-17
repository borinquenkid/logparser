/*
 * Apache HTTPD logparsing made easy
 * Copyright (C) 2011-2016 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ehi.carshare;

import java.text.NumberFormat;
import java.text.ParseException;

import nl.basjes.parse.core.Field;

public class ApacheHttpLog {

	private String clientIdentd;
	private String dateTimeString;
	private String endpoint;
	private String ipAddress;


	private String method;
	private String protocol;
	private String path;
	private String userID;
	private String action;
	private long responseSize;
	private long responseTime;
	private String status;

	@Field("STRING:request.firstline.uri.query.action")
	public void setAction(String action) {
		this.action = action;

	}

	@Field("IP:connection.client.host")
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Field("STRING:connection.client.user")
	public void setClientIdentd(String clientIdentd) {
		this.clientIdentd = clientIdentd;
	}

	@Field("HTTP.USERINFO:request.referer.userinfo")
	public void setUserID(String userID) {
		this.userID = userID;
	}

	@Field("TIME.STAMP:request.receive.time")
	public void setDateTimeString(String dateTimeString) {
		this.dateTimeString = dateTimeString;
	}

	@Field("HTTP.HOST:request.firstline.uri.method")
	public void setMethod(String method) {
		this.method = method;
	}

	@Field("HTTP.HOST:request.referer.host")
	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Field("HTTP.PROTOCOL:request.referer.protocol")
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	@Field("HTTP.PATH:request.firstline.uri.path")
	public void setPath(String path) {
		this.path = path;
	}

	@Field("BYTES:response.body.bytesclf")
	public void setResponseSize(String responseSize) {
		
		this.responseSize = parseLong(responseSize);

	}
	
	private long parseLong(String numberString) {
		long x = 0;
		try {
			x = NumberFormat.getInstance().parse(numberString).longValue();
		} catch (ParseException e) {
			
		}
		return x;
	}

	@Field("MICROSECONDS:server.process.time")
	public void setResponseTime(String responseTime) {
		this.responseTime = parseLong(responseTime);;

	}

	public long getResponseSize() {
		return responseSize;
	}

	public long getResponseTime() {
		return responseTime;
	}

	@Field("STRING:request.status.last")
	public void setStatus(String status) {
		this.status = status;

	}

	/**
	 * 2016-11-14 15:56:14 INFO Main:68 - BYTES:response.body.bytesclf [STRING,
	 * LONG] 2016-11-14 15:56:14 INFO Main:68 - IP:connection.client.host
	 * [STRING]
	 */

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(clientIdentd).append(", ").append(dateTimeString).append(", ").append(endpoint).append(", ")
				.append(ipAddress).append(", ").append(method).append(",").append(protocol).append(",").append(path)
				.append(",").append(userID).append(",").append(action).append(",").append(responseSize).append(",")
				.append(responseTime).append(",")
				.append(status).append(System.lineSeparator());
		return builder.toString();
	}

	public String getPath() {
		return path;
	}

	public String getAction() {
		return action;
	}
	
	
	public static String headerString() {
		StringBuilder builder = new StringBuilder();
		builder.append("clientIdentd").append(", dateTimeString").append(", endpoint").append(",ipAddress")
				.append(", method").append(", protocol").append(", path").append(", userID").append(", action")
				.append(", responseSize").append(",responseTime").append(",responseTime").append(System.lineSeparator());
		return builder.toString();
	}

	public String getStatus() {
		return status;
	}

}
