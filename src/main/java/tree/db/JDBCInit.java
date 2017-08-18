package tree.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Created by Admin on 8/16/2017.
 */
public class JDBCInit {

    public void init() {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/tree",
                    "tree", "%5P3m9o*3StQ");

            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery("select * from user");

            while (rs.next()) {
                System.out.println(rs.getString("id") + ", " +
                        rs.getString("username") + ", " +
                        rs.getString("guild"));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}
