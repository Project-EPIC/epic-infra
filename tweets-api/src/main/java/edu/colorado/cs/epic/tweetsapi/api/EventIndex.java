package edu.colorado.cs.epic.tweetsapi.api;

import com.google.cloud.storage.Blob;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class EventIndex {
    Blob file;
    int index;
    int size;

    public EventIndex(Blob file, int index) {
        this.file = file;
        this.index = index;
        this.size = 0;

    }

    public Blob getFile() {
        return file;
    }

    public void setFile(Blob file) {
        this.file = file;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getSize() {
        if (size == 0) {
            String nameSpilt = file.getName().replace(".json.gz", "");
            String[] fileDetails = nameSpilt.split("-");
            size = Integer.parseInt(fileDetails[fileDetails.length - 1]);
        }
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<JSONObject> getData(int startIndex, int endIndex) throws IOException, ParseException {

        ByteArrayInputStream bais = new ByteArrayInputStream(file.getContent());
        GZIPInputStream gzis = new GZIPInputStream(bais);
        InputStreamReader reader = new InputStreamReader(gzis);
        BufferedReader in = new BufferedReader(reader);
        String readed;
        List<JSONObject> tweetList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        while ((readed = in.readLine()) != null) {
            tweetList.add((JSONObject) parser.parse(readed));
        }
        if (startIndex < index)
            startIndex = index;
        if (endIndex >= index + size)
            endIndex = index + size - 1;
        return tweetList.subList(startIndex - index, endIndex - index);
    }
}
