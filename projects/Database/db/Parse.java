package db;

import com.sun.org.apache.bcel.internal.generic.Select;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parse {
    public Data myData;
    public void addData(Data db){
        myData = db;
    }
    // Various common constructs, simplifies parsing.
    private static final String REST  = "\\s*(.*)\\s*",
            COMMA = "\\s*,\\s*",
            AND   = "\\s+and\\s+";

    // Stage 1 syntax, contains the command name.
    private static final Pattern CREATE_CMD = Pattern.compile("create table " + REST),
            LOAD_CMD   = Pattern.compile("load " + REST),
            STORE_CMD  = Pattern.compile("store " + REST),
            DROP_CMD   = Pattern.compile("drop table " + REST),
            INSERT_CMD = Pattern.compile("insert into " + REST),
            PRINT_CMD  = Pattern.compile("print " + REST),
            SELECT_CMD = Pattern.compile("select " + REST);

    // Stage 2 syntax, contains the clauses of commands.
    private static final Pattern CREATE_NEW  = Pattern.compile("(\\S+)\\s+\\((\\S+\\s+\\S+\\s*" +
            "(?:,\\s*\\S+\\s+\\S+\\s*)*)\\)"),
            SELECT_CLS  = Pattern.compile("([^,]+?(?:,[^,]+?)*)\\s+from\\s+" +
                    "(\\S+\\s*(?:,\\s*\\S+\\s*)*)(?:\\s+where\\s+" +
                    "([\\w\\s+\\-*/'<>=!.]+?(?:\\s+and\\s+" +
                    "[\\w\\s+\\-*/'<>=!.]+?)*))?"),
            CREATE_SEL  = Pattern.compile("(\\S+)\\s+as select\\s+" +
                    SELECT_CLS.pattern()),
            INSERT_CLS  = Pattern.compile("(\\S+)\\s+values\\s+(.+?" +
                    "\\s*(?:,\\s*.+?\\s*)*)");

    public void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Expected a single query argument");
            return;
        }
        eval(args[0]);
    }

    public String eval(String query) {
        Matcher m;
        if ((m = CREATE_CMD.matcher(query)).matches()) {
            return createTable(m.group(1));
        } else if ((m = LOAD_CMD.matcher(query)).matches()) {
            return loadTable(m.group(1));
        } else if ((m = STORE_CMD.matcher(query)).matches()) {
            return storeTable(m.group(1));
        } else if ((m = DROP_CMD.matcher(query)).matches()) {
            return dropTable(m.group(1));
        } else if ((m = INSERT_CMD.matcher(query)).matches()) {
            return insertRow(m.group(1));
        } else if ((m = PRINT_CMD.matcher(query)).matches()) {
            return printTable(m.group(1));
        } else if ((m = SELECT_CMD.matcher(query)).matches()) {
            return select(m.group(1));
        } else {
            return "ERROR: Not a valid command";
        }
    }

    private String createTable(String expr) {
        Matcher m;
        if ((m = CREATE_NEW.matcher(expr)).matches()) {
            return createNewTable(m.group(1), m.group(2).split(COMMA));
        } else if ((m = CREATE_SEL.matcher(expr)).matches()) {
            return createSelectedTable(m.group(1), m.group(2), m.group(3), m.group(4));
        } else {
            return "ERROR: Cannot create a table!";
        }
    }

    private String createNewTable(String name, String[] cols) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < cols.length-1; i++) {
            joiner.add(cols[i]);
        }

        String colSentence = joiner.toString() + " and " + cols[cols.length-1];
        System.out.printf("You are trying to create a table named %s with the columns %s\n", name, colSentence);
        return this.myData.Create(name,cols);
    }

    private String createSelectedTable(String name, String exprs, String tables, String conds) {
        System.out.printf("You are trying to create a table named %s by selecting these expressions:" +
                " '%s' from the join of these tables: '%s', filtered by these conditions: '%s'\n", name, exprs, tables, conds);
        select(exprs, tables, conds);
        Table newTable = myData.Tables.get("temp");
        myData.Tables.put(name, newTable);
        newTable.name = name;
        return "";
    }

    private String loadTable(String name) {
        System.out.printf("You are trying to load the table named %s\n", name);
        return this.myData.Load(name);
    }

    private String storeTable(String name) {
        System.out.printf("You are trying to store the table named %s\n", name);
        return this.myData.Store(name);
    }

    private String dropTable(String name) {
        System.out.printf("You are trying to drop the table named %s\n", name);
        return this.myData.Drop(name);
    }

    private String insertRow(String expr) {
        Matcher m = INSERT_CLS.matcher(expr);
        if (!m.matches()) {
            return "ERROR: Wrong Insert";
        }

        System.out.printf("You are trying to insert the row \"%s\" into the table %s\n", m.group(2), m.group(1));
        return this.myData.Insert(m.group(1),m.group(2).split(COMMA));
    }

    private String printTable(String name) {
        System.out.printf("You are trying to print the table named %s\n", name);
        return this.myData.Print(name);
    }

    private String select(String expr) {
        Matcher m = SELECT_CLS.matcher(expr);
        if (!m.matches()) {
            return "ERROR: Wrong select";
        }

        return select(m.group(1), m.group(2), m.group(3));
    }

    private String select(String exprs, String tables, String conds) {
        System.out.printf("You are trying to select these expressions:" +
                " '%s' from the join of these tables: '%s', filtered by these conditions: '%s'\n", exprs, tables, conds);
        if (exprs.equals("")) {
            return "ERROR: Wrong select statement";
        }
        String result = "";
        if (exprs.equals("*")) {
            String[] tablesName = tables.split(COMMA);
            if (tablesName.length < 1) {
                return "ERROR: Need at least 1 table!";
            }
            if (tablesName.length == 1) {
                result = myData.Print(tablesName[0]);
                Table temp = myData.Tables.get(tablesName[0]);
                myData.Tables.put("temp",temp);
            } else if (tablesName.length == 2){
                this.myData.Join(tablesName[0], tablesName[1]);
                result = myData.Print("temp");
            } else {
                this.myData.Join(tablesName[0],tablesName[1]);

                result = myData.Print("temp");
                Table holdT = new Table("holdT");
                myData.Tables.put("join", holdT);
                holdT.Copy(myData.Tables.get("temp"));
                String secondTable = tablesName[2].replaceAll("\\s+","");
                this.myData.Join("join", secondTable);
                result = myData.Print("temp");
            }
            //* xia ji ba xie

        } else {
            String[] tablesName = tables.split(COMMA);
            Table joined = new Table("joined");
            if (tablesName.length == 1){
                result = myData.Print(tablesName[0]);
                Table temp = myData.Tables.get(tablesName[0]);
                myData.Tables.put("temp",temp);
            } else {
                this.myData.Join(tablesName[0],tablesName[1]);
            }

            joined = this.myData.Tables.get("temp");
            // check column expressions
            String[] cols = exprs.split(COMMA);
            String[] alias = new String[cols.length];
            String[][] PoArithmetic = new String[cols.length][];
            String[] columnType = new String[cols.length];
            Table resultT = new Table("resultT");
            myData.Tables.put("resultT",resultT);

            // find all alias or column names
            for (int i = 0; i < cols.length; i++) {
                alias[i] = findAlias(cols[i]);

            }
            // find all the operators and their items
            for (int i = 0; i < cols.length; i++) {
                PoArithmetic[i] = findArithmetic(cols[i]);
            }
            // make sure both items are the same type or ints
            for (int i = 0; i < cols.length; i++) {
                if (PoArithmetic[i][0] == "+") {
                    if (joined.columns.get(PoArithmetic[i][1]).type.equals("string")) {
                        if (!joined.columns.get(PoArithmetic[i][2]).type.equals("string")) {
                            return "ERROR: Not the same Type";
                        }
                    }
                } else if (PoArithmetic[i][0] != "No Arithmetic") {
                    if ((!joined.columnNames.contains(PoArithmetic[i][1]))){
                        return "ERROR: Not a element of table";
                    }
                    if (joined.columns.get(PoArithmetic[i][1]).type.equals("string") || joined.columns.get(PoArithmetic[i][2]).type.equals("string")) {
                        return "ERROR: invalid";
                    }
                }
            }
            //find all the column types
            for (int i = 0; i < cols.length; i++) {
                if (PoArithmetic[i][0] != "No Arithmetic") {
                    if (!myData.checkType(PoArithmetic[i][1]).equals("string") && !myData.checkType(PoArithmetic[i][2]).equals("string")){
                        return "ERROR: No two numbers";
                    } else if (myData.checkType(PoArithmetic[i][1]).equals("string")&& !myData.checkType(PoArithmetic[i][2]).equals("string")) {
                        if (joined.columns.get(PoArithmetic[i][1]).equals("string")) {
                            return "ERROR: Cannot operate string with number";
                        } else if (joined.columns.get(PoArithmetic[i][1]).equals("float") || myData.checkType(PoArithmetic[i][2]).equals("float")) {
                            columnType[i] = "float";
                        } else {
                            columnType[i] = "int";
                        }
                    }else if (!myData.checkType(PoArithmetic[i][1]).equals("string")&& myData.checkType(PoArithmetic[i][2]).equals("string")){
                        if (joined.columns.get(PoArithmetic[i][2]).equals("string")){
                            return "ERROR: Cannot operate string with number";
                        } else if (joined.columns.get(PoArithmetic[i][2]).equals("float") || myData.checkType(PoArithmetic[i][1]).equals("float")){
                            columnType[i] = "float";
                        } else {
                            columnType[i] = "int";
                        }
                    }

                    else{
                        if (joined.columns.get(PoArithmetic[i][1]).type == "float" || joined.columns.get(PoArithmetic[i][2]).type == "float") {
                            columnType[i] = "float";
                        } else {
                            columnType[i] = joined.columns.get(PoArithmetic[i][1]).type;

                        }
                    }
                } else {
                    columnType[i] = joined.columns.get(joined.columnNames.get(i)).type;
                }
            }

            // add the columnNames for resulting table
            for (int i = 0; i < cols.length; i++) {
                resultT.addColumn(alias[i], columnType[i]);
            }
            // calculate each column and add them
            for (int i = 0; i < cols.length; i++) {
                if (PoArithmetic[i][0].equals("No Arithmetic")) {
                    resultT.columns.put(alias[i], joined.columns.get(alias[i]));
                } else {
                    resultT.columns.put(alias[i], calEval(PoArithmetic[i][1], PoArithmetic[i][2], PoArithmetic[i][0], columnType[i], joined,alias[i]));
                }

            }
            myData.Tables.put("temp", resultT);
            result = myData.Print("temp");
        }
        //* codition!!!!!!!

        if (conds == null){
            return result;
        } else {
            String[] condList = conds.split(AND);
            for (int i = 0; i < condList.length; i++) {
                result = condition(condList[i],"temp");
                Table hold = myData.Tables.get("resultTable");
                myData.Tables.put("temp", hold);
            }
            return result;
        }
    }



    public String condition (String conds, String tablesName){
        String[] condList = conds.split(AND);
        Table resultTable = new Table("resultTable");
        myData.Tables.put("resultTable", resultTable);
        Table thisTable = myData.Tables.get(tablesName);
        for (int i = 0; i < thisTable.numberOfColumns ;i ++){
            resultTable.addColumn(thisTable.columns.get(thisTable.columnNames.get(i)).name, thisTable.columns.get(thisTable.columnNames.get(i)).type);
        }
        String currentCond = conds;
        String[] condSentence = findComparison(currentCond);
        if (condSentence[0].equals(">")) {
            if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                if ((col1.type.equals("string") && (!col2.type.equals("string"))) || ((!col1.type.equals("string")) && col2.type.equals("string"))) {
                    return "ERROR: Cannot compare string!";
                } else if (col1.type.equals("string")&&col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        String value1 = col1.get(j);
                        String value2 = col2.get(j);
                        if (value1.compareTo(value2)>0) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                } else if (!col1.type.equals("string")&&!col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        float value1 = Float.parseFloat(col1.get(j));
                        float value2 = Float.parseFloat(col2.get(j));
                        if (value1 > value2) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && !myData.checkType(condSentence[2]).equals("string")) {
                float toCompare = Float.parseFloat(condSentence[2]);
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("string")) {
                    return "ERROR: Cannot compare a string with a number";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    float value = Float.parseFloat(thisColumn.get(j));
                    if (value > toCompare) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && myData.checkType(condSentence[2]).equals("string")){
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("int")|| thisColumn.type.equals("float")){
                    return  "ERROR: Cannot compare a number with string";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.compareTo(condSentence[2]) > 0) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }

            }
            else {
                return "ERROR: Need a valid compare sentence for >!";
            }
        } else if (condSentence[0].equals("<")) {
            if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                if ((col1.type.equals("string") && (!col2.type.equals("string"))) || ((!col1.type.equals("string"))&& col2.type.equals("string"))) {
                    return "ERROR: Cannot compare string!";
                } else if (col1.type.equals("string")&&col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        String value1 = col1.get(j);
                        String value2 = col2.get(j);
                        if (value1.compareTo(value2) < 0) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                } else if (!col1.type.equals("string")&&!col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        float value1 = Float.parseFloat(col1.get(j));
                        float value2 = Float.parseFloat(col2.get(j));
                        if (value1 < value2) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && !myData.checkType(condSentence[2]).equals("string")) {
                float toCompare = Float.parseFloat(condSentence[2]);
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("string")) {
                    return "ERROR: Cannot compare a string with a number";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    float value = Float.parseFloat(thisColumn.get(j));
                    if (value < toCompare) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            }
            else if (thisTable.columnNames.contains(condSentence[1]) && myData.checkType(condSentence[2]).equals("string")){
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("int")|| thisColumn.type.equals("float")){
                    return  "ERROR: Cannot compare a number with string";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.compareTo(condSentence[2]) < 0) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }

            }
            else {
                return "ERROR: Need a valid compare sentence for <!";
            }
        } else if (condSentence[0].equals(">=")) {
            if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                if ((col1.type.equals("string") && (!col2.type.equals("string"))) || ((!col1.type.equals("string")) && col2.type.equals("string"))) {
                    return "ERROR: Cannot compare string!";
                } else if (col1.type.equals("string")&&col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        String value1 = col1.get(j);
                        String value2 = col2.get(j);
                        if (value1.compareTo(value2) >= 0) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                } else if (!col1.type.equals("string")&&!col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        float value1 = Float.parseFloat(col1.get(j));
                        float value2 = Float.parseFloat(col2.get(j));
                        if (value1 >= value2) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                }

            }
            else if (thisTable.columnNames.contains(condSentence[1]) && !myData.checkType(condSentence[2]).equals("string")) {
                float toCompare = Float.parseFloat(condSentence[2]);
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("string")) {

                    return "ERROR: Cannot compare a string with a number";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    float value = Float.parseFloat(thisColumn.get(j));
                    if (value >= toCompare) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && myData.checkType(condSentence[2]).equals("string")){
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("int")|| thisColumn.type.equals("float")){
                    return  "ERROR: Cannot compare a number with string";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.compareTo(condSentence[2]) >= 0) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }

            } else {
                return "ERROR: Need a valid compare sentence for >=!";
            }
        } else if (condSentence[0].equals("<=")) {
            if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                if ((col1.type.equals("string") && (!col2.type.equals("string"))) || ((!col1.type.equals("string")) && col2.type.equals("string"))) {
                    return "ERROR: Cannot compare string!";
                } else if (col1.type.equals("string")&&col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        String value1 = col1.get(j);
                        String value2 = col2.get(j);
                        if (value1.compareTo(value2) <= 0) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                } else if (!col1.type.equals("string")&&!col2.type.equals("string")) {
                    for (int j = 0; j < col1.numberOfElements; j++) {
                        float value1 = Float.parseFloat(col1.get(j));
                        float value2 = Float.parseFloat(col2.get(j));
                        if (value1 <= value2) {
                            resultTable.insertByRow(thisTable.getRow(j));
                        }
                    }
                }
            }
            else if (thisTable.columnNames.contains(condSentence[1]) && !myData.checkType(condSentence[2]).equals("string")) {
                float toCompare = Float.parseFloat(condSentence[2]);
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("string")) {
                    return "ERROR: Cannot compare a string with a number";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    float value = Float.parseFloat(thisColumn.get(j));
                    if (value <= toCompare) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && myData.checkType(condSentence[2]).equals("string")){
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("int")|| thisColumn.type.equals("float")){
                    return  "ERROR: Cannot compare a number with string";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.compareTo(condSentence[2]) <= 0) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }

            }  else if (thisTable.columnNames.contains(condSentence[1]) && !myData.checkType(condSentence[2]).equals("string")) {
                float toCompare = Float.parseFloat(condSentence[2]);
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("string")) {
                    return "ERROR: Cannot compare a string with a number";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    float value = Float.parseFloat(thisColumn.get(j));
                    if (value <= toCompare) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && myData.checkType(condSentence[2]).equals("string")){
                Col thisColumn = thisTable.columns.get(condSentence[1]);
                if (thisColumn.type.equals("int")|| thisColumn.type.equals("float")){
                    return  "ERROR: Cannot compare a number with string";
                }
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.compareTo(condSentence[2]) <= 0) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }

            }
            else {
                return "ERROR: Need a valid compare sentence for <=!";
            }
        } else if (condSentence[0].equals("==")) {
            if (thisTable.columnNames.contains(condSentence[1]) && !thisTable.columnNames.contains(condSentence[2])) {

                Col thisColumn = thisTable.columns.get(condSentence[1]);
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (value.equals(condSentence[2])) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                for (int j = 0; j < col1.numberOfElements; j++) {
                    String value1 = col1.get(j);
                    String value2 = col2.get(j);
                    if (value1.equals(value2)) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else {
                return "ERROR: Need a valid compare sentence for ==!";
            }
        } else if (condSentence[0].equals("!=")) {
            if (thisTable.columnNames.contains(condSentence[1]) && !thisTable.columnNames.contains(condSentence[2])) {

                Col thisColumn = thisTable.columns.get(condSentence[1]);
                for (int j = 0; j < thisColumn.numberOfElements; j++) {
                    String value = thisColumn.get(j);
                    if (!value.equals(condSentence[2])) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else if (thisTable.columnNames.contains(condSentence[1]) && thisTable.columnNames.contains(condSentence[2])) {
                Col col1 = thisTable.columns.get(condSentence[1]);
                Col col2 = thisTable.columns.get(condSentence[2]);
                for (int j = 0; j < col1.numberOfElements; j++) {
                    String value1 = col1.get(j);
                    String value2 = col2.get(j);
                    if (!value1.equals(value2)) {
                        resultTable.insertByRow(thisTable.getRow(j));
                    }
                }
            } else {
                return "ERROR: Need a totally valid compare sentence!";
            }

        }
        return myData.Print(resultTable.name);
    }
    public String[] findArithmetic(String expr) {
        Matcher m;
        // group1 = first item, group 2 = second item
        Pattern regexSum = Pattern.compile("\\s*(\\S+)\\s*[+]\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexSub = Pattern.compile("\\s*(\\S+)\\s*[-]\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexMul = Pattern.compile("\\s*(\\S+)\\s*[*]\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexDiv = Pattern.compile("\\s*(\\S+)\\s*[/]\\s*(\\S+)\\s*(.*)\\s*");
        String[] result = new String[3];
        if ((m = regexSum.matcher(expr)).matches()) {
            result[1] = m.group(1);
            result[0] = "+";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexSub.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = "-";
            result[2] = m.group(2);
            return result;
        }  else if ((m = regexMul.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = "*";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexDiv.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = "/";
            result[2] = m.group(2);
            return result;
        } else {
            result[0] = "No Arithmetic";
            return result;
        }
    }

    private String findAlias(String expr) {
        Pattern regexAlias = Pattern.compile("\\s*(\\S+)\\s*(.)\\s*(\\S+)\\s+as\\s+(\\S+)\\s*");
        Matcher m;
        String result;
        if ((m = regexAlias.matcher(expr)).matches()) {
            result = m.group(4);
        } else {
            result = expr;
        }
        return result;
    }

    public String[] findComparison(String expr) {
        Matcher m;
        // group1 = first item, group 2 = second item
        Pattern regexG = Pattern.compile("\\s*(\\S+)\\s*[>]\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexS = Pattern.compile("\\s*(\\S+)\\s*[<]\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexGE = Pattern.compile("\\s*(\\S+)\\s*>=\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexSE = Pattern.compile("\\s*(\\S+)\\s*<=\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexE = Pattern.compile("\\s*(\\S+)\\s*==\\s*(\\S+)\\s*(.*)\\s*");
        Pattern regexNE = Pattern.compile("\\s*(\\S+)\\s*!=\\s*(\\S+)\\s*(.*)\\s*");
        String[] result = new String[3];
        if ((m = regexGE.matcher(expr)).matches()) {
            result[1] = m.group(1);
            result[0] = ">=";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexSE.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = "<=";
            result[2] = m.group(2);
            return result;
        }  else if ((m = regexG.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = ">";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexS.matcher(expr)).matches()){
            result[1] = m.group(1);
            result[0] = "<";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexE.matcher(expr)).matches()) {
            result[1] = m.group(1);
            result[0] = "==";
            result[2] = m.group(2);
            return result;
        } else if ((m = regexNE.matcher(expr)).matches()) {
            result[1] = m.group(1);
            result[0] = "!=";
            result[2] = m.group(2);
            return result;
        } else {
            result[0] = "No Comparison";
            return result;
        }
    }
    private Col calEval(String col1Name, String col2Name, String oper, String colType, Table joined, String col) {
        if (myData.checkType(col1Name) != "string" && myData.checkType(col2Name).equals("string")) {
            return calCol2Constant(joined.columns.get(col2Name), col1Name, oper, colType,col);
        } else if (myData.checkType(col1Name).equals("string") && myData.checkType(col2Name) != "string") {
            return calCol2Constant(joined.columns.get(col1Name), col2Name, oper, colType,col);
        } else {
            return calCol2Col(joined.columns.get(col2Name), joined.columns.get(col1Name), oper, colType,col);
        }
    }



    private String addStr2Str(String str1, String str2) {
        return str2 + str1;
    }

    private Col calCol2Col(Col col1, Col col2, String oper, String colType,String name) {
        Col resultCol = new Col(name, colType);
        if (colType.equals("int")) {
            for (int i = 0; i < col1.numberOfElements; i++) {
                resultCol.insert(Integer.toString((calNum2Num(Integer.parseInt(col1.get(i)), Integer.parseInt(col2.get(i)), oper))));
            }
            return resultCol;
        } else if (colType.equals("float")) {
            for (int i = 0; i < col1.numberOfElements; i++) {
                resultCol.insert(Float.toString((calNum2Num((int) Float.parseFloat(col1.get(i)), (int) Float.parseFloat(col2.get(i)), oper))));
            }
            return resultCol;
        } else {
            for (int i = 0; i < col1.numberOfElements; i++) {
                resultCol.insert(addStr2Str(col1.get(i), col2.get(i)));
            }
            return resultCol;
        }
    }
    private Col calCol2Constant(Col col, String num, String oper, String colType,String name) {
        Col resultCol = new Col(name, colType);
        if (colType.equals("int")) {
            for (int i = 0; i < col.numberOfElements; i++) {
                resultCol.insert(Integer.toString((calNum2Num(Integer.parseInt(col.get(i)), Integer.parseInt(num), oper))));
            }
            return resultCol;
        } else {
            for (int i = 0; i < col.numberOfElements; i++) {
                resultCol.insert(Float.toString((calNum2Num((int) Float.parseFloat(col.get(i)), (int) Float.parseFloat(num), oper))));
            }
            return resultCol;
        }
    }
    private int calNum2Num(int num1, int num2, String oper) {
        if (oper == "+") {
            return num1 + num2;
        } else if (oper == "-") {
            return num1 - num2;
        } else if (oper == "*") {
            return num1 * num2;
        } else {
            return num1 / num2;
        }
    }

}