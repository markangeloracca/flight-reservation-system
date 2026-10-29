package ca.ucalgary.frs.ui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Adapter from a list of domain objects to Swing's {@code TableModel}. Columns are declared as
 * functions, so domain classes stay free of GUI code.
 */
public final class ListTableModel<T> extends AbstractTableModel {

    public record Column<T>(String name, Class<?> type, Function<T, ?> value) {
    }

    private final List<Column<T>> columns;
    private List<T> rows = List.of();

    @SafeVarargs
    public ListTableModel(Column<T>... columns) {
        this.columns = List.of(columns);
    }

    public static <T> Column<T> column(String name, Class<?> type, Function<T, ?> value) {
        return new Column<>(name, type, value);
    }

    public void setRows(List<T> newRows) {
        this.rows = new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    public List<T> rows() {
        return List.copyOf(rows);
    }

    /** Row at a <em>model</em> index (convert view indexes first). */
    public T row(int modelIndex) {
        return rows.get(modelIndex);
    }

    public int indexOf(T item) {
        return rows.indexOf(item);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).name();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns.get(column).type();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            return null;
        }
        return columns.get(columnIndex).value().apply(rows.get(rowIndex));
    }
}
