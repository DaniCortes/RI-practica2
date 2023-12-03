package RI.practica2_2;

import java.util.List;
import java.util.Scanner;


public class Main {


  //Únicamente para uso por terminal
  //Para iniciar la interfaz gráfica, iniciar desde SearchGUI
  public static void main(String[] args) throws Exception {
    System.out.println("Query: ");
    Scanner in = new Scanner(System.in);
    String query = in.nextLine();

    CustomIndex index = new CustomIndex();
    List<DocumentRank> rankedDocuments = index.searchIndex(query);

    for (DocumentRank document : rankedDocuments) {
      System.out.println(document);
    }

  }
}