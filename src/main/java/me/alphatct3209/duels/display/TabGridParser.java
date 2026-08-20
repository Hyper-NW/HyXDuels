package me.alphatct3209.duels.display;

import java.util.ArrayList;
import java.util.List;

/** Parses ordered {@code column|text} entries into four columns. */
public final class TabGridParser
{
    public static final int COLUMN_COUNT = 4;
    public static final int MAX_ROWS = 20;

    private TabGridParser() {}

    public static List<List<String>> parse(List<String> entries)
    {
        List<List<String>> columns = new ArrayList<>(COLUMN_COUNT);
        for (int column = 0; column < COLUMN_COUNT; column++)
        {
            columns.add(new ArrayList<>());
        }
        if (entries == null)
        {
            return immutable(columns);
        }

        for (int index = 0; index < entries.size(); index++)
        {
            String entry = entries.get(index);
            if (entry == null)
            {
                throw invalid(index, "entry must be text");
            }
            int delimiter = entry.indexOf('|');
            if (delimiter < 0)
            {
                throw invalid(index, "missing first '|' delimiter");
            }
            String rawColumn = entry.substring(0, delimiter).trim();
            int column;
            try
            {
                column = Integer.parseInt(rawColumn);
            }
            catch (NumberFormatException exception)
            {
                throw invalid(index, "column must be a whole number from 1 through 4");
            }
            if (column < 1 || column > COLUMN_COUNT)
            {
                throw invalid(index, "column must be from 1 through 4");
            }
            List<String> values = columns.get(column - 1);
            if (values.size() >= MAX_ROWS)
            {
                throw invalid(index, "column " + column + " exceeds " + MAX_ROWS + " rows");
            }
            // Everything after the first delimiter is display text, including additional delimiters.
            values.add(entry.substring(delimiter + 1));
        }
        return immutable(columns);
    }

    private static List<List<String>> immutable(List<List<String>> columns)
    {
        return columns.stream().map(List::copyOf).toList();
    }

    private static IllegalArgumentException invalid(int index, String message)
    {
        return new IllegalArgumentException("Columns.Entries[" + index + "]: " + message);
    }
}
