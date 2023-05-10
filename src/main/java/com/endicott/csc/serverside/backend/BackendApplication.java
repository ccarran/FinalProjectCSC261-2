package com.endicott.csc.serverside.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "null")
public class BackendApplication {
    public final int STATUS_CODE_ALL_OK                     = 1;
    public final int STATUS_CODE_USER_NOT_FOUND             = 2;
    public final int STATUS_CODE_WRONG_PASSWORD             = 3;
    public final int STATUS_CODE_USER_ALREADY_EXISTS        = 4;
    public final int STATUS_CODE_USER_COULD_NOT_BE_CREATED  = 5;
    public final int STATUS_CODE_GAME_NOT_FOUND             = 6;
    
    public final int STATUS_CODE_UNKOWN_ERROR               = -1;
    
    Gson gson = new Gson();
    
    public class Request {
        String gameRequest;
    }
    
    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        checkSteamAPI();
        SpringApplication.run(BackendApplication.class, args);
    }
    
    private static void checkSteamAPI() {
        GameList steamGameList = null;
        
        try {
            // First, we check if we have that list already       
            File dir = new File("data/gamelist");
            dir.mkdirs();
            File steamGamesList = new File(dir, "allGames.json");
            if(!steamGamesList.exists()) {
                //Then, we get the list of games from the Steam API
                URL steamGameURL = new URL("http://api.steampowered.com/ISteamApps/GetAppList/v0002/?key=STEAMKEY&format=json");
                HttpURLConnection connectListdata = (HttpURLConnection) steamGameURL.openConnection();
                BufferedReader readerListdata = new BufferedReader(new InputStreamReader(connectListdata.getInputStream()));

                //We parse the list into a json
                JsonObject gameListJson = JsonParser.parseReader(readerListdata).getAsJsonObject();
                Gson gson = new Gson();
                
                steamGameList = gson.fromJson(gameListJson, GameList.class);
   
                steamGameList.applist.apps.sort((o1, o2) -> o1.name.compareTo(
                        o2.name));
                
                // Finally, we write to the file
                FileWriter writer = new FileWriter("data/gamelist/allGamesFile.json");
                writer.write(gson.toJson(steamGameList));
                writer.close();
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param user
     * @param password
     * @return
     */
    @PostMapping("/login")
    public ServerResponse<User> login(String user, String password){
        ServerResponse<User> response = new ServerResponse();
        
        File dir = new File("data/users");
        dir.mkdirs();
        
        File userFile = new File(dir, user + ".json");
        if (userFile.exists()) {
            try {
                JsonObject userFileJson = JsonParser.parseReader(new FileReader(userFile)).getAsJsonObject();
                User userInfo = this.gson.fromJson(userFileJson, User.class);
                
                if (!userInfo.password.equals(password)) {
                    return response.setStatus(STATUS_CODE_WRONG_PASSWORD);
                }
                else {
                    response.setBody(userInfo).setStatus(STATUS_CODE_ALL_OK);
                    
                    return response;
                }
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            return response.setStatus(STATUS_CODE_USER_NOT_FOUND);
        }
        
        return response.setStatus(STATUS_CODE_UNKOWN_ERROR);
    }

    /**
     *
     * @param user
     * @param password
     * @return
     */
    @PostMapping("/createUser")
    public ServerResponse<User> createUser(String user, String password) {
        ServerResponse response = new ServerResponse().setBody(null);
               
        File dir = new File("data/users");
        dir.mkdirs();
        
        File file = new File(dir, user + ".json");
        if (file.exists()) {
            return response.setStatus(STATUS_CODE_USER_ALREADY_EXISTS);
        }
        
        try {
            User newUser = new User(user, password);
            
            FileWriter fileWriter = new FileWriter("data/users/" + user + ".json");
            fileWriter.write(gson.toJson(newUser, User.class));
            fileWriter.close();
            
            response.setBody(newUser);
        } catch (IOException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            return response.setStatus(STATUS_CODE_USER_COULD_NOT_BE_CREATED);
        }
        
        return response.setStatus(STATUS_CODE_ALL_OK);
    }

    /**
     *
     * @param user
     * @param gameId
     * @return
     */
    @PostMapping("/addGameToUser")
    public ServerResponse<User> addGameToUser(String user, String gameId) {
        ServerResponse response = new ServerResponse().setBody(null);
        
        File listFile = new File("data/gamelist", "allGamesFile.json");
        File file = new File("data/users", user + ".json");
        
        if (file.exists()) {
            try {
                JsonObject userFileJson = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                JsonObject listFileJson = JsonParser.parseReader(new FileReader(listFile)).getAsJsonObject();
                User userInfo = this.gson.fromJson(userFileJson, User.class);
                GameList list = this.gson.fromJson(listFileJson, GameList.class);
                
                userInfo.gamelist.put(gameId, 0);
                
                for (int i = 0; i < list.applist.apps.size(); i++){
                    if (list.applist.apps.get(i).appid.equals(gameId)) {                        
                        list.applist.apps.get(i).userNumber++;
                        break;
                    }
                }
                
                FileWriter fileWriter = new FileWriter("data/users/" + user + ".json");
                fileWriter.write(gson.toJson(userInfo, User.class));
                fileWriter.close();
                
                FileWriter fileWriter2 = new FileWriter("data/gamelist/allGamesFile.json");
                fileWriter2.write(gson.toJson(list, GameList.class));
                fileWriter2.close();
                
                response.setBody(userInfo).setStatus(STATUS_CODE_ALL_OK);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return response;
    }

    /**
     *
     * @param user
     * @param gameId
     * @return
     */
    @PostMapping("/removeGameFromUser")
    public ServerResponse<User> removeGameFromUser(String user, String gameId) {
        ServerResponse response = new ServerResponse().setBody(null);
        
        File file = new File("data/users", user + ".json");
        File listFile = new File("data/gamelist", "allGamesFile.json");
        
        if (file.exists()) {
            try {
                JsonObject userFileJson = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                JsonObject listFileJson = JsonParser.parseReader(new FileReader(listFile)).getAsJsonObject();
                User userInfo = this.gson.fromJson(userFileJson, User.class);
                GameList list = this.gson.fromJson(listFileJson, GameList.class);
                
                userInfo.gamelist.put(gameId, null);
                
                for (int i = 0; i < list.applist.apps.size(); i++){
                    if (list.applist.apps.get(i).appid.equals(gameId)) {                        
                        list.applist.apps.get(i).userNumber--;
                        break;
                    }
                }
                
                FileWriter fileWriter = new FileWriter("data/users/" + user + ".json");
                fileWriter.write(gson.toJson(userInfo, User.class));
                fileWriter.close();
                
                FileWriter fileWriter2 = new FileWriter("data/gamelist/allGamesFile.json");
                fileWriter2.write(gson.toJson(list, GameList.class));
                fileWriter2.close();
                
                response.setBody(userInfo).setStatus(STATUS_CODE_ALL_OK);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return response;
    }

    /**
     *
     * @param user
     * @param gameId
     * @param score
     * @return
     */
    @PostMapping("/updateGameScoreFromUser")
    public ServerResponse<User> updateGameScoreUser(String user, String gameId, String score){
        ServerResponse response = new ServerResponse().setBody(null);
        
        File file = new File("data/users", user + ".json");
        File gamesFile = new File("data/gamelist", "allGamesFile.json");
        
        if (file.exists()) {
            try {
                JsonObject userFileJson = JsonParser.parseReader(new FileReader(file)).getAsJsonObject();
                JsonObject gameFileJson = JsonParser.parseReader(new FileReader(gamesFile)).getAsJsonObject();
                User userInfo = this.gson.fromJson(userFileJson, User.class);
                GameList list = this.gson.fromJson(gameFileJson, GameList.class);
                
                int previousScore = userInfo.gamelist.get(gameId);
                
                userInfo.gamelist.put(gameId, Integer.valueOf(score));
                
                for (int i = 0; i < list.applist.apps.size(); i++){
                    if (list.applist.apps.get(i).appid.equals(gameId)) {
                        list.applist.apps.get(i).rating -= previousScore;
                        list.applist.apps.get(i).rating += Integer.parseInt(score);
                        break;
                    }
                }
                FileWriter fileWriter = new FileWriter("data/users/" + user + ".json");
                fileWriter.write(gson.toJson(userInfo, User.class));
                fileWriter.close();
                
                FileWriter fileWriter2 = new FileWriter("data/gamelist/" + "allGamesFile.json");
                fileWriter2.write(gson.toJson(list, GameList.class));
                fileWriter2.close();
                
                response.setBody(userInfo).setStatus(STATUS_CODE_ALL_OK);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        return response;
    }
    
    /**
     *
     * @param gameRequest
     * @return
     */
    @GetMapping("/gameInfo")
    public ServerResponse<GameInfo> gameInfo(String gameRequest) {
        GameInfo tmpGameEntry = null;
        ServerResponse response = new ServerResponse();
        
        try {
            URL steamGameURL = new URL("https://store.steampowered.com/api/appdetails?appids=" + gameRequest);
            HttpURLConnection connectGamedata = (HttpURLConnection)steamGameURL.openConnection();
            BufferedReader readerGamedata = new BufferedReader(new InputStreamReader(connectGamedata.getInputStream()));
            
            JsonObject gamedataJson = JsonParser.parseReader(readerGamedata).getAsJsonObject().getAsJsonObject(gameRequest);
        
            tmpGameEntry = this.gson.fromJson(gamedataJson, GameInfo.class);
        } catch (MalformedURLException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
//            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("API couldn't get a response for gameId: " + gameRequest);
        }
        
        if (tmpGameEntry == null || !tmpGameEntry.success) {
            return response.setStatus(STATUS_CODE_GAME_NOT_FOUND);
        }
        else{
            return response.setBody(tmpGameEntry).setStatus(STATUS_CODE_ALL_OK);
        }
    }

    /**
     *
     * @param keyWord
     * @return
     */
    @GetMapping("/gameSearch")
    public ServerResponse<ArrayList<GameList.AppList.Game>> gameSearch(String keyWord) {
        File gamelistFile = new File("data/gamelist", "allGamesFile.json");
        ArrayList<GameList.AppList.Game> gamesfound = new ArrayList<>();
        
        try {
            JsonObject gamelistJson = JsonParser.parseReader(
                    new FileReader(gamelistFile)).getAsJsonObject();
            GameList gamelist = gson.fromJson(gamelistJson, GameList.class);
            
            for (int i=0; i < gamelist.applist.apps.size(); i++) {
                GameList.AppList.Game tmp = gamelist.applist.apps.get(i);
                
                if (tmp.name.toLowerCase().contains(keyWord.toLowerCase())) {
                    gamesfound.add(tmp);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return new ServerResponse().setBody(gamesfound).setStatus(STATUS_CODE_ALL_OK);
    }

    /**
     *
     * @param user
     * @return
     */
    @GetMapping("/getUserList")
    public ServerResponse<ArrayList<GameList.AppList.Game>> getUserList(String user) {
        File gamelistFile = new File("data/gamelist", "allGamesFile.json");
        File userlistFile = new File("data/users", user + ".json");
        
        ArrayList<GameList.AppList.Game> gamesfound = new ArrayList<>();
        
        try {
            JsonObject gamelistJson = JsonParser.parseReader(
                    new FileReader(gamelistFile)).getAsJsonObject();
            GameList gamelist = gson.fromJson(gamelistJson, GameList.class);
            
            JsonObject userFileJson = JsonParser.parseReader(new FileReader(userlistFile)).getAsJsonObject();
            User userInfo = this.gson.fromJson(userFileJson, User.class);
            
            for (int i=0; i < gamelist.applist.apps.size(); i++) {
                GameList.AppList.Game tmp = gamelist.applist.apps.get(i);
                
                if (userInfo.gamelist.containsKey(tmp.appid) && !gamesfound.contains(tmp)) {
                    gamesfound.add(tmp);
                }
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return new ServerResponse().setBody(gamesfound).setStatus(STATUS_CODE_ALL_OK);
    }
    
    /**
     *
     * @param user
     * @return
     */
    @GetMapping("/userInfo")
    public ServerResponse<User> getUserInfo(String user) {
        ServerResponse response = new ServerResponse().setBody(null);
        
        File dir = new File("data/users");
        dir.mkdirs();
        
        File userFile = new File(dir, user + ".json");
        if (userFile.exists()) {
            try {
                JsonObject userFileJson = JsonParser.parseReader(new FileReader(userFile)).getAsJsonObject();
                User userInfo = this.gson.fromJson(userFileJson, User.class);
                
                response.setBody(userInfo);
                
                return response.setStatus(STATUS_CODE_ALL_OK);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else {
            return response.setStatus(STATUS_CODE_USER_NOT_FOUND);
        }
        
        return response.setStatus(STATUS_CODE_UNKOWN_ERROR);
    }

    /**
     *
     * @return
     */
    @GetMapping("/randomGame")
    public ServerResponse<GameInfo> randomGame() {
        
        File gamelistFile = new File("data/gamelist", "allGamesFile.json");
        
        try {
            JsonObject gamelistJson = JsonParser.parseReader(
                new FileReader(gamelistFile)).getAsJsonObject();
            GameList gamelist = gson.fromJson(gamelistJson, GameList.class);
            
            int random = ThreadLocalRandom.current().nextInt(0, gamelist.applist.apps.size());
            
            return this.gameInfo(gamelist.applist.apps.get(random).appid);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BackendApplication.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return new ServerResponse().setStatus(STATUS_CODE_UNKOWN_ERROR);
    }
}
