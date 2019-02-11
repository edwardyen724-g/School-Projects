package db;




public class Database {
    Data myData;

    public Database() {
        this.myData = new Data();

        // YOUR CODE HERE
    }

    public String transact(String query) {
        String returnString;
        Parse parseResult = new Parse();
        parseResult.addData(this.myData);
        returnString = parseResult.eval(query);
        return returnString;
    }
}