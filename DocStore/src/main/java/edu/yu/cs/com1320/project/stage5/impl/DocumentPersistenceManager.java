package edu.yu.cs.com1320.project.stage5.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.PersistenceManager;


import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;


/**
 * created by the document store and given to the BTree via a call to BTree.setPersistenceManager
 */
public class DocumentPersistenceManager implements PersistenceManager<URI, Document> {

    private com.google.gson.JsonSerializer<Document> serializer = (document, type, jsonSerializationContext) -> {
        JsonObject element = new JsonObject();
        Gson gson = new Gson();
        element.addProperty("txt", document.getDocumentAsTxt());
        element.addProperty("uri", gson.toJson(document.getKey()));
        element.addProperty("hashcode", document.getDocumentTextHashCode());
        element.addProperty("wordCount", gson.toJson((document.getWordMap())));
        return element;
    };

    private com.google.gson.JsonDeserializer<Document> deserializer = (jsonElement, type, jsonDeserializationContext) -> {
        try{
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();


            String text = jsonElement.getAsJsonObject().get("txt").getAsString();
            String map = jsonObject.get("wordCount").getAsString();

            URI uri = new URI(gson.fromJson(jsonElement.getAsJsonObject().get("uri").getAsString(), String.class));
            Type t = new TypeToken<HashMap<String, Integer>>() {}.getType();
            HashMap<String, Integer> textMap = gson.fromJson(map, t);

            int hashCode = jsonElement.getAsJsonObject().get("hashcode").getAsInt();
            DocumentImpl doc = new DocumentImpl(uri, text, hashCode);
            doc.setWordMap(textMap);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };
    private File baseDir;

    public DocumentPersistenceManager(File baseDir){

        if (baseDir == null) {
            this.baseDir = new File(System.getProperty("user.dir"));
        }

        else {
            this.baseDir = baseDir;
        }
    }

    @Override
    public void serialize(URI uri, Document val) throws IOException {
            String fil = baseDir.getPath();
            String uriString = uri.getHost() + uri.getPath();
            String string = uriString.replace("/", File.separator);
            String finalURIString = File.separator + string + ".json";
            String filePath = fil + finalURIString;


            Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, serializer).setPrettyPrinting().create();
            File finalFile = new File(filePath);
            Path p = Paths.get(filePath);
            Files.createDirectories(p.getParent());
            if(!Files.exists(p)) {
                Files.createFile(p);
            }
            FileWriter writer = new FileWriter(filePath, false);
            JsonWriter jw = new JsonWriter(writer);
            gson.toJson(val, Document.class, jw);
            writer.close();
    }

    @Override
    public Document deserialize(URI uri) throws IOException {
        try {
            String file = baseDir.getPath();
            String uriString = uri.getHost() + uri.getPath();
            String string = uriString.replace("/", File.separator);
            String finalURIString = File.separator + string + ".json";
            String filePath = file + finalURIString;

            File finalFile = new File(filePath);
            boolean test = finalFile.exists();
            if (!test){
                return null;
            }

            Gson gson = new GsonBuilder().registerTypeAdapter(Document.class, deserializer).create();

            FileReader r= new FileReader(filePath);
            Object object = JsonParser.parseReader(r);
            JsonObject jsonObject = (JsonObject) object;
            String newText = jsonObject.toString();
            Document doc = gson.fromJson(newText, Document.class);
            r.close();

            this.deletePath(filePath);

            return doc;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deletePath(String fp){
        while (!fp.equals(this.baseDir.toString())){
            File file = new File(fp);
            String parent = file.getParent();
            file.delete();
            fp = parent;
        }
    }
}