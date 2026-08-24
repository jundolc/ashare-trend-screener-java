package com.jundolc.ashare;

import java.util.*;

/** Small JSON parser used to keep this project dependency-free. */
final class Json {
    private final String text;
    private int pos;
    private Json(String text) { this.text = text; }
    static Object parse(String text) { return new Json(text).value(); }

    private Object value() {
        ws();
        if (pos >= text.length()) throw error("unexpected end");
        return switch (text.charAt(pos)) {
            case '{' -> object(); case '[' -> array(); case '"' -> string();
            case 't' -> literal("true", true); case 'f' -> literal("false", false);
            case 'n' -> literal("null", null); default -> number();
        };
    }
    private Map<String,Object> object() {
        Map<String,Object> out = new LinkedHashMap<>(); pos++; ws();
        if (take('}')) return out;
        do { ws(); String key=string(); ws(); require(':'); out.put(key,value()); ws(); } while (take(','));
        require('}'); return out;
    }
    private List<Object> array() {
        List<Object> out=new ArrayList<>(); pos++; ws(); if (take(']')) return out;
        do { out.add(value()); ws(); } while (take(',')); require(']'); return out;
    }
    private String string() {
        require('"'); StringBuilder b=new StringBuilder();
        while (pos < text.length()) {
            char c=text.charAt(pos++); if (c=='"') return b.toString();
            if (c!='\\') { b.append(c); continue; }
            char e=text.charAt(pos++); b.append(switch(e) {
                case '"','\\','/' -> e; case 'b' -> '\b'; case 'f' -> '\f';
                case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                case 'u' -> (char)Integer.parseInt(text.substring(pos, pos+=4),16);
                default -> throw error("bad escape"); });
        } throw error("unterminated string");
    }
    private Number number() {
        int start=pos; while(pos<text.length() && "-+0123456789.eE".indexOf(text.charAt(pos))>=0) pos++;
        String n=text.substring(start,pos); try { return Double.valueOf(n); } catch(Exception e) { throw error("bad number"); }
    }
    private Object literal(String expected,Object value) {
        if (!text.startsWith(expected,pos)) throw error("bad literal"); pos+=expected.length(); return value;
    }
    private void ws(){ while(pos<text.length() && Character.isWhitespace(text.charAt(pos))) pos++; }
    private boolean take(char c){ if(pos<text.length() && text.charAt(pos)==c){pos++;return true;} return false; }
    private void require(char c){ if(!take(c)) throw error("expected "+c); }
    private IllegalArgumentException error(String m){ return new IllegalArgumentException(m+" at "+pos); }

    @SuppressWarnings("unchecked") static Map<String,Object> map(Object o){ return (Map<String,Object>)o; }
    @SuppressWarnings("unchecked") static List<Object> list(Object o){ return (List<Object>)o; }
    static String str(Object o){ return o==null ? "" : String.valueOf(o); }
    static double num(Object o){
        if(o instanceof Number n) return n.doubleValue();
        String value=str(o).trim();
        if(value.isEmpty() || value.equals("-") || value.equals("null")) return Double.NaN;
        return Double.parseDouble(value);
    }
}
