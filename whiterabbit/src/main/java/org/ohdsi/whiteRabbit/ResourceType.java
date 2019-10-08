package org.ohdsi.whiteRabbit;

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

    public static String[] getAllLabels() {
        ResourceType[] values = values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = values[i].label;
        }
        return labels;
    }
}
