package db;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;

/**
 * Created by jiejunluo on 3/1/17.
 */
public class Table {
    public LinkedHashMap<String, Col> columns;
    public String name;
    public int numberOfColumns;
    public ArrayList<String > columnNames;
    public Table(String tableName) {
        this.name = tableName;
        this.numberOfColumns = 0;
        this.columns = new LinkedHashMap<String, Col>();
        this.columnNames = new ArrayList<String>();
    }
    public String addColumn(String columnName, String columnType) {
        if (this.columnNames.contains(columnName)) {
            return "ERROR: Duplicate column name: "+ columnName;
        }
        Col newColumn = new Col(columnName, columnType);
        columns.put(columnName, newColumn);
        columnNames.add(columnName);
        this.numberOfColumns += 1;
        return "";
    }
    public Col getCol(int i) {
        return this.columns.get(this.columnNames.get(i));
    }
    public String findElement(int row, String colName) {
        return this.columns.get(colName).get(row);
    }
    public HashMap<String, String> getRow(int i) {
        HashMap<String, String> row = new HashMap<String, String>();
        for (int j = 0; j < this.numberOfColumns; j++) {
            row.put(this.columnNames.get(j), this.getCol(j).get(i));
        }
        return row;
    }
    public void insertByRow(HashMap<String,String> row) {
        for (int i = 0; i < this.columns.size(); i++) {
            String currentName = this.columnNames.get(i);
            if (row.containsKey(currentName)) {
                this.columns.get(currentName).insert(row.get(currentName));
            }
        }
    }
    public int numberOfRows(){
        return this.getCol(0).numberOfElements;
    }

    public void Copy(Table original){
        this.name = original.name;
        for (int i = 0; i < original.numberOfColumns; i++){
            Col addCol = original.getCol(i);
            this.addColumn(addCol.name,addCol.type);
        }
        for (int i = 0; i < original.numberOfRows(); i++){
            this.insertByRow(original.getRow(i));
        }
    }
}