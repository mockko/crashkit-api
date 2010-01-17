package com.yoursway.crashkit.internal.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RequestString {

	public static String encode(Map<String, ?> data) {
		StringBuilder result = new StringBuilder();
		try {
			encode(data, result);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
		return result.toString();
	}

	private static void encode(Map<String, ?> data, StringBuilder result)
			throws UnsupportedEncodingException {
		List<String> keys = new ArrayList<String>(data.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			Object value = data.get(key);
			if (result.length() > 0)
				result.append('&');
			result.append(key);
			result.append('=');
			if (value == null)
				value = "";
			result.append(URLEncoder.encode(value.toString(), "UTF-8"));
		}
	}

	public static Map<String, String> decode(String data) {
		HashMap<String, String> result = new HashMap<String, String>();
		try {
			decode(data, result);
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
		return result;
	}

	private static void decode(String data, HashMap<String, String> result)
			throws UnsupportedEncodingException {
		for (String item : data.trim().split("&")) {
			String[] parts = item.split("=", 2);
			if (parts.length == 1)
				result.put(item, "");
			else
				result.put(parts[0], URLDecoder.decode(parts[1], "UTF-8"));
		}
	}

}
