package com.jundolc.ashare;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import static com.jundolc.ashare.Model.*;

public final class Main {
    public static void main(String[] args){try{System.exit(run(args));}catch(Exception e){System.err.println("ERROR 无法开始筛选："+e.getMessage());System.exit(1);}}
    static int run(String[] args)throws Exception{
        Path config=Paths.get(args.length>1&&args[0].equals("--config")?args[1]:"config.properties");Properties p=new Properties();try(Reader r=Files.newBufferedReader(config,StandardCharsets.UTF_8)){p.load(r);}
        int retries=i(p,"retries",3),workers=i(p,"workers",8),days=i(p,"history.days",60);MarketClient client=new MarketClient(retries);
        List<Quote> raw=client.spot();Set<String> prefixes=new HashSet<>(Arrays.asList(p.getProperty("exclude.prefixes","688,689").split(",")));boolean excludeSt=b(p,"exclude.st",false);
        int board=(int)raw.stream().filter(q->prefixes.stream().anyMatch(q.code()::startsWith)).count();
        List<Quote> afterBoard=raw.stream().filter(q->prefixes.stream().noneMatch(q.code()::startsWith)).collect(Collectors.toList());int st=excludeSt?(int)afterBoard.stream().filter(q->q.name().toUpperCase().contains("ST")).count():0;
        List<Quote> valid=afterBoard.stream().filter(q->!excludeSt||!q.name().toUpperCase().contains("ST")).filter(q->q.price()>0&&q.open()>0&&q.previousClose()>0&&q.volumeShares()>=0).collect(Collectors.toList());int invalid=afterBoard.size()-st-valid.size();
        List<Quote> candidates=valid.stream().filter(q->q.price()<q.previousClose()&&q.price()<q.open()).collect(Collectors.toList());
        System.out.printf("INFO 原始行情 %d 只；排除科创板 %d 只、ST %d 只、无效行情 %d 只；有效股票 %d 只；候选 %d 只%n",raw.size(),board,st,invalid,valid.size(),candidates.size());
        Rules rules=new Rules(d(p,"volume.ratio.max",.5),i(p,"trend.ma20.rising.lookback",3),i(p,"trend.recent.days",5),i(p,"trend.min.closes.above.ma10",3));Screener screener=new Screener();List<Match> matches=Collections.synchronizedList(new ArrayList<>());List<String[]> errors=Collections.synchronizedList(new ArrayList<>());AtomicInteger done=new AtomicInteger();
        ExecutorService pool=Executors.newFixedThreadPool(workers);LocalDate today=LocalDate.now(),start=today.minusDays(days);
        for(Quote q:candidates)pool.submit(()->{try{screener.evaluate(client.history(q.code(),start,today),q,rules).ifPresent(matches::add);}catch(Exception e){errors.add(new String[]{q.code(),q.name(),e.getClass().getSimpleName()+": "+e.getMessage()});}int n=done.incrementAndGet();if(n%100==0)System.out.printf("INFO 历史行情进度 %d/%d，命中 %d，失败 %d%n",n,candidates.size(),matches.size(),errors.size());});
        pool.shutdown();pool.awaitTermination(2,TimeUnit.HOURS);matches.sort(Comparator.comparingDouble(Match::changePercent).reversed());
        Path out=Paths.get(p.getProperty("output.dir","output"));Files.createDirectories(out);String stamp=today.toString();writeMatches(out.resolve("screen_"+stamp+".csv"),matches);writeErrors(out.resolve("errors_"+stamp+".csv"),errors);
        matches.forEach(m->System.out.printf("%s %-10s %7.2f%% 量比 %.3f%n",m.code(),m.name(),m.changePercent(),m.volumeRatio()));System.out.printf("结果：%d 只；失败：%d 只；目录：%s%n",matches.size(),errors.size(),out.toAbsolutePath());return errors.isEmpty()?0:2;
    }
    private static void writeMatches(Path f,List<Match> rows)throws IOException{try(BufferedWriter w=Files.newBufferedWriter(f,StandardCharsets.UTF_8)){w.write("\ufeff代码,名称,现价,涨跌幅%,截至运行时成交量(股),前5日平均成交量(股),50%成交量上限(股),量比(前5日均量),MA5,MA10,MA20\n");for(Match m:rows)w.write(String.format(Locale.ROOT,"%s,%s,%.3f,%.2f,%d,%d,%d,%.3f,%.3f,%.3f,%.3f%n",m.code(),csv(m.name()),m.price(),m.changePercent(),m.currentVolume(),m.average5Volume(),m.volumeLimit(),m.volumeRatio(),m.ma5(),m.ma10(),m.ma20()));}}
    private static void writeErrors(Path f,List<String[]> rows)throws IOException{try(BufferedWriter w=Files.newBufferedWriter(f,StandardCharsets.UTF_8)){w.write("\ufeff代码,名称,错误\n");for(String[]r:rows)w.write(r[0]+","+csv(r[1])+","+csv(r[2])+"\n");}}
    private static String csv(String s){return "\""+s.replace("\"","\"\"")+"\"";}private static int i(Properties p,String k,int v){return Integer.parseInt(p.getProperty(k,""+v));}private static double d(Properties p,String k,double v){return Double.parseDouble(p.getProperty(k,""+v));}private static boolean b(Properties p,String k,boolean v){return Boolean.parseBoolean(p.getProperty(k,""+v));}
}
