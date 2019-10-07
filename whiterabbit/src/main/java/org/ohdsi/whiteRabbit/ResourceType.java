package org.ohdsi.whiteRabbit;

import java.util.ArrayList;

public enum ResourceType {
    DELIMITED("Delimited text files"),
    MYSQL("MySQL"),
    ORACLE("Oracle"),
    MSSQL("SQL Server"),
    POSTGRESQL("PostgreSQL"),
    MSACCESS("MS Access"),
    PDW("PDW"),
    REDSHIFT("Redshift"),
    TERADATA("Teradata");

    public final String label;

    ResourceType(String label) {
        this.label = label;
    }

    public static ArrayList<String> getAllLabels(){
        ArrayList<String> labels = new ArrayList<>();
        for(ResourceType dt: ResourceType.values()) {
            labels.add(dt.label);
        }
        return labels;
    }
}
