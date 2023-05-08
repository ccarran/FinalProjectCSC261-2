/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.endicott.csc.serverside.backend;

/**
 *
 * @author cesar
 */
public class ServerResponse<T> {
    public T body;
    public int status;
    
    public ServerResponse<T> setStatus(int status) {
        this.status = status;
        return this;
    }
    
    public ServerResponse<T> setBody(T body) {
        this.body = body;
        return this;
    }
}
