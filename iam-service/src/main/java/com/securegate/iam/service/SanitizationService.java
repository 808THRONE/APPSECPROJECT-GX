package com.securegate.iam.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SanitizationService {

    /**
     * Sanitizes a string by escaping HTML characters to prevent XSS.
     * 
     * @param input The raw input string
     * @return The sanitized/escaped string
     */
    public String sanitize(String input) {
        if (input == null) {
            return null;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '&':
                    out.append("&amp;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&#39;");
                    break;
                case '/':
                    out.append("&#47;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Specialized sanitization for alphanumeric keys (no escaping needed, just
     * filtering).
     */
    public String sanitizeKey(String key) {
        if (key == null)
            return null;
        return key.replaceAll("[^a-zA-Z0-9_\\-]", "");
    }
}
