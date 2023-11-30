package org.example;

import java.io.File;
import java.util.ArrayList;

public class Main {

  public static void main(String[] args) throws Exception {
    File directory;
    directory = new File("src/main/resources/books");

    File[] fileList = directory.listFiles();
    ArrayList<Documento> documentList = new ArrayList<>();
    assert fileList != null;

    for (File file : fileList) {
      Documento document = new Documento(file);
      documentList.add(document);
      System.out.println(document.getLuceneDocument(document));
    }
  }
}