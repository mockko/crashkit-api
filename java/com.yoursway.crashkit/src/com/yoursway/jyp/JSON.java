package com.yoursway.jyp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Encodes and decodes JSON (JavaScript Object Notation).
 * 
 * @author Andrey Tarantsov <andreyvit@gmail.com>
 */
public class JSON {
    
    public static class SyntaxError extends Exception {
        private static final long serialVersionUID = 1L;
        
        public SyntaxError(String s, int charIndex) {
            super(s + " on character " + charIndex);
        }
        
    }
    
    public static String encode(Object value) {
        StringBuilder result = new StringBuilder();
        encode(value, result);
        return result.toString();
    }
    
    public static void encode(Object value, StringBuilder result) {
        try {
            encode(value, (Appendable) result);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    public static void encode(Object value, Appendable result) throws IOException {
        if (value == null)
            result.append("null");
        else if (value instanceof CharSequence)
            encodeString((CharSequence) value, result);
        else if (value instanceof Character)
            encodeString(value.toString(), result);
        else if (value instanceof Float)
            result.append(encodeFloat((Float) value));
        else if (value instanceof Double)
            result.append(encodeDouble((Double) value));
        else if (value instanceof Number)
            result.append(encodeNumber((Number) value));
        else if (value instanceof Boolean)
            result.append(encodeBoolean((Boolean) value));
        else if (value instanceof Map<?, ?>)
            encodeMap((Map<?, ?>) value, result);
        else if (value instanceof Iterable<?>)
            encodeArray((Iterable<?>) value, result);
        else if (value.getClass().isArray()) {
            Object[] array = (Object[]) value;
            encodeArray(Arrays.asList(array), result);
        } else {
            throw new IllegalArgumentException("Cannot encode complex types into JSON: "
                    + value.getClass().getName());
        }
    }
    
    public static Object decode(String string) throws SyntaxError {
        StringReader reader = new StringReader(string);
        try {
            return decode(reader);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    public static Object decode(Reader reader) throws SyntaxError, IOException {
        JSONTokener tokenizer = new JSONTokener(reader);
        return tokenizer.nextValue();
    }
    
    private static void encodeString(CharSequence string, Appendable sb) throws IOException {
        char c = 0;
        final int len = string.length();
        
        sb.append('"');
        for (int i = 0; i < len; i += 1) {
            char b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\').append(c);
                break;
            case '/':
                if (b == '<')
                    sb.append('\\');
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ' || (c >= '\u0080' && c < '\u00a0') || (c >= '\u2000' && c < '\u2100')) {
                    String t = "000" + Integer.toHexString(c);
                    sb.append("\\u" + t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
    }
    
    private static void encodeArray(Iterable<?> iterable, Appendable result) throws IOException {
        result.append('[');
        boolean first = true;
        for (Object value : iterable) {
            if (first)
                first = false;
            else
                result.append(',');
            encode(value, result);
        }
        result.append(']');
    }
    
    private static void encodeMap(Map<?, ?> map, Appendable result) throws IOException {
        result.append('{');
        boolean first = true;
        List<Map.Entry<?, ?>> entries = new ArrayList<Map.Entry<?, ?>>(map.entrySet());
        try {
            Collections.sort(entries, ENTRY_KEY_COMPARATOR);
        } catch (ClassCastException e) {
            // uncomparable, go unsorted
        }
        for (Map.Entry<?, ?> entry : entries) {
            if (first)
                first = false;
            else
                result.append(',');
            encode(entry.getKey(), result);
            result.append(':');
            encode(entry.getValue(), result);
            
        }
        result.append('}');
    }
    
    private static String encodeBoolean(boolean value) {
        return Boolean.toString(value);
    }
    
    private static String encodeFloat(Float n) {
        if (n.isInfinite())
            throw new IllegalArgumentException("Infinite numbers cannot be represented in JSON");
        if (n.isNaN())
            throw new IllegalArgumentException("Infinite numbers cannot be represented in JSON");
        return encodeNumber(n);
    }
    
    private static String encodeDouble(Double n) {
        if (n.isInfinite())
            throw new IllegalArgumentException("Infinite numbers cannot be represented in JSON");
        if (n.isNaN())
            throw new IllegalArgumentException("Infinite numbers cannot be represented in JSON");
        return encodeNumber(n);
    }
    
    private static String encodeNumber(Number n) {
        return chopTrailingDecimalZeros(n.toString());
    }
    
    private static String chopTrailingDecimalZeros(String s) {
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0"))
                s = s.substring(0, s.length() - 1);
            if (s.endsWith("."))
                s = s.substring(0, s.length() - 1);
        }
        return s;
    }
    
    final static Comparator<Map.Entry<?, ?>> ENTRY_KEY_COMPARATOR = new Comparator<Entry<?, ?>>() {
        
        @SuppressWarnings("unchecked")
        public int compare(Entry<?, ?> o1, Entry<?, ?> o2) {
            return ((Comparable<Object>) o1.getKey()).compareTo(o2.getKey());
        }
    };
    
}

/**
 * A JSONTokener takes a source string and extracts characters and tokens from
 * it.
 * 
 * @author JSON.org
 * @author Andrey Tarantsov <andreyvit@gmail.com>
 * @version 2008-09-18
 */
class JSONTokener {
    
    private int index;
    private Reader reader;
    private char lastChar;
    private boolean useLastChar;
    
    JSONTokener(Reader reader) {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        this.useLastChar = false;
        this.index = 0;
    }
    
    /**
     * Back up one character. This provides a sort of lookahead capability, so
     * that you can test for a digit or letter before attempting to parse the
     * next number or identifier.
     */
    private void back() {
        if (useLastChar || index <= 0) {
            throw new AssertionError("Stepping back two steps is not supported");
        }
        index -= 1;
        useLastChar = true;
    }
    
    /**
     * Get the next character in the source string.
     * 
     * @return The next character, or 0 if past the end of the source string.
     */
    private char next() throws IOException {
        if (this.useLastChar) {
            this.useLastChar = false;
            if (this.lastChar != 0) {
                this.index += 1;
            }
            return this.lastChar;
        }
        int c = this.reader.read();
        
        if (c <= 0) { // End of stream
            this.lastChar = 0;
            return 0;
        }
        this.index += 1;
        this.lastChar = (char) c;
        return this.lastChar;
    }
    
    /**
     * Get the next n characters.
     * 
     * @param n
     *            The number of characters to take.
     * @return A string of n characters.
     */
    private String next(int n) throws IOException, JSON.SyntaxError {
        if (n == 0) {
            return "";
        }
        
        char[] buffer = new char[n];
        int pos = 0;
        
        if (this.useLastChar) {
            this.useLastChar = false;
            buffer[0] = this.lastChar;
            pos = 1;
        }
        
        int len;
        while ((pos < n) && ((len = reader.read(buffer, pos, n - pos)) != -1)) {
            pos += len;
        }
        this.index += pos;
        
        if (pos < n)
            throw new JSON.SyntaxError("Unexpected end of JSON string", index);
        
        this.lastChar = buffer[n - 1];
        return new String(buffer);
    }
    
    /**
     * Get the next char in the string, skipping whitespace.
     * 
     * @return A character, or 0 if there are no more characters.
     */
    private char nextClean() throws IOException {
        for (;;) {
            char c = next();
            if (c == 0 || c > ' ') {
                return c;
            }
        }
    }
    
    /**
     * Return the characters up to the next close quote character. Backslash
     * processing is done. The formal JSON format does not allow strings in
     * single quotes, but an implementation is allowed to accept them.
     * 
     * @param quote
     *            The quoting character, either <code>"</code>&nbsp;<small>(double
     *            quote)</small> or <code>'</code>&nbsp;<small>(single quote)</small>.
     * @return A String.
     * @throws IOException
     *             Unterminated string.
     */
    private String nextString(char quote) throws IOException, JSON.SyntaxError {
        char c;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = next();
            switch (c) {
            case 0:
            case '\n':
            case '\r':
                throw new JSON.SyntaxError("Unterminated string", index);
            case '\\':
                c = next();
                switch (c) {
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    sb.append((char) Integer.parseInt(next(4), 16));
                    break;
                case 'x':
                    sb.append((char) Integer.parseInt(next(2), 16));
                    break;
                default:
                    sb.append(c);
                }
                break;
            default:
                if (c == quote) {
                    return sb.toString();
                }
                sb.append(c);
            }
        }
    }
    
    private Map<Object, Object> nextObject() throws JSON.SyntaxError, IOException {
        String key;
        Map<Object, Object> result = new HashMap<Object, Object>();
        
        if (nextClean() != '{') {
            throw new JSON.SyntaxError("A JSON object must begin with '{'", index);
        }
        for (;;) {
            char c = nextClean();
            switch (c) {
            case 0:
                throw new JSON.SyntaxError("Unexpected end of JSON: unterminated object, '}' expected", index);
            case '}':
                return result;
            default:
                back();
                key = nextValue().toString();
            }
            
            // The key is followed by ':'. We will also tolerate '=' or '=>'.
            c = nextClean();
            if (c == '=') {
                if (next() != '>') {
                    back();
                }
            } else if (c != ':') {
                throw new JSON.SyntaxError("Expected a ':' after a key", index);
            }
            result.put(key, nextValue());
            
            /*
             * Pairs are separated by ','. We will also tolerate ';'.
             */

            switch (nextClean()) {
            case ';':
            case ',':
                if (nextClean() == '}') {
                    return result;
                }
                back();
                break;
            case '}':
                return result;
            default:
                throw new JSON.SyntaxError("Expected a ',' or '}'", index);
            }
        }
        
    }
    
    private List<Object> nextArray() throws JSON.SyntaxError, IOException {
        List<Object> result = new ArrayList<Object>();
        
        char c = nextClean();
        char q;
        if (c == '[') {
            q = ']';
        } else if (c == '(') {
            q = ')';
        } else {
            throw new JSON.SyntaxError("A JSON array must start with '['", index);
        }
        if (nextClean() == ']') {
            return result;
        }
        back();
        for (;;) {
            if (nextClean() == ',') {
                back();
                result.add(null);
            } else {
                back();
                result.add(nextValue());
            }
            c = nextClean();
            switch (c) {
            case ';':
            case ',':
                if (nextClean() == ']') {
                    return result;
                }
                back();
                break;
            case ']':
            case ')':
                if (q != c) {
                    throw new JSON.SyntaxError(("Expected a '" + new Character(q) + "'"), index);
                }
                return result;
            default:
                throw new JSON.SyntaxError("Expected a ',' or ']'", index);
            }
        }
        
    }
    
    /**
     * Get the next value. The value can be a Boolean, Double, Integer,
     * JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
     * 
     * @throws JSONException
     *             If syntax error.
     * 
     * @return An object.
     */
    public Object nextValue() throws JSON.SyntaxError, IOException {
        char c = nextClean();
        String s;
        
        switch (c) {
        case 0:
            throw new JSON.SyntaxError("JSON is empty", index);
        case '"':
        case '\'':
            return nextString(c);
        case '{':
            back();
            return nextObject();
        case '[':
        case '(':
            back();
            return nextArray();
        }
        
        /*
         * Handle unquoted text. This could be the values true, false, or null,
         * or it can be a number. An implementation (such as this one) is
         * allowed to also accept non-standard forms.
         * 
         * Accumulate characters until we reach the end of the text or a
         * formatting character.
         */

        s = accumulateUnquotedText(c);
        if (s.equals("")) {
            throw new JSON.SyntaxError("Missing value", index);
        }
        return parseJsonWord(s);
    }
    
    private String accumulateUnquotedText(char c) throws IOException {
        String s;
        StringBuffer sb = new StringBuffer();
        while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
            sb.append(c);
            c = next();
        }
        back();
        
        s = sb.toString().trim();
        return s;
    }
    
    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     * 
     * @param s
     *            A String.
     * @return A simple JSON value.
     */
    private Object parseJsonWord(String s) throws JSON.SyntaxError {
        if (s.equals("")) {
            return s;
        }
        if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (s.equalsIgnoreCase("null")) {
            return null;
        }
        
        /*
         * If it might be a number, try converting it. We support the 0- and 0x-
         * conventions. If a number cannot be produced, then the value will just
         * be a string. Note that the 0-, 0x-, plus, and implied string
         * conventions are non-standard. A JSON parser is free to accept
         * non-JSON forms as long as it accepts all correct JSON forms.
         */

        char b = s.charAt(0);
        if ((b >= '0' && b <= '9') || b == '.' || b == '-' || b == '+') {
            if (b == '0') {
                if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                    try {
                        return new Integer(Integer.parseInt(s.substring(2), 16));
                    } catch (Exception e) {
                        throw new JSON.SyntaxError("Unparsable hex integer: " + s, index);
                    }
                } else {
                    try {
                        return new Integer(Integer.parseInt(s, 8));
                    } catch (Exception e) {
                        throw new JSON.SyntaxError("Unparsable oct integer: " + s, index);
                    }
                }
            }
            try {
                return new Integer(s);
            } catch (NumberFormatException e) {
                try {
                    return new Long(s);
                } catch (NumberFormatException f) {
                    try {
                        return new Double(s);
                    } catch (NumberFormatException g) {
                        throw new JSON.SyntaxError("Unparsable number: " + s, index);
                    }
                }
            }
        }
        return s;
    }
    
}
