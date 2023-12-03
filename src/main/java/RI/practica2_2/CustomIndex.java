package RI.practica2_2;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class CustomIndex {

  private static final String docPath = "src/main/resources/books";
  private static final String indexPath = "src/main/resources/index";
  private static IndexWriter writer;

  private static Analyzer analyzer;

  CustomIndex() throws IOException, TikaException, SAXException {
    setIndexConfiguration();
    indexFiles();
  }

  private void setIndexConfiguration() throws IOException {
    analyzer = new EnglishAnalyzer(getLanguageStopWords());
    Similarity similarity = new BM25Similarity();
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    iwc.setSimilarity(similarity);
    iwc.setOpenMode(OpenMode.CREATE);
    FSDirectory indexDirectory = FSDirectory.open(Paths.get(indexPath));
    writer = new IndexWriter(indexDirectory, iwc);
  }

  private void indexFiles() throws TikaException, IOException, SAXException {
    File[] fileList = new File(docPath).listFiles();
    assert fileList != null;
    for (File file : fileList) {
      Document doc = createDocumentFromFile(file);
      writer.addDocument(doc);
    }
    close();
  }

  private int countStopWords() throws IOException {
    String pathName;
    pathName = "src/main/resources/stopwords/en";

    Path path = Paths.get(pathName);
    int lines;
    try (Stream<String> stream = Files.lines(path)) {
      lines = (int) stream.count();
    }
    return lines;
  }

  private CharArraySet getLanguageStopWords() throws IOException {
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

  private Document createDocumentFromFile(File file)
      throws TikaException, IOException, SAXException {
    Document doc = new Document();

    AutoDetectParser parser = new AutoDetectParser();
    BodyContentHandler textHandler = new BodyContentHandler(-1);
    Metadata metadata = new Metadata();
    ParseContext parseContext = new ParseContext();

    InputStream is = new BufferedInputStream(new FileInputStream(file));
    parser.parse(is, textHandler, metadata, parseContext);

    String title = metadata.get("dc:title");
    if (title == null) {
      title = getFilenameWithoutExtension(file);
    }

    doc.add(new TextField("title", title, Store.YES));
    doc.add(new TextField("content", textHandler.toString(), Store.YES));

    return doc;
  }

  private String getFilenameWithoutExtension(File file) {
    return FilenameUtils.removeExtension(file.getName());
  }

  public void close() {
    try {
      writer.commit();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace(System.out);
    }
  }

  public List<DocumentRank> searchIndex(String queryString) throws IOException, ParseException {
    List<DocumentRank> rankedDocuments = new ArrayList<>();
    FSDirectory dir = FSDirectory.open(Paths.get("src/main/resources/index"));
    DirectoryReader reader = DirectoryReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(reader);

    QueryParser queryParser = new QueryParser("content", analyzer);
    Query query = queryParser.parse(queryString);

    TopDocs results = searcher.search(query, 6);
    StoredFields storedFields = searcher.storedFields();

    for (ScoreDoc scoreDoc : results.scoreDocs) {
      Document doc = storedFields.document(scoreDoc.doc);
      String title = doc.get("title");
      float score = scoreDoc.score;
      rankedDocuments.add(new DocumentRank(title, score));
    }
    return rankedDocuments;
  }
}
