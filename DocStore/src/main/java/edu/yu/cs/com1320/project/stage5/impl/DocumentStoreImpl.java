package edu.yu.cs.com1320.project.stage5.impl;

import edu.yu.cs.com1320.project.BTree;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;


import edu.yu.cs.com1320.project.stage5.PersistenceManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import edu.yu.cs.com1320.project.impl.BTreeImpl;


import java.io.File;

import java.net.URISyntaxException;
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.function.Function;

public class DocumentStoreImpl<uri> implements DocumentStore {
    private BTreeImpl<URI, DocumentImpl> BTree;
    private StackImpl<Undoable> commandStack;
    private TrieImpl<URI> trie = new TrieImpl<>();
    private MinHeapImpl<fakeDoc> miniHeap = new MinHeapImpl<>();


    private String word;
    private String prefix;
    private List<String> emptyStringList = new ArrayList<>();
    private List<byte[]> emptyByteList = new ArrayList<>();
    private Set<URI> emptySetList = new HashSet<>();
    private int docCount = Integer.MAX_VALUE;
    private int docByte = Integer.MAX_VALUE;
    private int docNumber;
    private int docByteCount;
    private long undoTime;
    private Set<fakeDoc> uriSet = new HashSet<>();
    private fakeDoc get(URI uri){
        for (Iterator<fakeDoc> fd = uriSet.iterator(); fd.hasNext(); ) {
            fakeDoc f = fd.next();
            if (f.uri.equals(uri)) {
                return f;
            }
        }
        return null;
    }

    public DocumentStoreImpl() {
        this.commandStack = new StackImpl<Undoable>();
        this.BTree = new BTreeImpl<>();
        try {
            BTree.put(new URI(""), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        PersistenceManager dpm = new DocumentPersistenceManager(null);
        BTree.setPersistenceManager(dpm);
    }
    public DocumentStoreImpl(File baseDir){
        this.BTree = new BTreeImpl<>();
        try {
            BTree.put(new URI(""), null);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        this.commandStack = new StackImpl<Undoable>();
        PersistenceManager dpm =  new DocumentPersistenceManager(baseDir);
        BTree.setPersistenceManager(dpm);
    }
    protected void deserialize(URI uri){
        BTree.get(uri);
    }
    private class fakeDoc implements Comparable {
        private URI uri;
        private long time;
        private fakeDoc(URI uri){
            this.uri = uri;
            this.time = BTree.get(uri).getLastUseTime();
        }
        private void setTime(long time){
            this.time = time;
        }
        private long getTime() {
            return this.time;
        }
        private URI getUri(){
            return this.uri;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof DocumentStoreImpl.fakeDoc)) {
                return false;
            }
            fakeDoc fd = (fakeDoc) obj;
            return this.uri.equals(fd.uri);
        }

        @Override
        public int compareTo(Object o) {
            fakeDoc fd = (fakeDoc) o;
            if (this.time > fd.getTime()){
                return 1;
            }
            if (this.time == fd.getTime()){
                return 0;
            }
            return -1;
        }
    }


    private void setWord(String key){
        this.word = key.toLowerCase();
    }
    private void setPrefix(String key){
        this.prefix = key.toLowerCase();
    }
    Comparator<URI> comparator = new Comparator<URI>() {
        @Override
        public int compare(URI uri1, URI uri2) {
            DocumentImpl doc1 = BTree.get(uri1);
            DocumentImpl doc2 = BTree.get(uri2);
            int doc1Count = doc1.wordCount(word);
            int doc2Count = doc2.wordCount(word);
            if (doc1Count < doc2Count) {
                return 1;
            }
            if (doc1Count == doc2Count) {
                return 0;
            }
            return -1;
        }
    };

    Comparator<URI> comparatorPrefix = new Comparator<URI>() {
        @Override
        public int compare(URI uri1, URI uri2) {
            DocumentImpl doc1 = BTree.get(uri1);
            DocumentImpl doc2 = BTree.get(uri2);
            String txt1 = doc1.getDocumentAsTxt();
            int counter1 = getCount(txt1);
            String txt2 = doc2.getDocumentAsTxt();
            int counter2 = getCount(txt2);
            if (counter1 < counter2) {
                return 1;
            }
            if (counter1 == counter2) {
                return 0;
            }
            return -1;
        }

        private int getCount(String txt) {
            int counter = 0;
            txt = txt.replaceAll("[^A-Za-z0-9 ]", "");
            txt = txt.toLowerCase();
            String[] words = txt.split(" ");
            for (String word : words) {
                word = word.trim();
                if (word.startsWith(prefix)) {
                    counter++;
                }
            }
            return counter;
        }
    };






    private void miniHeapMethod(fakeDoc doc){
        doc.setTime(Long.MIN_VALUE);
        miniHeap.reHeapify(doc); //changed this
        miniHeap.removeMin();
    }

    private byte[] convertToByte(InputStream input) throws IOException {
        int integer;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] bytearray = new byte[1024];
        while ((integer = input.read(bytearray, 0, bytearray.length)) != -1) {
            output.write(bytearray, 0, integer);
        }
        output.flush();
        return output.toByteArray();
    }

    private int hashCode (URI uri) {
        int h = BTree.get(uri).getDocumentTextHashCode();
        return h;
    }

    private int deleteDoc (URI uri) {
        int h = hashCode(uri);
        deleteDocument(uri);
        return h;
    }
    private String PDDocument(byte[] pdf) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        PDDocument pd = PDDocument.load(pdf);
        String text = stripper.getText(pd).trim();
        pd.close();
        return text;
    }
    private void setNumber() {
        try {
            while (docNumber > docCount) {
                fakeDoc fd = miniHeap.removeMin();
                URI uri = fd.getUri();
                Document doc = BTree.get(uri); //changed this
                BTree.moveToDisk(uri);
                uriSet.remove(fd);
                docNumber--;
                docByteCount = docByteCount - (doc.getDocumentAsPdf().length + doc.getDocumentAsTxt().getBytes().length);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void setByte() {
        try {
            while (docByteCount > docByte) {
                fakeDoc fd = miniHeap.removeMin();
                URI uri = fd.getUri();
                Document doc = BTree.get(uri); //changed this
                BTree.moveToDisk(uri);
                uriSet.remove(fd);
                docNumber--;
                docByteCount = docByteCount - (doc.getDocumentAsPdf().length + doc.getDocumentAsTxt().getBytes().length);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) {
        if (uri == null || format == null){ throw new IllegalArgumentException(); }
        if (input == null && BTree.get(uri) == null){  deleteDocument(uri); return 0; }
        if (input == null && BTree.get(uri) != null){ return deleteDoc(uri); }
        docNumber++;
        if (BTree.get(uri) != null && (uriSet.contains(get(uri)))){
            docNumber--;
        }
        setNumber();
        switch (format) {
            case PDF:
                return PDFFile(input, uri);
            case TXT:
                return textFile(input, uri);
        }
        return 0;
    }
    private int PDFFile(InputStream input, URI uri) {
        try {
            long time = System.nanoTime();
            byte[] pdf = convertToByte(input);
            String text = PDDocument(pdf);
            DocumentImpl docCheck = BTree.get(uri);
            DocumentImpl document = new DocumentImpl(uri, text, text.hashCode(), pdf);
            docByteCount = document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length + docByteCount;
            if (BTree.get(uri) != null && BTree.get(uri).getDocumentTextHashCode() == text.hashCode()) {
                stack(uri, document, docCheck);
                BTree.get(uri).setLastUseTime(time);
                if (uriSet.contains(get(uri))){
                    get(uri).setTime(time);
                    miniHeap.reHeapify(get(uri)); //chnaged this
                    docByteCount -= (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
                }
                else{
                    BTree.get(uri);//NNED TO MOVE TO RAM
                    fakeDoc fd = new fakeDoc(uri);
                    uriSet.add(fd);
                    miniHeap.insert(fd);
                    miniHeap.reHeapify(fd);
//                    docByteCount += (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
                    setNumber();
                    setByte();
                }
                return text.hashCode();
            }
            document.setLastUseTime(time);
            BTree.put(uri, document);
            if (docCheck != null) {
                deleteFromT(docCheck);
                minHeapMethod(docCheck);
                fakeDoc fd = new fakeDoc(uri);
                uriSet.add(fd);
                miniHeap.insert(fd);
                miniHeap.reHeapify(fd);
                setByte();
            }
            else{
                fakeDoc fd = new fakeDoc(uri);
                uriSet.add(fd);
                miniHeap.insert(fd);
                miniHeap.reHeapify(fd);
                setByte();
            }

            add(uri, document, docCheck);//add to trie, stack and miniheap
            if (docCheck != null) { return (docCheck.getDocumentTextHashCode()); }
            return 0;
        }catch (IOException e){ e.printStackTrace(); }
        return 0;
    }
    private void minHeapMethod(DocumentImpl doc){
        URI uri = doc.getKey();
        if (uriSet.contains(get(uri))){
             get(uri).setTime(Long.MIN_VALUE);
             miniHeap.reHeapify(get(uri));
             miniHeap.removeMin();
             uriSet.remove(get(uri));
             docByteCount -= (doc.getDocumentAsPdf().length + doc.getDocumentAsTxt().getBytes().length);
        }
    }
    private int textFile(InputStream input, URI uri) {
        try {
            long time = System.nanoTime();
            byte[] pdf = convertToByte(input);
            String text = new String(pdf);
            DocumentImpl docCheck = BTree.get(uri);
            DocumentImpl document = new DocumentImpl(uri, text, text.hashCode());
            docByteCount = document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length + docByteCount;
            if (BTree.get(uri) != null && BTree.get(uri).getDocumentTextHashCode() == text.hashCode()) {
                stack(uri, document, docCheck);
                BTree.get(uri).setLastUseTime(time);
                if (uriSet.contains(get(uri))){
                    get(uri).setTime(time);
                    miniHeap.reHeapify(get(uri)); //chnaged this
                    docByteCount -= (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
                }
                else{
                    //NEED TO  OVE TO RAM
                    BTree.get(uri);
                    fakeDoc fd = new fakeDoc(uri);
                    uriSet.add(fd);
                    miniHeap.insert(fd);
                    miniHeap.reHeapify(fd);
//                    docByteCount += (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
                    setNumber();
                    setByte();
                }
                return text.hashCode();
            }
            document.setLastUseTime(time);
            BTree.put(uri, document);
            if (docCheck != null) {
                deleteFromT(docCheck);
                minHeapMethod(docCheck);
                fakeDoc fd = new fakeDoc(uri);
                uriSet.add(fd);
                miniHeap.insert(fd);
                miniHeap.reHeapify(fd);
                setByte();
            }
            else{
                fakeDoc fd = new fakeDoc(uri);
                uriSet.add(fd);
                miniHeap.insert(fd);
                miniHeap.reHeapify(fd);
                setByte();
            }
            add(uri, document, docCheck);//add to trie, stack and miniheap
            if (docCheck != null) { return (docCheck.getDocumentTextHashCode()); }
            return 0;
        }catch (IOException e){ e.printStackTrace(); }
        return 0;
    }
    private void deleteFromT(Document document){
        String doc = document.getDocumentAsTxt();
        doc = doc.replaceAll("[^A-Za-z0-9 ]", "");
        doc = doc.toLowerCase();
        String[] words = doc.split(" ");
        for (String x : words){
            trie.delete(x, document.getKey());//changed this
        }
    }
    private void add(URI uri, DocumentImpl document, DocumentImpl docCheck){
        addToTrie(document);
        stack(uri, document, docCheck);
    }
    private void addToTrie(DocumentImpl document){
        String doc = document.getDocumentAsTxt();
        doc = doc.replaceAll("[^A-Za-z0-9 ]", "");
        doc = doc.toLowerCase();
        String[] words = doc.split(" ");
        for (String x : words){
            trie.put(x, document.getKey()); //changed this
        }
    }
    @SuppressWarnings("unchecked")
    private void stack(URI uri, DocumentImpl document, DocumentImpl oldDocument){
        Function<URI, Boolean> undoPut =
                x -> {
                        BTree.put(x, oldDocument);
                        if (!(uriSet.contains(get(x)))){
                            BTree.get(x);//need to deserialize doc
                        }
                        deleteFromT(document);
                        if (uriSet.contains(get(uri))) {
                            miniHeapMethod(get(uri));
                            uriSet.remove(get(uri));
                            docByteCount = docByteCount - (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
                            docNumber--;
                            }
                        if (oldDocument != null) {
                            addToTrie(oldDocument);
                            fakeDoc fd = new fakeDoc(oldDocument.getKey());
                            long time = System.nanoTime();
                            oldDocument.setLastUseTime(time);
                            fd.setTime(time);
                            uriSet.add(fd);
                            miniHeap.insert(fd);
                            miniHeap.reHeapify(fd);
                            docByteCount = docByteCount + (oldDocument.getDocumentAsPdf().length + oldDocument.getDocumentAsTxt().getBytes().length);
                            docNumber++;
                            setByte();
                            setNumber();
                        }
                        return true;
                };
        GenericCommand com1 = new GenericCommand(uri,undoPut);
        commandStack.push(com1);
    }
    @Override
    public byte[] getDocumentAsPdf(URI uri) {
        if (uri == null){
            throw new IllegalArgumentException();
        }
        if (BTree.get(uri) == null) {
            return null;
        }
        long time = System.nanoTime();
        if (uriSet.contains(get(uri))) {
            DocumentImpl doc = BTree.get(uri);
            doc.setLastUseTime(time);
            get(uri).setTime(time);
            miniHeap.reHeapify(get(uri)); //changed this
            return doc.getDocumentAsPdf();
        } else{
            //MOVE TO RAM_ NEED TO DO
            DocumentImpl doc = BTree.get(uri);
            doc.setLastUseTime(time);
            fakeDoc fd = new fakeDoc(uri);
            uriSet.add(fd);
            miniHeap.insert(fd);
            miniHeap.reHeapify(fd);
            docNumber++;
            docByteCount += doc.getDocumentAsPdf().length + doc.getDocumentAsTxt().getBytes().length;
            setByte();
            setNumber();
            return doc.getDocumentAsPdf();
        }
    }
    @Override
    public String getDocumentAsTxt(URI uri) {
        if (uri == null){
            throw new IllegalArgumentException();
        }
        if (BTree.get(uri) == null) {
            return null;
        }
        long time = System.nanoTime();
        if (uriSet.contains(get(uri))) {
            DocumentImpl doc = BTree.get(uri);
            doc.setLastUseTime(time);
            get(uri).setTime(time);
            miniHeap.reHeapify(get(uri)); //changed this
            return doc.getDocumentAsTxt();
        } else{
            //MOVE TO RAM_ NEED TO DO
            DocumentImpl doc = BTree.get(uri);
            doc.setLastUseTime(time);
            fakeDoc fd = new fakeDoc(uri);
            uriSet.add(fd);
            miniHeap.insert(fd);
            miniHeap.reHeapify(fd);
            docNumber++;
            docByteCount += doc.getDocumentAsPdf().length + doc.getDocumentAsTxt().getBytes().length;
            setByte();
            setNumber();
            return doc.getDocumentAsTxt();
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public boolean deleteDocument(URI uri) {
        if (uri == null){ throw new IllegalArgumentException(); }
        if (BTree.get(uri) == null) {
            Function<URI, Boolean> undoDelete = x -> { return true; };
            GenericCommand command = new GenericCommand(uri, undoDelete);
            commandStack.push(command);
            return false;
        }
        DocumentImpl document = BTree.get(uri);
        Function<URI, Boolean> undoDelete =
                x -> {
            undoDelete(document,x);
            return true;
                };
        GenericCommand command = new GenericCommand(uri, undoDelete);
        commandStack.push(command);
        if (uriSet.contains(get(uri))){
            BTree.put(uri, null); //delete from BTree
            deleteDocFromTrie(document);//delete doc from trie
            document.setLastUseTime(Long.MIN_VALUE);
            get(uri).setTime(Long.MIN_VALUE);
            miniHeap.reHeapify(get(uri)); //changed this
            miniHeap.removeMin();
            uriSet.remove(get(uri));
            docByteCount = docByteCount - (document.getDocumentAsPdf().length+document.getDocumentAsTxt().getBytes().length);
            docNumber--;
        }else{
            BTree.get(uri);
            BTree.put(uri, null);//delete from BTree
            deleteDocFromTrie(document);//delete doc from trie
        }
        return true;
    }

    private boolean undoDelete(DocumentImpl document, URI x){
        docNumber++;
        setNumber();
        BTree.put(x, document);
        document.setLastUseTime(this.undoTime);
        fakeDoc fd = new fakeDoc(x);
        uriSet.add(fd);
        addToTrie(document);
        miniHeap.insert(fd); //changed this
        miniHeap.reHeapify(fd); //changed this
        docByteCount = docByteCount + (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
        setByte();
        return true;
    }

    private void deleteDocFromTrie(Document document){
        String doc = document.getDocumentAsTxt();
        doc = doc.replaceAll("[^A-Za-z0-9 ]", "");
        doc = doc.toLowerCase();
        String[] words = doc.split(" ");
        for (String x : words){
            trie.delete(x, document.getKey()); //changed this
        }

    }
    @SuppressWarnings("unchecked")
    @Override
    public void undo() throws IllegalStateException {
        if (commandStack.size() == 0){
            throw new IllegalStateException();
        }
        undoTime = System.nanoTime();
        if(commandStack.peek() instanceof GenericCommand){
            commandStack.pop().undo();
        }
        if (commandStack.peek() instanceof CommandSet){
            CommandSet commandSet = (CommandSet)commandStack.pop();
            commandSet.undoAll();
        }
    }
    @SuppressWarnings("unchecked")
    @Override
    public void undo(URI uri) throws IllegalStateException {
        if (commandStack.size() == 0){ throw new IllegalStateException(); }
        StackImpl<Undoable> stack = new StackImpl<Undoable>();
        undoTime = System.nanoTime();
        while(commandStack.size() != 0) {
            if (commandStack.peek() instanceof GenericCommand) {
                GenericCommand command = (GenericCommand) commandStack.peek();
                if (command.getTarget() != uri) {
                    stack.push(commandStack.pop()); }
                else{ break; }
            }
            else {
                if(commandStack.peek() instanceof CommandSet) {
                    CommandSet commandSet = (CommandSet) commandStack.peek();
                    if (commandSet.containsTarget(uri)){ break; }
                    else{ stack.push(commandStack.pop()); }
                }
            }
        }
        if (commandStack.size() == 0) { throw new IllegalStateException(); }
        if(commandStack.peek() instanceof CommandSet) {
            CommandSet commandSet = (CommandSet) commandStack.peek();
            commandSet.undo(uri);
            if (commandSet.size() == 0){
                commandStack.pop();
            }
        }
        else { commandStack.pop().undo(); }
        while(stack.peek()!= null){
            commandStack.push(stack.pop()); }
    }
    /**
     * @return the Document object stored at that URI, or null if there is no such
    Document */
    protected Document getDocument(URI uri){
        if (uriSet.contains(get(uri))){
            return BTree.get(uri);
        }
        return null;
    }
    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */

    @Override
    public List<String> search(String keyword) { //changed this method
        if(keyword == null) {
            return emptyStringList;
        }
        setWord(keyword);
        List<URI> docList = trie.getAllSorted(keyword, comparator);

        List<String> stringList = new ArrayList<>();
        if (docList.isEmpty()){
            return stringList;
        }
        long time = System.nanoTime();
        for (URI uri : docList) {
            searchRam(stringList,uri,time);
        }
        return stringList;
    }

    private void searchRam(List<String> stringList, URI uri, long time){
        if (uriSet.contains(get(uri))) {
            Document x = BTree.get(uri);
            String doc = x.getDocumentAsTxt();
            x.setLastUseTime(time);
            get(uri).setTime(time);
            miniHeap.reHeapify(get(uri));
            stringList.add(doc);
        } else {
            // NEED TO MOVE TO RAM
            Document x = BTree.get(uri);
            String doc = x.getDocumentAsTxt();
            x.setLastUseTime(time);
            fakeDoc fd = new fakeDoc(uri);
            uriSet.add(fd);
            miniHeap.insert(fd);
            miniHeap.reHeapify(fd);
            docNumber++;
            docByteCount += (x.getDocumentAsPdf().length+x.getDocumentAsTxt().getBytes().length);
            setByte();
            setNumber();
            stringList.add(doc);
        }
    }
    /**
     * same logic as search, but returns the docs as PDFs instead of as Strings
     */
    @Override
    public List<byte[]> searchPDFs(String keyword) { //changed this
        if(keyword == null) {
            return emptyByteList;
        }
        setWord(keyword);
        List<URI> docList = trie.getAllSorted(keyword, comparator);

        List<byte[]> byteList = new ArrayList<>();
        if (docList.isEmpty()){
            return byteList;
        }
        long time = System.nanoTime();
        for (URI uri :docList){
            searchPDF(byteList,uri,time);
        }
        return byteList;
    }

    private void searchPDF(List<byte[]> byteList, URI uri, long time){
        if (uriSet.contains(get(uri))) {
            Document x = BTree.get(uri);
            byte[] doc = x.getDocumentAsPdf();
            x.setLastUseTime(time);
            get(uri).setTime(time);
            miniHeap.reHeapify(get(uri));
            byteList.add(doc);
        } else {
            // NEED TO MOVE TO RAM
            Document x = BTree.get(uri);
            byte[] doc = x.getDocumentAsPdf();
            x.setLastUseTime(time);
            fakeDoc fd = new fakeDoc(uri);
            uriSet.add(fd);
            miniHeap.insert(fd);
            miniHeap.reHeapify(fd);
            docNumber++;
            docByteCount += (x.getDocumentAsPdf().length+x.getDocumentAsTxt().getBytes().length);
            setByte();
            setNumber();
            byteList.add(doc);
        }
    }

    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<String> searchByPrefix(String keywordPrefix) { //changed this
        if(keywordPrefix == null) {
            return emptyStringList;
        }
        setPrefix(keywordPrefix);
        List<URI> docList = trie.getAllWithPrefixSorted(keywordPrefix, comparatorPrefix);

        List<String> stringList = new ArrayList<>();
        if (docList.isEmpty()){
            return stringList;
        }
        long time = System.nanoTime();
        for (URI uri :docList){
            searchRam(stringList,uri,time);
        }
        return stringList;
    }

    /**
     * same logic as searchByPrefix, but returns the docs as PDFs instead of as Strings
     */
    @Override
    public List<byte[]> searchPDFsByPrefix(String keywordPrefix) { //changed this
        if(keywordPrefix == null) {
            return emptyByteList;
        }
        setPrefix(keywordPrefix);
        List<URI> docList = trie.getAllWithPrefixSorted(keywordPrefix, comparatorPrefix);
        List<byte[]> byteList = new ArrayList<>();
        if (docList.isEmpty()){
            return byteList;
        }
        long time = System.nanoTime();
        for (URI uri :docList){
            searchPDF(byteList,uri,time);
        }
        return byteList;
    }
    /**
     * delete ALL exact matches for the given key
     * @param key
     * @return a Set of URIs of the documents that were deleted.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<URI> deleteAll(String keyword) { //changed this method
        if (keyword == null) { return emptySetList; }
        CommandSet<GenericCommand> commandSet = new CommandSet<GenericCommand>();
        Set<URI> set = trie.deleteAll(keyword);
        Set<URI> URISet = new HashSet<>();
        if (set.isEmpty()) {
            CommandSet command = new CommandSet();
            commandStack.push(command);
            return URISet;
        }
        for (URI uri : set) { //changed this
            DocumentImpl document = BTree.get(uri);
            if (uriSet.contains(get(uri))) {
                Function<URI, Boolean> undoDelete =
                        x -> { undoDel(document, uri);
                            return true;
                        };
                GenericCommand command = new GenericCommand(uri, undoDelete);
                commandSet.addCommand(command);
                delete(URISet, document, uri, keyword);
            } else {
                DocumentImpl doc = BTree.get(uri);
                Function<URI, Boolean> undoDelete =
                        x -> { undoDel(doc,uri);
                            return true;
                        };
                GenericCommand command = new GenericCommand(uri, undoDelete);
                commandSet.addCommand(command);
                BTree.put(uri, null);//delete from BTree
                URISet.add(uri);
                deleteFromTrie(document, keyword);//delete doc from trie
            }
        }
        commandStack.push(commandSet);
        return URISet;
    }
    private void delete(Set<URI> URISet, DocumentImpl document, URI uri,String keyword){
        URISet.add(uri);
        BTree.put(uri, null); //delete from BTree
        deleteFromTrie(document, keyword);//delete doc from trie
        document.setLastUseTime(Long.MIN_VALUE);
        get(uri).setTime(Long.MIN_VALUE);
        miniHeap.reHeapify(get(uri)); //changed this
        miniHeap.removeMin();
        uriSet.remove(get(uri));
        docByteCount = docByteCount - (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
        docNumber--;
    }
    private boolean undoDel(DocumentImpl document, URI x){
        docNumber++;
        setNumber();
        BTree.put(x, document);
        document.setLastUseTime(this.undoTime);
        fakeDoc fd = new fakeDoc(x);
        uriSet.add(fd);
        addToTrie(document);
        miniHeap.insert(fd); //changed this
        miniHeap.reHeapify(fd); //changed this
        docByteCount = docByteCount + (document.getDocumentAsPdf().length + document.getDocumentAsTxt().getBytes().length);
        setByte();
        return true;
    }

    private void deleteFromTrie(Document document, String word){
        String doc = document.getDocumentAsTxt();
        doc = doc.replaceAll("[^A-Za-z0-9 ]", "");
        doc = doc.toLowerCase();
        String[] words = doc.split(" ");
        for (String x : words){
            if (x.equals(word)){
                continue;
            }
            trie.delete(x, document.getKey());
        }
    }
    /**
     * Delete all matches that contain a String with the given prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of URIs of the documents that were deleted.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) { //changed this method
        if (keywordPrefix == null) { return emptySetList; }
        CommandSet<GenericCommand> commandSet = new CommandSet<GenericCommand>();
        Set<URI> set = trie.deleteAllWithPrefix(keywordPrefix);
        Set<URI> URISet = new HashSet<>();
        if (set.isEmpty()) {
            CommandSet command = new CommandSet();
            commandStack.push(command);
            return URISet;
        }
        for (URI uri : set) { //changed this
            DocumentImpl document = BTree.get(uri);
            if (uriSet.contains(get(uri))) {
                Function<URI, Boolean> undoDelete =
                        x -> { undoDel(document, uri);
                            return true;
                        };
                GenericCommand command = new GenericCommand(uri, undoDelete);
                commandSet.addCommand(command);
                delete(URISet, document, uri, keywordPrefix);
            } else {
                //NEED TO MOVE TO RAM and delete file
                DocumentImpl doc = BTree.get(uri);
                Function<URI, Boolean> undoDelete =
                        x -> { undoDel(doc, uri);
                            return true;
                        };
                GenericCommand command = new GenericCommand(uri, undoDelete);
                commandSet.addCommand(command);
                BTree.put(uri, null);//delete from BTree
                URISet.add(uri);
                deleteFromTrie(document, keywordPrefix);//delete doc from trie
            }
        }
        commandStack.push(commandSet);
        return URISet;
    }

    @Override
    public void setMaxDocumentCount(int limit) {
        if (limit <0){
            throw new IllegalArgumentException();
        }
        this.docCount = limit;
        setNumber();
        setByte();
    }

    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit <0){
            throw new IllegalArgumentException();
        }
        this.docByte = limit;
        setByte();
        setNumber();
    }
}