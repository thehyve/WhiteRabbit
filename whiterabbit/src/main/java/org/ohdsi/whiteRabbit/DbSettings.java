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
package org.ohdsi.whiteRabbit;

import org.apache.commons.csv.CSVFormat;
import org.ohdsi.databases.DbType;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.utilities.files.IniFile;

import java.util.ArrayList;
import java.util.List;


public class DbSettings {
	public static int	DATABASE	= 1;
	public static int	CSVFILES	= 2;
	
	public int			dataType;
	public List<String>	tables		= new ArrayList<String>();
	
	// Database settings
	public DbType		dbType;
	public String		user;
	public String		password;
	public String		database;
	public String		server;
	public String		domain;
	
	// CSV file settings
	public char			delimiter	= ',';
	public CSVFormat	csvFormat	= CSVFormat.RFC4180;

	DbSettings() {
	}

	DbSettings(IniFile iniFile) {
		if (iniFile.get("DATA_TYPE").equalsIgnoreCase(ResourceType.DELIMITED.label)) {
			this.dataType = DbSettings.CSVFILES;
			setDelimiter(iniFile.get("DELIMITER"));
		} else {
			this.dataType = DbSettings.DATABASE;
			this.user = iniFile.get("USER_NAME");
			this.password = iniFile.get("PASSWORD");
			this.server = iniFile.get("SERVER_LOCATION");
			this.database = iniFile.get("DATABASE_NAME");

			String dataType = iniFile.get("DATA_TYPE");
			this.setDbType(dataType);

			if (this.dbType.equals(DbType.MSSQL) || this.dbType.equals(DbType.PDW)) {
				parseUserField(this.user);
			}
		}
		if (iniFile.get("TABLES_TO_SCAN").equalsIgnoreCase("*")) {
			try (RichConnection connection = new RichConnection(this.server, this.domain, this.user, this.password, this.dbType)) {
				this.tables.addAll(connection.getTableNames(this.database));
			}
		} else {
			for (String table : iniFile.get("TABLES_TO_SCAN").split(",")) {
				if (this.dataType == DbSettings.CSVFILES)
					table = iniFile.get("WORKING_FOLDER") + "/" + table;
				this.tables.add(table);
			}
		}
	}

	DbSettings(String sourceType, String user, String password,	String server, String delimiter, String database) {
		if (sourceType.equals(ResourceType.DELIMITED.label)) {
			this.dataType = DbSettings.CSVFILES;
			setDelimiter(delimiter);
		} else {
			this.dataType = DbSettings.DATABASE;
			this.user = user;
			this.password = password;
			this.server = server;
			this.database = database;
			setDbType(sourceType);
			if (this.dbType.equals(DbType.MSSQL) || this.dbType.equals(DbType.PDW)) {
				parseUserField(user);
			}
		}
	}

	static DbSettings createInstanceFromTargetFields(String resourceType, String user, String password,
													 String server, String csvFormat, String database) {
		DbSettings dbSettings = new DbSettings();

		if (resourceType.equals(ResourceType.DELIMITED.label)) {
			dbSettings.dataType = DbSettings.CSVFILES;
			dbSettings.setCsvFormat(csvFormat);
		} else {
			dbSettings.dataType = DbSettings.DATABASE;
			dbSettings.user = user;
			dbSettings.password = password;
			dbSettings.server = server;
			dbSettings.database = database;
			dbSettings.setDbType(resourceType);
			if (dbSettings.dbType.equals(DbType.MSSQL) || dbSettings.dbType.equals(DbType.PDW)) {
				dbSettings.parseUserField(user);
			}
		}
		return dbSettings;
	}

	private void parseUserField (String userField) {
		if (userField.length() != 0) { // Not using windows authentication
			String[] parts = userField.split("/");
			if (parts.length == 2) {
				this.user = parts[1];
				this.domain = parts[0];
			}
		}
	}

	private void setDbType(String dataType) {
		if (dataType.equalsIgnoreCase(ResourceType.MYSQL.label))
			this.dbType = DbType.MYSQL;
		else if (dataType.equalsIgnoreCase(ResourceType.ORACLE.label))
			this.dbType = DbType.ORACLE;
		else if (dataType.equalsIgnoreCase(ResourceType.POSTGRESQL.label))
			this.dbType = DbType.POSTGRESQL;
		else if (dataType.equalsIgnoreCase(ResourceType.REDSHIFT.label))
			this.dbType = DbType.REDSHIFT;
		else if (dataType.equalsIgnoreCase(ResourceType.MSSQL.label))
			this.dbType = DbType.MSSQL;
		else if (dataType.equalsIgnoreCase(ResourceType.PDW.label))
			this.dbType = DbType.PDW;
		else if (dataType.equalsIgnoreCase(ResourceType.MSACCESS.label))
			this.dbType = DbType.MSACCESS;
		else if (dataType.equalsIgnoreCase(ResourceType.TERADATA.label))
			this.dbType = DbType.TERADATA;
	}

	private void setDelimiter(String delimiter) {
		if (delimiter.length() == 0)
			throw new IllegalArgumentException("Delimiter cannot be absent.");
		if (delimiter.equalsIgnoreCase("tab"))
			this.delimiter = '\t';
		else
			this.delimiter = delimiter.charAt(0);
	}

	private void setCsvFormat(String csvFormat) {

		switch(csvFormat) {
			case "Default (comma, CRLF)":
				this.csvFormat = CSVFormat.DEFAULT;
				break;
			case "RFC4180":
				this.csvFormat = CSVFormat.RFC4180;
				break;
			case "Excel CSV":
				this.csvFormat = CSVFormat.EXCEL;
				break;
			case "TDF (tab, CRLF)":
				this.csvFormat = CSVFormat.TDF;
				break;
			case "MySQL (tab, LF)":
				this.csvFormat = CSVFormat.MYSQL;
				break;
			default:
				this.csvFormat = CSVFormat.RFC4180;
		}
	}
}
