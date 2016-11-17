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
package nl.basjes.parse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.basjes.parse.core.Parser;
import nl.basjes.parse.httpdlog.ApacheHttpdLoglineParser;
import nl.basjes.parse.httpdlog.HttpdLoglineParser;

public final class Main {
	private Main() {
	}

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);

	private void printAllPossibles(String logformat) {
		// To figure out what values we CAN get from this line we instantiate
		// the parser with a dummy class
		// that does not have ANY @Field annotations.
		Parser<Object> dummyParser = new HttpdLoglineParser<Object>(Object.class, logformat);

		List<String> possiblePaths;
		possiblePaths = dummyParser.getPossiblePaths();

		// If you want to call 'getCasts' then the actual parser needs to be
		// constructed.
		// Simply calling getPossiblePaths does not build the actual parser.
		// Because we want this for all possibilities yet we are never actually
		// going to use this instance of the parser
		// We simply give it a random method with the right signature and tell
		// it we want all possible paths
		try {
			dummyParser.addParseTarget(String.class.getMethod("indexOf", String.class), possiblePaths);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return;
		}

		LOG.info("==================================");
		LOG.info("Possible output:");
		for (String path : possiblePaths) {
			LOG.info("{}     {}", path, dummyParser.getCasts(path));
		}
		LOG.info("==================================");
	}

	private void run() throws Exception {

		String logformat = "%h %l %u %t %D %T \"%r\" %>s %b \"%{Referer}i\" \"%{User-agent}i\"";

		printAllPossibles(logformat);

		Parser<MyRecord> parser = new ApacheHttpdLoglineParser<MyRecord>(MyRecord.class, logformat);
		parser.ignoreMissingDissectors();

		String inputFile = "/Users/walterbduquedeestrada/logs/access_log-20160807";
		

		//Load file in memory
		File file = new File(inputFile);
		if (!file.exists()) {
			throw new RuntimeException("Input file does not exist");
		}
		BufferedReader reader = new BufferedReader(new FileReader(file));
		List<String> readLines = new ArrayList<String>();
		String line = reader.readLine();
		while (line != null) {
			readLines.add(line);
			line = reader.readLine();
		}
		reader.close();
		
		//Parse apache logs
		List<MyRecord> myRecords = new ArrayList<MyRecord>();
		for (String readLine : readLines) {

			try {
				MyRecord myRecord = new MyRecord();
				parser.parse(myRecord, readLine);
				if (myRecord.getAction() != null && "200".equals(myRecord.getStatus()) && myRecord.getPath() != null
						&& myRecord.getPath().contains("WSUser")) {
					myRecords.add(myRecord);
				}
			} catch (Exception e) {
			}
		}
		
		//Group by action
		Map<String, List<MyRecord>> map = new HashMap<String, List<MyRecord>>();
		for (MyRecord item : myRecords) {

			String key = item.getAction();
			if (map.get(key) == null) {
				map.put(key, new ArrayList<MyRecord>());
			}
			map.get(key).add(item);
		}
		
		//Collect stats
		List<MyRecordStats> recordStats = new ArrayList<MyRecordStats>();
		for (Entry<String, List<MyRecord>> entry : map.entrySet()) {
			MyRecordStats stats = new MyRecordStats();
			stats.setActionName(entry.getKey());
			long responseCount = entry.getValue().size();
			stats.setResponseCount(responseCount);
			long sum = 0;
			for(MyRecord myRecord:entry.getValue()) {
				sum = sum + myRecord.getResponseTime();
			}
			BigDecimal average = new BigDecimal(sum).divide(new BigDecimal(responseCount*1000000), 2, RoundingMode.HALF_UP).setScale(2, RoundingMode.UP);
			stats.setAverageResponseTime(average.toPlainString());
			recordStats.add(stats);
		}
		
		
		//Write lines to file
		String outputFile = "/Users/walterbduquedeestrada/logs/access_log-20160807.csv";
		PrintWriter f0 = new PrintWriter(new FileWriter(outputFile));
		f0.print(MyRecord.headerString());
		for (MyRecordStats myRecordStats : recordStats) {
			f0.print(myRecordStats.toString());
		}
		f0.close();
/*		
		

		try (Stream<String> stream = Files.lines(Paths.get(inputFile))) {
			System.out.println(MyRecord.headerString());
			Files.write(Paths.get(outputFile), MyRecord.headerString().getBytes());

			Stream<MyRecord> recordStream = stream.map(logLine -> {
				MyRecord record = new MyRecord();
				try {
					parser.parse(record, logLine.trim());
				} catch (Exception e) {
				}
				return record;
			}).filter(myRecord -> {
				boolean b = myRecord.getAction() != null && "200".equals(myRecord.getStatus())
						&& myRecord.getPath() != null && myRecord.getPath().contains("WSUser");

				if (b) {
					LOG.debug(myRecord.toString());
				}
				return b;
			});
			Map<String, List<MyRecord>> collect = recordStream.collect(Collectors.groupingBy(MyRecord::getAction));
			Stream<Entry<String, List<MyRecord>>> stream2 = collect.entrySet().stream();
			List<MyRecordStats> collect2 = stream2.map(entry -> {
				MyRecordStats stats = new MyRecordStats();
				stats.setActionName(entry.getKey());
				stats.setResponseCount(entry.getValue().size());
				OptionalDouble average = entry.getValue().stream().mapToLong(MyRecord::getResponseTime).average();
				stats.setAverageResponseTime(average.getAsDouble());
				return stats;
			}).collect(Collectors.toList());

			collect2.forEach(myRecordStats -> {
				try {
					Files.write(Paths.get(outputFile), myRecordStats.toString().getBytes(), StandardOpenOption.CREATE,
							StandardOpenOption.APPEND);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});

		} catch (Throwable e) {
			e.printStackTrace();
		}*/

	}

	/**
	 * @param args
	 *            The commandline arguments
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static void main(final String[] args) throws Exception {
		new Main().run();
	}

}
