package com.gome.ecmall.bean;

/**
 *【暂未引用】
 */
public class User {
    private String user;
    private String pwd;

    public User() {
    }

    public User(String user, String pwd) {
        this.user = user;
        this.pwd = pwd;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }
}