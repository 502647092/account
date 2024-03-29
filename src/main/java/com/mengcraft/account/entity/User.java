package com.mengcraft.account.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.mengcraft.account.lib.SecureUtil;

import java.security.SecureRandom;
import java.util.Random;

@Entity
@Table(name = "pre_ucenter_members")
public class User {

    @Id
    private int uid;

    @Column(length = 15, unique = true)
    private String username;

    @Column(length = 32)
    private String password;

    @Column(length = 6)
    private String salt;

    @Column(length = 15)
    private String regip;

    @Column
    private int regdate;

    @Column
    private int lastloginip;

    @Column
    private int lastlogintime;

    @Column(length = 8, nullable = false)
    private String secques;

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Set new password and a random salt.
     *
     * @param rawPassword The password will be encrypted automatic.
     */
    public void setRawPassword(String rawPassword) {
        SecureUtil util = SecureUtil.DEFAULT;
        salt = util.random(3);
        try {
            password = util.digest(util.digest(rawPassword) + salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean valid(String in) {
        SecureUtil util = SecureUtil.DEFAULT;
        try {
            in = util.digest(util.digest(in) + salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return password.equals(in);
    }

    /**
     * @return {@code true} if is a registered user.
     */
    public boolean valid() {
        return getUid() != 0;
    }

    public String getRegip() {
        return regip;
    }

    public void setRegip(String regip) {
        this.regip = regip;
    }

    public int getRegdate() {
        return regdate;
    }

    public void setRegdate(int regdate) {
        this.regdate = regdate;
    }

    public int getLastloginip() {
        return lastloginip;
    }

    public void setLastloginip(int lastloginip) {
        this.lastloginip = lastloginip;
    }

    public int getLastlogintime() {
        return lastlogintime;
    }

    public void setLastlogintime(int lastlogintime) {
        this.lastlogintime = lastlogintime;
    }

    public String getSecques() {
        return secques;
    }

    public void setSecques(String secques) {
        this.secques = secques;
    }

}
