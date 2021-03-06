/*******************************************************************************
 * Copyright 2019 Observational Health Data Sciences and Informatics
 * 
 * This file is part of WhiteRabbit
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.ohdsi.whiteRabbit.fakeDataGenerator;

import java.util.*;

import org.ohdsi.databases.RichConnection;
import org.ohdsi.rabbitInAHat.dataModel.Database;
import org.ohdsi.rabbitInAHat.dataModel.Field;
import org.ohdsi.rabbitInAHat.dataModel.Table;
import org.ohdsi.rabbitInAHat.dataModel.ValueCounts;
import org.ohdsi.utilities.StringUtilities;
import org.ohdsi.utilities.collections.OneToManySet;
import org.ohdsi.utilities.files.Row;
import org.ohdsi.utilities.files.WriteCSVFileWithHeader;
import org.ohdsi.whiteRabbit.DbSettings;

public class FakeDataGenerator {

	private RichConnection connection;
	private int targetType;
	private int maxRowsPerTable = 1000;
	private boolean firstFieldAsKey;
	private boolean doUniformSampling;


	private static int REGULAR = 0;
	private static int RANDOM = 1;
	private static int PRIMARY_KEY = 2;

	public void generateData(DbSettings dbSettings, int maxRowsPerTable, String filename, String folder) {
		generateData(dbSettings, maxRowsPerTable, filename, folder, false, false);
	}

	public void generateData(DbSettings dbSettings, int maxRowsPerTable, String filename, String folder, boolean firstFieldAsKey, boolean doUniformSampling) {
		this.maxRowsPerTable = maxRowsPerTable;
		this.targetType = dbSettings.dataType;
		this.firstFieldAsKey = firstFieldAsKey;
		this.doUniformSampling = doUniformSampling;

		StringUtilities.outputWithTime("Starting creation of fake data");
		System.out.println("Loading scan report from " + filename);
		Database database = Database.generateModelFromScanReport(filename);

		if (targetType == DbSettings.DATABASE) {
			connection = new RichConnection(dbSettings.server, dbSettings.domain, dbSettings.user, dbSettings.password, dbSettings.dbType);
			connection.use(dbSettings.database);
			for (Table table : database.getTables()) {
				if (table.getName().toLowerCase().endsWith(".csv"))
					table.setName(table.getName().substring(0, table.getName().length() - 4));
				System.out.println("Generating table " + table.getName());
				createTable(table);
				connection.insertIntoTable(generateRows(table).iterator(), table.getName(), false);
			}
			connection.close();
		} else {
			for (Table table : database.getTables()) {
				String name = folder + "/" + table.getName();
				if (!name.toLowerCase().endsWith(".csv"))
					name = name + ".csv";
				System.out.println("Generating table " + name);
				WriteCSVFileWithHeader out = new WriteCSVFileWithHeader(name, dbSettings.csvFormat);
				for (Row row : generateRows(table))
					out.write(row);
				out.close();
			}
		}
		StringUtilities.outputWithTime("Done");
	}

	private List<Row> generateRows(Table table) {
		if (table.getRowCount() == 0 || table.getRowsCheckedCount() == 0) {
			// Empty table, return empty list (writes empty file)
			return new ArrayList<>();
		}

		String[] fieldNames = new String[table.getFields().size()];
		ValueGenerator[] valueGenerators = new ValueGenerator[table.getFields().size()];
		int size = maxRowsPerTable;
		for (int i = 0; i < table.getFields().size(); i++) {
			Field field = table.getFields().get(i);
			fieldNames[i] = field.getName();
			ValueGenerator valueGenerator = new ValueGenerator(field, this.firstFieldAsKey && i == 0);
			valueGenerators[i] = valueGenerator;
//			if (valueGenerator.generatorType == PRIMARY_KEY && valueGenerator.values.length < size)
//				size = valueGenerator.values.length;
		}
		List<Row> rows = new ArrayList<Row>();
		for (int i = 0; i < size; i++) {
			Row row = new Row();
			for (int j = 0; j < fieldNames.length; j++)
				row.add(fieldNames[j], valueGenerators[j].generate());
			rows.add(row);
		}
		return rows;
	}

	private void createTable(Table table) {
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE " + table.getName() + " (\n");
		for (int i = 0; i < table.getFields().size(); i++) {
			Field field = table.getFields().get(i);
			sql.append("  " + field.getName() + " " + field.getType().toUpperCase());
			if (i < table.getFields().size() - 1)
				sql.append(",\n");
		}
		sql.append("\n);");
		connection.execute(sql.toString());
	}

	private boolean isVarChar(String type) {
		type = type.toUpperCase();
		return (type.equals("VARCHAR") || type.equals("VARCHAR2") || type.equals("CHARACTER VARYING"));
	}

	private boolean isInt(String type) {
		type = type.toUpperCase();
		return (type.equals("INT") || type.equals("INTEGER") || type.equals("BIGINT"));
	}

	private class ValueGenerator {

		private String[] values;
		private int[] cumulativeFrequency;
		private int totalFrequency;
		private String type;
		private int length;
		private int pk_cursor;
		private int generatorType;
		private Random random = new Random();

		public ValueGenerator(Field field, boolean forcePrimaryKey) {
			ValueCounts valueCounts = field.getValueCounts();
			type = field.getType();

			if (valueCounts.isEmpty()) {
				length = field.getMaxLength();
				generatorType = RANDOM;
			} else {
				int length = valueCounts.size();

				int runningTotal = 0;
				values = new String[length];
				cumulativeFrequency = new int[length];
				for (int i = 0; i < length; i++) {
					values[i] = valueCounts.get(i).getValue();
					int frequency;
					if (doUniformSampling) {
						frequency = 1;
					} else {
						frequency = valueCounts.get(i).getFrequency();
					}
					runningTotal += frequency;
					cumulativeFrequency[i] = runningTotal;
				}
				totalFrequency = runningTotal;
				generatorType = REGULAR;
			}

			if (forcePrimaryKey) {
				generatorType = PRIMARY_KEY;
				pk_cursor = 0;
			}
		}

		public String generate() {
			if (generatorType == RANDOM) { // Random generate a string:
				if (isVarChar(type)) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < length; i++)
						sb.append(Character.toChars(65 + random.nextInt(26)));
					return sb.toString();
				} else if (isInt(type)) {
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < length; i++)
						sb.append(Character.toChars(48 + random.nextInt(10)));
					return sb.toString();
				} else if (type.equals("Date")) // todo: add code
					return "";
				else if (type.equals("Real")) // todo: add code
					return "";
				else if (type.equals("Empty"))
					return "";
				else
					return "";
			} else if (generatorType == PRIMARY_KEY) { // Pick the next value:
				String value = values[pk_cursor];
				pk_cursor++;
				if (pk_cursor >= values.length) {
					// Loop back to the first (not primary key anymore!)
					pk_cursor = 0;
				}
				return value;
			} else { // Sample from values:
				int index = random.nextInt(totalFrequency);
				int i = 0;
				while (i < values.length - 1 && cumulativeFrequency[i] <= index)
					i++;
				if (!type.equals("VarChar") && values[i].trim().length() == 0)
					return "";
				return values[i];
			}
		}
	}

}
