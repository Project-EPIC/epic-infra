package edu.colorado.cs.epic.tweetsapi.api;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class EventIndex {
    private Blob file;
    private int index;
    private int size;

    public EventIndex(Blob file, int index, int size) {
        this.file = file;
        this.index = index;
        this.size = size;

    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public String getData(int startIndex, int endIndex) throws IOException, ParseException {

        ByteArrayInputStream bais = new ByteArrayInputStream(file.getContent());
        GZIPInputStream gzis = new GZIPInputStream(bais);
        InputStreamReader reader = new InputStreamReader(gzis);
        LineNumberReader in = new LineNumberReader(reader);

        int start = Math.max(startIndex - index, 0);
        int end = Math.min(endIndex - index, size);

        String readed;
        StringBuilder data = new StringBuilder();
        while ((readed = in.readLine()) != null) {
            int lineNumber = in.getLineNumber()-1;
            if (lineNumber >= start && lineNumber < end) {
                data.append(readed);
                if (lineNumber != (end - 1))
                    data.append(",");
            } else if (lineNumber >= end) {
                break;
            }

        }

        return data.toString();
    }
}
