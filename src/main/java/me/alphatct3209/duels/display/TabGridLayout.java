package me.alphatct3209.duels.display;

import java.util.ArrayList;
import java.util.List;

/** Flattens four independent columns into the client's four-by-twenty player-list order. */
public final class TabGridLayout
{
    public static final int CELL_COUNT = TabGridParser.COLUMN_COUNT * TabGridParser.MAX_ROWS;

    private TabGridLayout() {}

    public static List<String> cells(List<List<String>> columns)
    {
        if (columns == null || columns.size() != TabGridParser.COLUMN_COUNT)
        {
            throw new IllegalArgumentException("A tab grid must contain exactly four columns");
        }
        List<String> cells = new ArrayList<>(CELL_COUNT);
        for (List<String> column : columns)
        {
            if (column.size() > TabGridParser.MAX_ROWS)
            {
                throw new IllegalArgumentException("A tab column cannot exceed 20 rows");
            }
            for (int row = 0; row < TabGridParser.MAX_ROWS; row++)
            {
                cells.add(row < column.size() ? column.get(row) : "");
            }
        }
        return List.copyOf(cells);
    }
}
