package org.example;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Documento {

  private final File file;
  private String language;
  private final BodyContentHandler textHandler;
  private final Metadata metadata;
  private final List<Entry<String, Integer>> languageTokens;
  private final List<Entry<String, Integer>> standardTokens;
  private final List<Entry<String, Integer>> wildcardTokens;


  public Documento(File file) throws TikaException, IOException, SAXException {
    this.file = file;
    AutoDetectParser parser = new AutoDetectParser();
    textHandler = new BodyContentHandler(-1);
    metadata = new Metadata();
    ParseContext parseContext = new ParseContext();

    //Esto lo he hecho porque necesito que el FileInputStream tenga soporte
    // de mark/reset ya que si el InputStream que le paso al charsetDetector
    // no tiene este soporte, da error.
    InputStream is = new BufferedInputStream(new FileInputStream(file));
    parser.parse(is, textHandler, metadata, parseContext);
    retrieveLanguage();

    new File("src/main/resources/tokens").mkdir();

    languageTokens = getSortedTokens("language");
    standardTokens = getSortedTokens("standard");
    wildcardTokens = getSortedTokens("whitespace");
    writeTokensInFile();
  }

  private String identifyLanguage() {
    LanguageDetector identifier = new OptimaizeLangDetector().loadModels();
    LanguageResult language = identifier.detect(textHandler.toString());
    return language.getLanguage();
  }

  private void retrieveLanguage() {
    language = metadata.get("dc.language");

    if (language == null) {
      language = metadata.get("Content-Language");
      if (language == null) {
        language = identifyLanguage();
      }
    }
  }

  private String getFilenameWithoutExtension() {
    return FilenameUtils.removeExtension(file.getName());
  }

  private int countStopWords(String mode) throws IOException {
    String pathName;
    if (mode.equals("language")) {
      pathName = "src/main/resources/stopwords/" + language;
    } else {
      pathName = "src/main/resources/stopwords/py";
    }
    Path path = Paths.get(pathName);
    int lines;
    try (Stream<String> stream = Files.lines(path)) {
      lines = (int) stream.count();
    }
    return lines;
  }

  private CharArraySet getLanguageStopWords() throws IOException {
    CharArraySet stopWords = new CharArraySet(countStopWords("language"), true);
    try (BufferedReader br = new BufferedReader(
        new FileReader("src/main/resources/stopwords/" + language))) {
      String line;
      while ((line = br.readLine()) != null) {
        stopWords.add(line);
      }
    }
    return stopWords;
  }

  private Analyzer selectAnalyzer(String mode) throws IOException {
    switch (mode) {
      case "language":
        switch (language) {
          case "en":
            return new EnglishAnalyzer(getLanguageStopWords());
          case "el":
            return new GreekAnalyzer(getLanguageStopWords());
          case "fr":
            return new FrenchAnalyzer(getLanguageStopWords());
          case "it":
            return new ItalianAnalyzer(getLanguageStopWords());
        }
      case "standard":
        return new StandardAnalyzer();
      default:
        return new WhitespaceAnalyzer();
    }
  }

  private Map<String, Integer> tokenizeDocument(String mode) throws IOException {
    Analyzer an = selectAnalyzer(mode);
    Map<String, Integer> tokens = new HashMap<>();
    TokenStream stream = an.tokenStream(null, textHandler.toString());

    stream.reset();
    while (stream.incrementToken()) {
      String token = stream.getAttribute(CharTermAttribute.class).toString();
      if (!tokens.containsKey(token)) {
        tokens.put(token, 1);
      } else {
        tokens.replace(token, tokens.get(token) + 1);
      }
    }
    stream.end();
    stream.close();

    return tokens;
  }

  public String getDocumentTokens() throws IOException {
    Analyzer an = selectAnalyzer("language");
    StringBuilder tokens = new StringBuilder();
    TokenStream stream = an.tokenStream(null, textHandler.toString());

    stream.reset();
    while (stream.incrementToken()) {
      String token = stream.getAttribute(CharTermAttribute.class).toString();
      tokens.append(token).append(" ");
    }
    stream.end();
    stream.close();

    return tokens.toString();
  }

  private String createFile(String type) throws IOException {
    String filename = getFilenameWithoutExtension() + "-" + type;
    String filePath = "src/main/resources/tokens/" + filename + ".dat";
    File file = new File(filePath);

    if (file.createNewFile()) {
      System.out.println("File " + filename + " created.");
    } else {
      System.out.println("File " + filename + " already exists. "
          + "Proceeding to delete its contents.");
      new FileWriter(filePath, false).close();
      System.out.println("File contents deleted.");
    }

    return filePath;
  }

  private List<Entry<String, Integer>> getSortedTokens(String type) throws IOException {
    Map<String, Integer> tokens = tokenizeDocument(type);

    List<Entry<String, Integer>> sortedTokens = new ArrayList<>(tokens.entrySet());
    sortedTokens.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    return sortedTokens;
  }

  public void writeTokensInFile() throws IOException {
    writeTokensInFile("language");
    writeTokensInFile("standard");
    writeTokensInFile("whitespace");
  }

  private void writeTokensInFile(String type) throws IOException {
    List<Entry<String, Integer>> sortedTokens = switch (type) {
      case "language" -> languageTokens;
      case "standard" -> standardTokens;
      default -> wildcardTokens;
    };

    String filePath = createFile(type);

    try (FileWriter writer = new FileWriter(filePath)) {
      for (Map.Entry<String, Integer> token : sortedTokens) {
        writer.write(token.getKey() + " " + token.getValue()
            + String.format("%n"));
      }
      System.out.println(type + " tokens saved." + String.format("%n"));
    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  public String getName() {
    return file.getName();
  }

  public String getLanguage() {
    return language;
  }

  public String toString() {
    return textHandler.toString();
  }

  public Document getLuceneDocument(Documento documento) throws IOException {
    Document doc = new Document();
    doc.add(new TextField("content", getDocumentTokens(), Field.Store.YES));
    System.out.println(doc.get("content"));
    return doc;
  }
}
