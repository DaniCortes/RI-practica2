package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        File directory = new File("src/main/resources/books");
        File[] fileList = directory.listFiles();
        ArrayList<Document> documentList = new ArrayList<>();
        assert fileList != null;

        for (File file : fileList) {
            Document document = new Document(file);
            documentList.add(document);
        }

        for (Document document : documentList) {
            System.out.println(document);
        }



    }



}