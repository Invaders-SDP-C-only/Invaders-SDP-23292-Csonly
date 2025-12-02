package engine;

import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinimalFormatterTest {

    @Test
    void format() {
        MinimalFormatter formatter = new MinimalFormatter();
        long millis = System.currentTimeMillis();
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setMillis(millis);

        DateFormat format = new SimpleDateFormat("h:mm:ss");
        String expectedDate = format.format(new Date(millis));
        String expected = "[" + Level.INFO.toString() + "|" + expectedDate + "]: Test message " + System.getProperty("line.separator");

        assertEquals(expected, formatter.format(record));
    }
    
    @Test
    void format_differentLevelAndMessage() {
        MinimalFormatter formatter = new MinimalFormatter();
        long millis = System.currentTimeMillis();
        LogRecord record = new LogRecord(Level.WARNING, "Another message");
        record.setMillis(millis);

        DateFormat format = new SimpleDateFormat("h:mm:ss");
        String expectedDate = format.format(new Date(millis));
        String expected = "[" + Level.WARNING.toString() + "|" + expectedDate + "]: Another message " + System.getProperty("line.separator");

        assertEquals(expected, formatter.format(record));
    }
}
