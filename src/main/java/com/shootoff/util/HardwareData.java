package com.shootoff.util;

import static java.net.URLEncoder.encode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;

public class HardwareData {
	private static final Logger logger = LoggerFactory.getLogger(HardwareData.class);

	private static final SystemInfo si = new SystemInfo();
	private static final long BYTES_IN_MEGABYTE = 1048576;

	public static String getCpuName() {
		// Remove (R) and (TM) because they make the processor harder to search
		// for as most databases don't include them
		return si.getHardware().getProcessor().getName().replaceAll("\\(R\\)", "").replaceAll("\\(TM\\)", "");
	}

	public static long getMegabytesOfRam() {
		return si.getHardware().getMemory().getTotal() / BYTES_IN_MEGABYTE;
	}

	public static Optional<Integer> getCpuScore() {
		final String cpuResult = searchCpuByName(getCpuName());

		if ("Connection to the search engine failed.".equals(cpuResult) || cpuResult.startsWith("No results found"))
			return Optional.empty();

		final String cpuData = getCpuByUrl(cpuResult);

		if (logger.isTraceEnabled()) logger.trace("CPU score data: {}", cpuData);

		final JSONParser parser = new JSONParser();

		try {
			final JSONObject jsonObject = (JSONObject) parser.parse(cpuData);
			final JSONObject scoreData = (JSONObject) jsonObject.values().toArray()[0];

			return Optional.of(Integer.parseInt((String) scoreData.get("Score")));
		} catch (final ParseException e) {
			return Optional.empty();
		}
	}

	/*
	 * Copyright (c) 2015 yakka34 Permission is hereby granted, free of charge,
	 * to any person obtaining a copy of this software and associated
	 * documentation files (the "Software"), to deal in the Software without
	 * restriction, including without limitation the rights to use, copy,
	 * modify, merge, publish, distribute, sublicense, and/or sell copies of the
	 * Software, and to permit persons to whom the Software is furnished to do
	 * so, subject to the following conditions: The above copyright notice and
	 * this permission notice shall be included in all copies or substantial
	 * portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT
	 * WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
	 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
	 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
	 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
	 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
	 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
	 */

	private static String getCpuByUrl(String url) {
		final String jsonString = getCpuInfo(url);
		return jsonString;
	}

	// Uses cpubenchmark.net's zoom zearch and returns cpu's
	// benhmark/information url.
	private static String searchCpuByName(String cpuName) {
		final String encodedName = encodeToUrl(cpuName);
		Document html = null;
		String url = null;
		try {
			// Connects to zoom's search engine and looks for given cpu from
			// benhmarks section.
			html = Jsoup.connect("https://www.passmark.com/search/zoomsearch.php?zoom_sort=0&zoom_query=" + encodedName
					+ "&zoom_cat%5B%5D=5").get();
		} catch (final IOException e) {
			logger.warn("Connection throws an exception: " + e);
		}

		// Regex check is used to validate correct search result.
		if (html != null) {
			Elements links = html.select("div.results");
			links = links.select("a[href~=^(https?:\\/\\/www.cpubenchmark.net/cpu.php\\?)]");
			url = links.attr("href");
			if (url.isEmpty()) {
				return "No results found for: " + cpuName;
			}
		} // message for connection issues.
		else {
			return "Connection to the search engine failed.";
		}
		return url;
	}

	// getCpuInfo calls various methods to construct a complete Json formated
	// string from given url.
	private static String getCpuInfo(String url) {
		Document html = null;
		String infoString;
		String jsonString;
		String infoArray[];
		try {
			html = Jsoup.connect(url).get();
		} catch (final IOException e) {
			logger.warn("Connection to: " + url + " ,throws an exception: " + e);
		}

		if (html != null) {
			// Attributes in the infoString are seperated by commas and data by
			// semicolons.
			infoString = parseHtmlForInfo(html);
			// infoString needs to be split into array for further processing.
			infoArray = parseStringToArray(infoString);
			// Array is used to create JSONString.
			jsonString = convertArrayToJsonString(infoArray);
		} else {
			logger.warn("No CPU data html value assigned returning null!");
			return null;
		}
		return jsonString;
	}

	// Parses given html file. Data parsing is hardcoded because lack of id
	// tagging on cpubenchmark.net behalf.
	private static String parseHtmlForInfo(Document html) {
		// Instead of parsing the the whole html page everytime, only useful
		// table section is used.
		final Element table = html.select("table.desc").first();
		// <span> containing the name is clearly labeled as cpuname.
		final String cpuName = table.select("span.cpuname").text();
		// Score is the last one to use <span> tag and will be parsed to int.
		final int cpuScore = Integer.parseInt(table.select("span").last().text());
		// There are 2 <em> tags containing information. First one has
		// description and second one has "Other names" eg.alternative name.
		final String description = table.select("em").first().text();
		final String altName = table.select("em").last().text();
		// Name -> Score -> possible description -> AltName.
		final String infoString = cpuName + ",Score:" + cpuScore + "," + description + ",AltName:" + altName;
		return infoString;
	}

	// Splits the infoString into array by using regex split.
	private static String[] parseStringToArray(String infoString) {
		// Splits the String everytime it founds comma or semicolon.
		final String[] infoArray = infoString.split("[,:]");
		return infoArray;
	}

	// Data from array is read and placed into json object which will be parsed
	// into String.
	@SuppressWarnings("unchecked")
	private static String convertArrayToJsonString(String[] infoArray) {
		// Depending on prefered formating, use of temp is not necessary.
		final JSONObject temp = new JSONObject();
		final JSONObject jObj = new JSONObject();
		final int length = infoArray.length;
		for (int i = 1; i < length - 1; i += 2) {
			final int y = i + 1;
			temp.put(infoArray[i].trim(), infoArray[y].trim());
		}
		// Name of the cpu is always located first in the array.
		jObj.put(infoArray[0], temp);
		return jObj.toString();
	}

	private static String encodeToUrl(String string) {
		String encodedUrl = null;
		try {
			encodedUrl = encode(string, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			logger.warn("Encoding not supported: " + e);
		}
		return encodedUrl;
	}

}
