package db;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by jiejunluo on 3/3/17.
 */
public class Data {
    public HashMap<String, Table> Tables;
    public Data(){
        this.Tables = new HashMap<String, Table>();
    }
    public void addTable(String tableName, Table t){
        this.Tables.put(tableName, t);
    }

    public String Create(String tableName, String[] cols) {
        Table newTable = new Table(tableName);
        for (int i = 0; i < cols.length; i++) {
            String[] col = cols[i].split("\\s+");
            if (col.length != 2) {
                return "ERROR: You should enter the column name and its type!";
            }
            if (col[1].equals("int") || col[1].equals("string")|| col[1].equals("float")) {
                newTable.addColumn(col[0], col[1]);
            } else {
                return "ERROR: Not an available type!";
            }
        }
        Tables.put(tableName, newTable);
        return "";
    }
    public String Insert(String tableName, String[] rowValues){
        if (Tables.containsKey(tableName)) {
            Table currentTable = Tables.get(tableName);
            if (rowValues.length != currentTable.numberOfColumns){
                return "ERROR: Doesn't match the table";
            } else {

                for (int i = 0; i < currentTable.numberOfColumns; i++) {
                    String currentName = currentTable.columnNames.get(i);
                    Col currentCol = currentTable.columns.get(currentName);
                    String elementType = checkType(rowValues[i]);
                    if (elementType.equals(currentCol.type)) {
                        if (elementType.equals("float")){
                            float f = Float.parseFloat(rowValues[i]);
                            currentCol.insert(String.format("%.3f", f));
                        } else {
                            currentCol.insert(rowValues[i]);
                        }
                    } else if (rowValues[i].equals("NOVALUE")) {
                        currentCol.insert("NOVALUE");
                    }
                    else {
                        return "ERROR: Not an available type";
                    }
                }
                return "";
            }
        }
        else {
            return "ERROR: Table doesn't exist";
        }
    }
    public String Drop(String tableName){
        if(this.Tables.isEmpty()){
            return"ERROR: There is no table in database";
        } else if (this.Tables.containsKey(tableName)) {
            this.Tables.remove(tableName);
            return "";
        } else {
            return "ERROR: There is no " + tableName + " in database !";
        }
    }
    public String Load(String tableName) {
        try {

            FileReader r = new FileReader(tableName + ".tbl");
            Scanner scan = new Scanner(r);
            scan.useDelimiter("\\s*\n\\s*");
            String columnName = scan.next();
            String[] columnNames = columnName.split("\\s*,\\s*");
            Create(tableName, columnNames);
            while (scan.hasNext()) {
                Insert(tableName, scan.next().split("\\s*,\\s*"));
            }
            return "";
        }
        catch (FileNotFoundException e){return "ERROR: There is no such file in this directory!";}
    }
    public String Print(String tableName) {
        if (!Tables.containsKey(tableName)) {
            return "ERROR: Table doesn't exist!";
        } else {
            String returnString = "";
            // gather column names
            Table currentTable = Tables.get(tableName);
            for (int i = 0; i < currentTable.numberOfColumns; i++) {
                Col currentColumn = currentTable.columns.get(currentTable.columnNames.get(i));
                if (i != currentTable.numberOfColumns - 1) {
                    returnString = returnString + currentColumn.name + " " + currentColumn.type + ",";
                } else {
                    returnString = returnString + currentColumn.name + " " + currentColumn.type + "\n";
                }
            }

            // gather all the rowValues with nested forloop
            int numberOfRows = currentTable.columns.get(currentTable.columnNames.get(0)).numberOfElements;
            for (int j = 0; j < numberOfRows; j++) {
                for (int i = 0; i < currentTable.numberOfColumns; i++) {
                    Col currentColumn = currentTable.columns.get(currentTable.columnNames.get(i));
                    if (i != currentTable.numberOfColumns - 1) {
                        returnString = returnString + currentColumn.elements.get(j) + ",";
                    } else {
                        returnString = returnString + currentColumn.elements.get(j) + "\n";
                    }
                }
            }
            return returnString;
        }
    }

    public String Store (String tableName){
        if (this.Tables.containsKey(tableName)) {
            try {
                File file = new File(tableName + ".tbl");
                file.createNewFile();
                FileWriter writer = new FileWriter(file);
                Table currentTable = Tables.get(tableName);
                for (int i = 0; i < currentTable.numberOfColumns; i++) {
                    Col currentColumn = currentTable.columns.get(currentTable.columnNames.get(i));
                    if (i != currentTable.numberOfColumns - 1) {
                        writer.write(currentColumn.name + " " + currentColumn.type + ",");
                    } else {
                        writer.write(currentColumn.name + " " + currentColumn.type + "\n");
                    }
                }
                // print all the rowValues with nested forloop
                int numberOfRows = currentTable.columns.get(currentTable.columnNames.get(0)).numberOfElements;
                for (int j = 0; j < numberOfRows; j++) {
                    for (int i = 0; i < currentTable.numberOfColumns; i++) {
                        Col currentColumn = currentTable.columns.get(currentTable.columnNames.get(i));
                        if (i != currentTable.numberOfColumns - 1) {
                            writer.write(currentColumn.elements.get(j) + ",");
                        } else {
                            writer.write(currentColumn.elements.get(j) + "\n");
                        }
                    }
                }
                writer.flush();
                writer.close();
                return "";
            } catch (IOException E) {
                return "ERROR: "+tableName + " already exists !";
            }
        } else {
            return "ERROR: "+ tableName + " doesn't exist in database !";
        }
    }

    public void Join(String table1, String table2) {
        Table temp = new Table("temp");
        this.Tables.put("temp", temp);
        Table t1 = this.Tables.get(table1);
        Table t2 = this.Tables.get(table2);

        ArrayList<String> commonCol = new ArrayList<String>();
        for (int i = 0; i < t1.numberOfColumns; i++) {

            if (t2.columnNames.contains(t1.columnNames.get(i))) {
                Col currentCol = t1.getCol(i);
                temp.addColumn(currentCol.name, currentCol.type);
                commonCol.add(currentCol.name);
            }
        }
        for (int i = 0; i < t1.numberOfColumns; i++) {
            if (!temp.columnNames.contains(t1.columnNames.get(i))) {
                Col currentCol = t1.getCol(i);
                temp.addColumn(currentCol.name, currentCol.type);
            }
        }
        for (int i = 0; i < t2.numberOfColumns; i++) {
            if (!temp.columnNames.contains(t2.columnNames.get(i))) {
                Col currentCol = t2.getCol(i);
                temp.addColumn(currentCol.name, currentCol.type);
            }
        }
        for (int i = 0; i < t1.numberOfRows(); i++) {
            for (int j = 0; j < t2.numberOfRows(); j++) {
                joinHelper(t1.getRow(i), t2.getRow(j), commonCol, temp, commonCol);
            }
        }
        if (temp.numberOfRows() == 0) {
            temp = new Table("temp");
            this.Tables.put("temp", temp);
        }
    }






    public void joinHelper(HashMap<String,String> row1, HashMap<String,String> row2, ArrayList<String> Check,Table temp, ArrayList<String> hold) {

        ArrayList<String> toCheck = new ArrayList<String>(Check);
        if (toCheck.size() == 0) {
            temp.insertByRow(row1);
            for (int i = 0; i < hold.size() ;i++){
                row2.remove(hold.get(i));
            }
            temp.insertByRow(row2);
        } else{
            String colName = toCheck.get(0);
            if (row1.get(colName).equals(row2.get(colName))) {
                toCheck.remove(0);
                joinHelper(row1, row2, toCheck,temp,hold);
            }
        }
    }


    public String checkType(String toCheck){
        String regexInt = "\\s*-?\\d+\\s*";
        String regexFloat = "\\s*-?\\d+\\.\\d+\\s*";
        String regexString = "(\\s*.+\\s*)+";
        if (toCheck.matches(regexInt)){
            return "int";
        } else if (toCheck.matches(regexFloat)) {
            return "float";
        } else if (toCheck.matches(regexString)){
            return "string";
        } else {
            return "error";
        }
    }
}