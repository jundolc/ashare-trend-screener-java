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
        char c=text.charAt(pos);
        if(c=='{') return object(); if(c=='[') return array(); if(c=='"') return string();
        if(c=='t') return literal("true",true); if(c=='f') return literal("false",false); if(c=='n') return literal("null",null);
        return number();
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
            char e=text.charAt(pos++);
            if(e=='"'||e=='\\'||e=='/') b.append(e); else if(e=='b')b.append('\b'); else if(e=='f')b.append('\f');
            else if(e=='n')b.append('\n'); else if(e=='r')b.append('\r'); else if(e=='t')b.append('\t');
            else if(e=='u'){b.append((char)Integer.parseInt(text.substring(pos,pos+4),16));pos+=4;} else throw error("bad escape");
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
        if(o instanceof Number) return ((Number)o).doubleValue();
        String value=str(o).trim();
        if(value.isEmpty() || value.equals("-") || value.equals("null")) return Double.NaN;
        return Double.parseDouble(value);
    }
}
