package org.example;

import java.io.File;
import java.util.ArrayList;

public class Main {

  public static void main(String[] args) throws Exception {
    System.out.println(System.getProperty("user.dir"));
    File directory = new File("src/main/resources/books");
    File[] fileList = directory.listFiles();
    ArrayList<Document> documentList = new ArrayList<>();
    assert fileList != null;

    for (File file : fileList) {
      Document document = new Document(file);
      documentList.add(document);
    }
  }
}