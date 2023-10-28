package org.example;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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

public class Document {

    private final File file;
    private String language;
    private final BodyContentHandler textHandler;
    private final Metadata metadata;

    public Document(File file) throws TikaException, IOException, SAXException {
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

    private int countStopWords() throws IOException {
        String pathName = "src/main/java/resources/" + language;
        Path path = Paths.get(pathName);
        int lines;
        try (Stream<String> stream = Files.lines(path)) {
            lines = (int) stream.count();
        }
        return lines;
    }

    private CharArraySet getLanguageStopWords() throws IOException {
        CharArraySet stopWords = new CharArraySet(countStopWords(), true);
        try (BufferedReader br = new BufferedReader(new FileReader("src/main/java/resources/" + language))) {
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
                        return new StopAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
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

    public Map<String, Integer> tokenizeDocument(String mode) throws IOException {
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


    public String getName() {
        return file.getName();
    }

    public String getLanguage() {
        return language;
    }

    public String toString() {
        return textHandler.toString();
    }
}
