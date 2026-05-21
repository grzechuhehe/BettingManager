package com.grzechuhehe.SportsBettingManagerApp.util;

public class PiiUtils {

    /**
     * Masks an email address for safe logging.
     * Example: huber.betting@gmail.com -> h***g@gmail.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        
        int atIndex = email.indexOf("@");
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (name.length() <= 2) {
            return name.substring(0, 1) + "***" + domain;
        }
        
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }
}
