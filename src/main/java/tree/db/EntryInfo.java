package tree.db;

/**
 * Created by Valued Customer on 9/20/2017.
 */
public class EntryInfo {
    private String columnLabel;
    private int type;

    public EntryInfo(String columnLabel, int type) {
        this.columnLabel = columnLabel;
        this.type = type;
    }

    public String getColumnLabel() {
        return columnLabel;
    }

    public int getType() {
        return type;
    }
}
