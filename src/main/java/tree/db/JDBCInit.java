package tree.db;

import java.sql.*;

/**
 * Created by Admin on 8/16/2017.
 */
public class JDBCInit {
    private static Connection conn;

    public static void init() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tree",
                    "tree", "%5P3m9o*3StQ");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return conn;
    }

    public static boolean createTable(String tableName, String... fields) {
        String tableToCreate = "CREATE TABLE " +  tableName + " (";
        for (String field : fields) {
            tableToCreate += field + ",";
        }
        tableToCreate += ")";
        try {
            Statement statement = conn.createStatement();
            //The next line has the issue
            statement.executeUpdate(tableToCreate);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

    public static boolean hasTable(String tableName) {
        DatabaseMetaData dbm = null;
        try {
            dbm = conn.getMetaData();
            // check if "employee" table is there
            ResultSet tables =
                    dbm.getTables(null, null,
                            tableName, null);
            if (tables.next()) {
                return true;
            }
            else {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void insertIntoGuildTable(String table, int numFields, int numStatements) {
        try {
            Statement st = conn.createStatement();
            st.executeUpdate("INSERT INTO " +
                            table +
                    " VALUES " +
                    "(1001, 'Simpson', 'Mr.', 'Springfield', 2001)");
            st.executeUpdate("INSERT INTO Customers " +
                    "VALUES (1002, 'McBeal', 'Ms.', 'Boston', 2004)");
            st.executeUpdate("INSERT INTO Customers " +
                    "VALUES (1003, 'Flinstone', 'Mr.', 'Bedrock', 2003)");
            st.executeUpdate("INSERT INTO Customers " +
                    "VALUES (1004, 'Cramden', 'Mr.', 'New York', 2001)");
//            conn.close();
        } catch (SQLException e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        }
}
}
