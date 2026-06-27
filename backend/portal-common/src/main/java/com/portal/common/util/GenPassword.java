package com.portal.common.util;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenPassword {
    public static void main(String[] args) {
        GenPassword gen = new GenPassword();
        System.out.println("==============BCryptPasswordEncoder==============");
        gen.genPwd("admin");
        gen.genPwd("user1");
        gen.genPwd("dbadmin");
    }

    public void genPwd(String pwd) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(pwd);
        System.out.println("【" + pwd + "】加密后的值：【" + hash + "】");
    }
}
