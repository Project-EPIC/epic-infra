package edu.colorado.cs.epic.tweetsapi.api;

import com.google.cloud.storage.Blob;
import org.eclipse.jetty.http.GZIPContentDecoder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class EventIndex {
    Blob file;
    int index;

    public EventIndex(Blob file, int index){
        this.file=file;
        this.index=index;
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

    public List<String> getData(int startIndex, int endIndex) throws IOException {

        byte[] b=new byte[Math.toIntExact(file.getSize())];
        ByteArrayInputStream bais = new ByteArrayInputStream(file.getContent());
        GZIPInputStream gzis = new GZIPInputStream(bais);
        InputStreamReader reader = new InputStreamReader(gzis);
        BufferedReader in = new BufferedReader(reader);
        String readed;
        List<String> tweetList=new ArrayList<>();
        while ((readed = in.readLine()) != null) {
            tweetList.add(readed);
        }
        return tweetList.subList(startIndex-index,endIndex-index);
    }
}
