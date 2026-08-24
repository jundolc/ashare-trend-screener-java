package com.jundolc.ashare;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static com.jundolc.ashare.Model.*;

final class MarketClient {
    private final int retries;
    MarketClient(int retries){this.retries=retries;}

    List<Quote> spot() throws Exception {
        try { List<Quote> q=eastmoneySpot(); System.out.println("INFO 实时行情源：东方财富"); return q; }
        catch(Exception first){ System.out.println("WARN 东方财富不可用，自动切换新浪行情："+first.getClass().getSimpleName());
            List<Quote> q=sinaSpot(); System.out.println("INFO 实时行情源：新浪（备用）"); return q; }
    }
    private List<Quote> eastmoneySpot() throws Exception {
        List<Quote> out=new ArrayList<>(); int page=1,total=Integer.MAX_VALUE;
        while(out.size()<total){
            String url="https://82.push2.eastmoney.com/api/qt/clist/get?pn="+page+"&pz=100&po=1&np=1&ut=bd1d9ddb04089700cf9c27f6f7426281&fltt=2&invt=2&fid=f12&fs="+enc("m:0 t:6,m:0 t:80,m:1 t:2,m:1 t:23,m:0 t:81 s:2048")+"&fields=f2,f5,f12,f14,f17,f18";
            Map<String,Object> root=Json.map(Json.parse(get(url,StandardCharsets.UTF_8))); Map<String,Object> data=Json.map(root.get("data"));
            total=(int)Json.num(data.get("total")); List<Object> rows=Json.list(data.get("diff")); if(rows.isEmpty())break;
            for(Object o:rows){Map<String,Object> r=Json.map(o); String code=Json.str(r.get("f12"));
                out.add(new Quote(code,Json.str(r.get("f14")),Json.num(r.get("f2")),Json.num(r.get("f17")),Json.num(r.get("f18")),Json.num(r.get("f5"))*100,LocalDate.now()));}
            page++;
        } return out;
    }
    private List<Quote> sinaSpot() throws Exception {
        List<Quote> out=new ArrayList<>();
        for(int page=1;page<=100;page++){
            String url="https://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?page="+page+"&num=80&sort=symbol&asc=1&node=hs_a&symbol=";
            List<Object> rows=Json.list(Json.parse(get(url,StandardCharsets.UTF_8))); if(rows.isEmpty())break;
            for(Object o:rows){Map<String,Object> r=Json.map(o);String raw=Json.str(r.get("symbol"));String code=raw.replaceAll(".*?(\\d{6})$","$1");
                out.add(new Quote(code,Json.str(r.get("name")),Json.num(r.get("trade")),Json.num(r.get("open")),Json.num(r.get("settlement")),Json.num(r.get("volume")),LocalDate.now()));}
        } return out;
    }
    List<Bar> history(String code,LocalDate start,LocalDate end) throws Exception {
        try{return eastmoneyHistory(code,start,end);}catch(Exception e){return tencentHistory(code,start,end);}
    }
    private List<Bar> eastmoneyHistory(String code,LocalDate start,LocalDate end)throws Exception{
        String secid=(code.startsWith("6")?"1.":"0.")+code;
        String url="https://push2his.eastmoney.com/api/qt/stock/kline/get?secid="+secid+"&klt=101&fqt=1&beg="+fmt(start)+"&end="+fmt(end)+"&lmt=100&fields1=f1&fields2=f51,f52,f53,f54,f55,f56";
        Map<String,Object> data=Json.map(Json.map(Json.parse(get(url,StandardCharsets.UTF_8))).get("data"));
        List<Bar> out=new ArrayList<>(); for(Object o:Json.list(data.get("klines"))){String[] x=Json.str(o).split(",");out.add(new Bar(LocalDate.parse(x[0]),d(x[1]),d(x[2]),d(x[5])*100));}return out;
    }
    private List<Bar> tencentHistory(String code,LocalDate start,LocalDate end)throws Exception{
        String symbol=(code.startsWith("6")?"sh":code.startsWith("4")||code.startsWith("8")||code.startsWith("9")?"bj":"sz")+code;
        String param=symbol+",day,"+start+","+end+",100,qfq";
        String body=get("https://proxy.finance.qq.com/ifzqgtimg/appstock/app/newfqkline/get?param="+enc(param),StandardCharsets.UTF_8);
        int brace=body.indexOf('{'); Map<String,Object> root=Json.map(Json.parse(body.substring(brace))); Map<String,Object> node=Json.map(Json.map(root.get("data")).get(symbol));
        Object rows=node.getOrDefault("qfqday",node.get("day")); List<Bar> out=new ArrayList<>();
        for(Object o:Json.list(rows)){List<Object>x=Json.list(o);double raw=Json.num(x.get(5));double shares=(symbol.startsWith("sh688")?raw:raw*100);out.add(new Bar(LocalDate.parse(Json.str(x.get(0))),Json.num(x.get(1)),Json.num(x.get(2)),shares));}return out;
    }
    private String get(String url,java.nio.charset.Charset charset)throws Exception{
        Exception last=null;
        for(int i=1;i<=retries;i++) try {
            HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
            connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent","Mozilla/5.0");
            int status=connection.getResponseCode(); if(status/100!=2)throw new IOException("HTTP "+status);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(); InputStream in=connection.getInputStream();
            byte[] buffer=new byte[8192]; int n; while((n=in.read(buffer))!=-1)bytes.write(buffer,0,n); in.close(); connection.disconnect();
            return new String(bytes.toByteArray(),charset);
        } catch(Exception e){last=e;if(i<retries)Thread.sleep(i*1000L);}
        throw last;
    }
    private static String enc(String s){try{return URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new IllegalStateException(e);}}
    private static String fmt(LocalDate d){return d.toString().replace("-","");}
    private static double d(String s){return Double.parseDouble(s);}
}
