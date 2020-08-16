package edu.yu.cs.com1320.project.stage5.impl;


import edu.yu.cs.com1320.project.stage5.Document;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DocumentImpl implements Document {

    private URI uri;
    private String txt;
    private byte[] pdf;
    private int hashcode;
    private Map<String, Integer> wordCount;
    private long time;



    public DocumentImpl(URI uri, String txt, int hashcode, byte[] pdf) {
        if (uri == null){
            throw new IllegalArgumentException();
        }
        if (txt == null){
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.txt = txt;
        countWords();
        this.hashcode = hashcode;
        this.pdf = pdf;
        this.time = java.lang.System.nanoTime();

    }
    
    public DocumentImpl(URI uri, String txt, int hashcode) {
        if (uri == null){
            throw new IllegalArgumentException();
        }
        if (txt == null){
            throw new IllegalArgumentException();
        }
        this.uri = uri;
        this.txt =  txt;
        countWords();
        this.hashcode = hashcode;
        this.time = java.lang.System.nanoTime();
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream content = new PDPageContentStream(document, page);
            content.setFont(PDType1Font.COURIER, 12);
            content.beginText();
            content.showText(txt);
            content.endText();
            content.close();
            ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
            document.save(byteArray);
            document.close();
            this.pdf = byteArray.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void countWords(){
        String text = this.txt.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        this.wordCount = new HashMap<>();
        String[] words = text.split(" ");
        for(String word : words){
            word = word.trim();
            if(this.hasWord(word)){
                this.wordCount.put(word,this.wordCount.get(word)+1);
            }else{
                this.wordCount.put(word,1);
            }
        }
    }

    private boolean hasWord(String word){
        word = word.toLowerCase();
        if (this.wordCount.get(word) == null){
            return false;
        }
        return true;
    }


    @Override
    public byte[] getDocumentAsPdf() {
        return this.pdf;
    }
    @Override
    public String getDocumentAsTxt() {

        return this.txt;
    }
    @Override
    public int getDocumentTextHashCode() {

        return this.txt.hashCode();
    }
    @Override
    public URI getKey() {

        return this.uri;
    }

    @Override
    public int wordCount(String word) {
        word = word.toLowerCase();
        if(this.wordCount.get(word) != null){
            return this.wordCount.get(word);
        }
        return 0;
    }

    @Override
    public long getLastUseTime() {
        return this.time;
    }

    @Override
    public void setLastUseTime(long timeInNanoseconds) {
        this.time = timeInNanoseconds;
    }
    @Override
    public Map<String, Integer> getWordMap() {
        return wordCount;
    }
    @Override
    public void setWordMap(Map<String, Integer> wordMap) {
        this.wordCount = wordMap;
    }

    @Override
    public int compareTo(Document o) {
        if (this.time > o.getLastUseTime()){
            return 1;
        }
        if (this.time == o.getLastUseTime()){
            return 0;
        }
        return -1;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentImpl document = (DocumentImpl) o;
        return Objects.equals(txt, document.txt);
    }

    @Override
    public int hashCode() {

        return Objects.hash(txt) ;
    }
}






