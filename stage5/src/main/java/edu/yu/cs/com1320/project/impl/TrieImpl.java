package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value> {
    @SuppressWarnings("unchecked")
    private Node root;// root of trie
    private Set<Value> emptySet = new HashSet<>();
    private List<Value> emptyList = new ArrayList<>();
    private Value returnValue = null;
    private Set<Value> returnedSet = new HashSet<>();





    public TrieImpl(){
        this.root = null;
    }



    @SuppressWarnings("unchecked")
    private class Node<Value> {
        private Value val;
        private Set<Value> valuesSet = new HashSet<Value>();
        private Node[] links = new Node[256];
        private boolean endWord = false;


        private Node(){
        }

    }
    /**
     * add the given value at the given key
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if (key == null || key.length() == 0){
            return;
        }
        String text = key.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        if (val == null) {
            return;
        }
        else {
            this.root = put(this.root, text, val, 0);
        }
    }


    @SuppressWarnings("unchecked")
    private Node put(Node x, String key, Value val, int d) {
        //create a new node
        if (x == null) {
            x = new Node();
        }
        //we've reached the last node in the key,
        //set the value for the key and return the node
        if (d == key.length()) {
            x.endWord = true;
            if (x.valuesSet != null) {
                if (x.valuesSet.contains(val)) {
                    return x;
                }
            }
            if (x.valuesSet == null){
                x.valuesSet = new HashSet();
            }
            x.valuesSet.add(val);
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        x.links[c] = this.put(x.links[c], key, val, d + 1);
        return x;
    }
    /**
     * get all exact matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     * @param key
     * @param comparator used to sort  values
     * @return a List of matching Values, in descending order
     */
    @SuppressWarnings("unchecked")
    public List<Value> getAllSorted(String key, Comparator<Value> comparator){
        if (key == null || key.length() == 0){ return emptyList; }
        String text = key.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        Node node = this.get(this.root, text, 0);
        if (node == null) {
            return emptyList;
        }
        Set<Value> x = node.valuesSet;
        if (x == null) {
            return emptyList;
        }
        List<Value> valueList = new ArrayList<>();
        for(Value v: x){
            valueList.add(v);
        }
        if (valueList.size() == 0){
            return valueList;
        }
        return finalSimpleList(text, valueList, comparator);
    }
    private List<Value> finalSimpleList(String key, List<Value> list, Comparator<Value> comparator){
        List<Value> finalList = new ArrayList<Value>();
        for (Value v : list) {
            if (!finalList.contains(v)) {
                finalList.add(v);
            }
        }
        finalList.sort(comparator);
        return finalList;
    }

    @SuppressWarnings("unchecked")
    private Node get(Node x, String key, int d){
        //link was null - return null, indicating a miss
        if (x == null){
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length()) {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }
    /**
     * get all matches which contain a String with the given prefix, sorted in descending order.
     * For example, if the key is "Too", you would return any value that contains "Tool", "Too", "Tooth", "Toodle", etc.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @param comparator used to sort values
     * @return a List of all matching Values containing the given prefix, in descending order
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator){
        if (prefix == null || prefix.length() == 0) {
            return emptyList;
        }
        String text = prefix.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        Set<Value> set = new HashSet<Value>();
        Node x = this.get(this.root, text, 0);
        if (x == null) {
            return emptyList;
        }
        this.getPrefix(x, new StringBuilder(text), set);
        List<Value> valueList = new ArrayList<>();
        for (Value v : set) {
            valueList.add(v);
        }

        if (valueList.size() == 0) {
            return valueList;
        }
        return finalList(text, valueList, comparator);
    }
    private List<Value> finalList(String prefix, List<Value> list, Comparator<Value> comparator){

        List<Value> finalList = new ArrayList<Value>();
        for (Value v : list) {
            if (!finalList.contains(v)) {
                finalList.add(v);
            }
        }
        finalList.sort(comparator);
        return finalList;
    }
    @SuppressWarnings("unchecked")
    private void getPrefix(Node x, StringBuilder prefix, Set<Value> set){
        if (x==null){
            return;
        }
        if (!x.valuesSet.isEmpty()){
            Set<Value> s = x.valuesSet;
            for (Value v: s){
                set.add(v);
            }
        }
        for (char c =0; c<256; c++){
            if(x.links[c]!=null) {
                prefix.append(c);
                this.getPrefix(x.links[c], prefix, set);
                prefix.deleteCharAt(prefix.length() - 1);
            }
        }
    }
    /**
     * Delete all matches that contain a String with the given prefix.
     * Search is CASE INSENSITIVE.
     * @param prefix
     * @return a Set of all Values that were deleted.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<Value> deleteAllWithPrefix(String prefix) {
        if (prefix == null || prefix.length() == 0) {
            return emptySet;
        }
        String text = prefix.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        Set<Node> set = new HashSet<Node>();
        this.getPrefixDelete(this.root, text, new StringBuilder(prefix), set, 0);
        return finalSet(text, set);
    }
    @SuppressWarnings("unchecked")
    private Node getPrefixDelete(Node x, String pre, StringBuilder prefix, Set<Node> set, int d) {
        if (x==null){
            return null;
        }
        if (d == pre.length()) {
            set.add(x);
            for (char c = 0; c < 256; c++) {
                if (x.links[c] != null) {
                    prefix.append(c);
                    this.getPrefixDelete(x.links[c], pre, prefix, set, d);
                    prefix.deleteCharAt(prefix.length() - 1);
                }
            }
        }
        //we're at the node to del - set the val to null
        //continue down the trie to the target node
        else {
            char c = pre.charAt(d);
            x.links[c] = this.getPrefixDelete(x.links[c], pre, prefix, set, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.valuesSet != null) {
            if (!x.valuesSet.isEmpty())
                return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <256; c++) {
            if (x.links[c] != null) {
                return x; //not empty
            }
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private Set<Value> finalSet(String prefix, Set<Node> set){
        Set<Value> value1 = new HashSet<Value>();
        for(Node x: set) {
            Set<Value> s = x.valuesSet;
            for (Value v : s) {
                value1.add(v);
            }
            x.valuesSet.clear();
        }
        return value1;
    }
    /**
     * delete ALL exact matches for the given key
     * @param key
     * @return a Set of all Values that were deleted.
     */
    @SuppressWarnings("unchecked")

    @Override
    public Set<Value> deleteAll(String key) {
        if (key == null|| key.length() == 0){
            return emptySet;
        }
        String text = key.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        this.root = deleteAll(this.root, text, 0);
        return returnedSet;
    }
    @SuppressWarnings("unchecked")
    private Node deleteAll(Node x, String key, int d) {
        if (x == null) {
            returnedSet = emptySet;
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length()) {
            if (x.valuesSet.isEmpty()){ return null; }
            returnedSet = x.valuesSet;
            x.valuesSet = null;
        }
        //continue down the trie to the target node
        else {
            char c = key.charAt(d);
            x.links[c] = this.deleteAll(x.links[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.valuesSet != null) {
            if (!x.valuesSet.isEmpty())
                return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <256; c++) {
            if (x.links[c] != null) {
                return x; //not empty

            }
        }
        //empty - set this link to null in the parent
        return null;
    }
    /**
     * delete ONLY the given value from the given key. Leave all other values.
     * @param key
     * @param val
     * @return if there was a Value already at that key, return that previous Value. Otherwise, return null.
     */
    @Override
    public Value delete(String key, Value val) {
        if (key == null || key.length() == 0){
            return null;
        }
        if (val == null){
            return null;
        }
        String text = key.replaceAll("[^A-Za-z0-9 ]", "");
        text = text.toLowerCase();
        Value value = val;
        delete(this.root, text, val, 0);

        return returnValue;

    }
    @SuppressWarnings("unchecked")
    private Node delete(Node x, String key, Value val, int d) {
        if (x == null) { returnValue = null; return null; }
        //we're at the node to del - set the val to null
        if (d == key.length()) {
            if (x.valuesSet.isEmpty()){ return null; }
            Set<Value> returnedSet = x.valuesSet;
            List<Value> list = new LinkedList<Value>();
            for (Value v : returnedSet){
                list.add(v);
            }
            if (list.contains(val)) {
                x.valuesSet.clear();
                int index = list.indexOf(val);
                returnValue = list.get(index);
                list.remove(val);
                for (Value v : list) { x.valuesSet.add(v); }
            }
        }
        //continue down the trie to the target node
        else {
            char c = key.charAt(d);
            x.links[c] = this.delete(x.links[c], key, val, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.valuesSet != null) {
            if (!x.valuesSet.isEmpty()) { return x; }
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <256; c++) {
            if (x.links[c] != null) { return x; }
        }
        //empty - set this link to null in the parent
        return null;
    }
}