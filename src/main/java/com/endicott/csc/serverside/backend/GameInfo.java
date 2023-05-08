/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.endicott.csc.serverside.backend;

import java.util.ArrayList;

/**
 *
 * @author cesar
 */
public class GameInfo {
//    This classes uses snake_cases instead of camelCases because the response
//    from the API uses them
    
    public class GameEntry {
        public class Genre {
            public String id;
            public String description;
        }
        public class ReleaseDate{
            public String date;
            public boolean coming_soon;
        }
        public class Platforms {
            public boolean windows;
            public boolean mac;
            public boolean linux;
        }
    
        public String steam_appid;
        public String type;
        public String name;
        public ArrayList<Genre> genres;
        public String short_description;
        public ReleaseDate release_date;
        public ArrayList<String> publishers;
        public ArrayList<String> developers;
        public Platforms platforms;
        public String website;
        public String header_image;   
    }
    
    public boolean success;
    public GameEntry data;
}
