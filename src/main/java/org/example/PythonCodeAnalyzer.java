package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

public class PythonCodeAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String s) {
        try (final Tokenizer tokenizer = new LetterTokenizer()) {
            TokenStream tokenStream = new PythonSymbolFilter(tokenizer);
            return new TokenStreamComponents(tokenizer, tokenStream);
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
            return null;
        }
    }
}

class PythonSymbolFilter extends TokenStream {
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final TokenStream input;

    public PythonSymbolFilter(TokenStream input) {
        this.input = input;
    }

    public boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            char[] buffer = termAtt.buffer();
            int length = termAtt.length();
            String token = new String(buffer, 0, length);

            // Remove symbols that are not letters or numbers
            if (token.matches("^[A-Za-z0-9]+$")) {
                return true;
            }
        }
        return false;

    }
}

