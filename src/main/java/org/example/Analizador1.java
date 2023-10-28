package org.example;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.UAX29URLEmailAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.en.EnglishAnalyzer;

import java.io.IOException;


public class Analizador1 {

    public static void main(String[] args) throws IOException {
        //Analyzer an = new StandardAnalyzer();
        Analyzer an = new StopAnalyzer(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        //Analyzer an = new WhitespaceAnalyzer();
        //Analyzer an = new UAX29URLEmailAnalyzer();
        String cadena = "This is a sample text of using your Own lucene-9.8.0 analyzer, brought to you by fjrodriguez@ugr.es";

        TokenStream stream = an.tokenStream(null, cadena);
        // se van imprimiendo los tokens identificados por el Analyzer
        stream.reset();
        System.out.println("***************\nToken List: \n");
        while (stream.incrementToken()) {
            System.out.println(stream.getAttribute(CharTermAttribute.class));
        }
        stream.end();
        stream.close();
    }

}
