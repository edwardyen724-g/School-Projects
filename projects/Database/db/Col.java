package db;
import java.util.ArrayList;

/**
 * Created by jiejunluo on 3/1/17.
 */
public class Col {
    public String name;
    public ArrayList<String> elements;
    public int numberOfElements;
    public String type;
    public Col(String colName, String colType){
        elements = new ArrayList();
        this.name = colName;
        numberOfElements = 0;
        this.type = colType;
    }
    public void insert(String element) {

        this.elements.add(element);
        this.numberOfElements += 1;

    }
    public String get(int i) {
        return this.elements.get(i);
    }
}
