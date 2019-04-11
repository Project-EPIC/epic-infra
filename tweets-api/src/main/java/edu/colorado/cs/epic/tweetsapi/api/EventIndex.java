package edu.colorado.cs.epic.tweetsapi.api;

import com.google.cloud.storage.Blob;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class EventIndex {

    private List<Item> index;
    private Date updateTime;

    public EventIndex(Date updateTime) {
        this.updateTime = updateTime;
        this.index = new ArrayList<>();
    }

    public List<Item> getIndex() {
        return index;
    }

    public void addItem(Item item) {
        index.add(item);
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }


    public static class Item {
        private Blob file;
        private int index;
        private int size;

        public Item(Blob file, int index, int size) {
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
                int lineNumber = in.getLineNumber() - 1;
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


}
