package me.redstoner2019.configuration;

import me.redstoner2019.serverhandling.Util;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    public String path;
    public Configuration(String path){
        this.path = path;
    }
    public List<Game> games = new ArrayList<>();
    public void addGame(Game game){
        games.add(game);
    }
    public boolean hasGame(String game){
        for(Game g : games) if(g.name.equals(game)) return true;
        return false;
    }
    public Game getGame(String name){
        for(Game o : games) if(o.name.equals(name)) return o;
        return null;
    }

    @Override
    public String toString() {
        JSONObject config = new JSONObject();
        for(Game game : games){
            JSONObject gameObject = new JSONObject();
            for(Version version : game.versions){
                JSONObject versionObject = new JSONObject();
                for(Type type : version.types){
                    JSONObject typeObject = new JSONObject();

                    typeObject.put("file",type.file);
                    typeObject.put("size",type.size);
                    typeObject.put("changes",type.changes);
                    typeObject.put("title",type.title);

                    versionObject.put(type.name,typeObject);
                    versionObject.put("alpha",version.isAlpha);
                    gameObject.put(version.name,versionObject);
                    config.put(game.name,gameObject);
                }
            }
        }
        return config.toString();
    }

    public void load(){
        File file = new File(path);
        try {
            JSONObject config = new JSONObject(Util.readFile(file));
            for(String game : config.keySet()){
                JSONObject gameObject = config.getJSONObject(game);
                Game gameO = new Game(game);
                addGame(gameO);
                for(String version : gameObject.keySet()){
                    JSONObject versionObject = gameObject.getJSONObject(version);
                    Version versionO = new Version(version);
                    if(versionObject.has("alpha")) versionO.isAlpha = versionObject.getBoolean("alpha");
                    gameO.addVersion(versionO);
                    for(String type : versionObject.keySet()){
                        if(type.equals("alpha")) continue;
                        JSONObject typeObject = versionObject.getJSONObject(type);
                        Type typeO = new Type(type);
                        versionO.addType(typeO);
                        typeO.file = typeObject.getString("file");
                        typeO.size = typeObject.getLong("size");
                        typeO.changes = typeObject.getString("changes");
                        typeO.title = typeObject.getString("title");
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void save(){
        try {
            Util.writeStringToFile(Util.prettyJSON(toString()),new File(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
