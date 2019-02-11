package db;

/**
 * Created by jiejunluo on 3/2/17.
 */
import static org.junit.Assert.*;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DBTest {
    /* @Test
     public void testCol(){
         Col testCol = new Col("x1", "int");
         ArrayList<String> emptyArray = new ArrayList<String>();
         assertEquals(emptyArray,testCol.elements);
         assertEquals("x1",testCol.name);
         assertEquals("int",testCol.type);
         testCol.insert("why");
         testCol.insert("so");
         testCol.insert("Hard");
         emptyArray.add("why");
         emptyArray.add("so");
         emptyArray.add("Hard");
     }
     @Test
     public void testTable(){
         Table testTable = new Table("T1");
         testTable.addColumn("x1","int");
         testTable.addColumn("x2","string");
         Col testCol = new Col("x1", "int");
         System.out.println(testTable.columnNames);
         System.out.println(testTable.columns.get(testTable.columnNames.get(0)).elements);
         String[] row = new String[] {"1","2"};
         System.out.println(testTable.columns.get(testTable.columnNames.get(1)).elements);
     }
     @Test
     public void testData(){
         Data db = new Data();
         String[] cols = new String[] {"x1 int","x2 int"};
         String[] row = new String[] {"1","2"};
         db.Create("t3", cols);
         System.out.println(db.Tables.get("t3").columnNames);
         db.Insert("t3",row);
         System.out.println(db.Tables.get("t3").columnNames);
         System.out.println(db.Tables.get("t3").columns.get(db.Tables.get("t3").columnNames.get(0)).type);
         db.Load("t1");
         db.Load("t2");
         db.Print("t1");
         db.Print("t2");
         db.Insert("t1", row);
         String t =db.Print("t1");
         System.out.println(t);
     }*/
    @Test
    public void testDB(){
        Database db = new Database();
        db.transact("create table t1 (x int, y int)");
        db.transact("insert into t1 values 1,7");
        db.transact("insert into t1 values 7,5");
        db.transact("create table t2 (x int, a int)");
        db.transact("insert into t2 values 1,5");
        db.transact("insert into t2 values 6,7");
        db.transact("create table t3 (x int, g int)");
        db.transact("insert into t3 values 1,8");
        db.transact("insert into t3 values 5,7");
        db.transact("load records");
        System.out.print(db.transact("select * from t1,t2,t3 "));
        /*System.out.print(db.transact("print t5"));*/


    }
   /*@Test
    public void testWhat() {
       Parse p = new Parse();
       Database db = new Database();
       Pattern regexAlias = Pattern.compile("\\s*(\\S+)\\s*(.)\\s*(\\S+)\\s+as\\s+(\\S+)\\s*");
       Matcher m;
       String result = "";
       if ((m = regexAlias.matcher("x + y as a")).matches()) {
           result = m.group(4);
       }
       System.out.print(result);
   }
    /*@Test
    public void t(){
        ArrayList<Integer> a = new ArrayList<Integer>();
        a.add(1);
        a.add(2);
        a.add(3);
        a.remove(0);
        System.out.print(a);
        System.out.print(a.get(0));
    }*/


}