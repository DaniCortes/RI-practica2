package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;

import java.io.IOException;
import java.io.StringReader;


public class Analizador2 {

    public static void tokenizeString(Analyzer an, String string) {
        try {
            TokenStream stream = an.tokenStream(null, new StringReader(string));
            OffsetAttribute offsetAtt = stream.addAttribute(OffsetAttribute.class);
            CharTermAttribute cAtt = stream.addAttribute(CharTermAttribute.class);

            stream.reset();

            while (stream.incrementToken()) {
                System.out.println(cAtt.toString() + " : [ " + offsetAtt.startOffset() + " , " + offsetAtt.endOffset() + " ] ");
            }
            stream.end();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws RuntimeException {
        Analyzer an = new WhitespaceAnalyzer();
        String text = "Ejemplo    de analizador + WhiteSpace , en lucene-9.8.0";
        tokenizeString(an, text);
    }
}
