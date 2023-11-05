package org.example;


import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public class PythonCodeAnalyzer extends StopwordAnalyzerBase {

  public PythonCodeAnalyzer(CharArraySet stopwords) {
    super(stopwords);
  }

  protected TokenStreamComponents createComponents(String fieldName) {
    Tokenizer source = new LetterTokenizer();
    TokenStream result = new PythonTokenFilter(source);
    result = new LowerCaseFilter(result);
    result = new StopFilter(result, this.stopwords);

    return new TokenStreamComponents(source, result);
  }
}

class PythonTokenFilter extends TokenFilter {

  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  public PythonTokenFilter(TokenStream input) {
    super(input);
  }

  public boolean incrementToken() throws IOException {
    while (input.incrementToken()) {
      String token = new String(termAtt.buffer(), 0, termAtt.length());

      if (token.matches("^[A-Za-z0-9]+$")) {
        return true;
      }
    }
    return false;

  }
}

