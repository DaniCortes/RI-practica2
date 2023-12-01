package org.example;

import com.healthmarketscience.jackcess.Index;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

public class Main {

  public static void main(String[] args) throws Exception {
    File directory;
    FSDirectory dir = FSDirectory.open(Paths.get("src/main/resources/index"));
    directory = new File("src/main/resources/books");

    File[] fileList = directory.listFiles();
    ArrayList<Documento> documentList = new ArrayList<>();
    assert fileList != null;
    Analyzer analyzer = new WhitespaceAnalyzer();
    IndexWriterConfig config = new IndexWriterConfig(analyzer);
    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
    IndexWriter writer = new IndexWriter(dir, config);

    for (File file : fileList) {
      Documento document = new Documento(file);
      documentList.add(document);
      writer.addDocument(document.getLuceneDocument());
    }
  }

  private static int countStopWords() throws IOException {
    Path path = Paths.get("src/main/resources/stopwords/en");
    int lines;
    try (Stream<String> stream = Files.lines(path)) {
      lines = (int) stream.count();
    }
    return lines;
  }
  public static CharArraySet getLanguageStopWords() throws IOException {
    CharArraySet stopWords = new CharArraySet(countStopWords(), true);
    try (BufferedReader br = new BufferedReader(
        new FileReader("src/main/resources/stopwords/en"))) {
      String line;
      while ((line = br.readLine()) != null) {
        stopWords.add(line);
      }
    }
    return stopWords;
  }
}

