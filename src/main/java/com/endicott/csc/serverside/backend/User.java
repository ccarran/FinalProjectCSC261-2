/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.endicott.csc.serverside.backend;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author cesar
 */
public class User {
    public String username;
    public String password;
    public HashMap<String, Integer> gamelist = new HashMap<>();
    
    public User(String user, String newPassword) {
        this.username = user;
        this.password = newPassword;
    }
    
    public void updateGameEntry(String gameId, int score) {
        this.gamelist.put(gameId, score);
    }
}
