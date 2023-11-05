package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    System.out.println("¿Vas a indexar código? (Y/n)");
    Scanner sc = new Scanner(System.in);
    char response = sc.nextLine().charAt(0);
    boolean code;
    File directory;

    if (response == 'Y' || response == 'y') {
      //Código
      directory = new File("src/main/resources/code");
      code = true;
    } else {
      //Lista de libros Gutenberg
      directory = new File("src/main/resources/books");
      code = false;
    }
    File[] fileList = directory.listFiles();
    ArrayList<Document> documentList = new ArrayList<>();
    assert fileList != null;

    for (File file : fileList) {
      Document document = new Document(file, code);
      documentList.add(document);
    }
  }
}