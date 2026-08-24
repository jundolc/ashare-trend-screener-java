package com.jundolc.ashare;

import java.util.*;
import java.util.stream.Collectors;
import static com.jundolc.ashare.Model.*;

final class Screener {
    Optional<Match> evaluate(List<Bar> input, Quote q, Rules rules) {
        List<Bar> history=input.stream().filter(b -> b.date().isBefore(q.date()))
                .sorted(Comparator.comparing(Bar::date)).collect(Collectors.toList());
        if(history.size()<20+rules.ma20RisingLookback() || q.price()<=0 || q.open()<=0 || q.previousClose()<=0 || q.volumeShares()<0) return Optional.empty();
        List<Double> completed=new ArrayList<>(); for(Bar b:history) completed.add(b.close());
        int last=completed.size()-1;
        double yesterdayMa10=averageAt(completed,last,10),yesterdayMa20=averageAt(completed,last,20);
        double earlierMa20=averageAt(completed,last-rules.ma20RisingLookback(),20);
        int closesAbove=0;for(int n=last-rules.recentDays()+1;n<=last;n++)if(completed.get(n)>averageAt(completed,n,10))closesAbove++;
        boolean completedTrend=yesterdayMa10>yesterdayMa20&&yesterdayMa20>earlierMa20&&closesAbove>=rules.minClosesAboveMa10();
        List<Double> closes=new ArrayList<>(completed); closes.add(q.price());
        double m5=averageAt(closes,closes.size()-1,5),m10=averageAt(closes,closes.size()-1,10),m20=averageAt(closes,closes.size()-1,20);
        double avg=history.subList(history.size()-5,history.size()).stream().mapToDouble(Bar::volumeShares).average().orElse(0);
        double ratio=avg>0 ? q.volumeShares()/avg : Double.POSITIVE_INFINITY;
        if(!(q.price()<q.previousClose() && q.price()<q.open() && q.price()>m5 && q.price()>m10 && completedTrend && ratio<=rules.volumeRatioMax())) return Optional.empty();
        return Optional.of(new Match(q.code(),q.name(),q.price(),(q.price()/q.previousClose()-1)*100,
                Math.round(q.volumeShares()),Math.round(avg),Math.round(avg*rules.volumeRatioMax()),ratio,m5,m10,m20));
    }
    private double averageAt(List<Double> values,int end,int length){double sum=0;for(int n=end-length+1;n<=end;n++)sum+=values.get(n);return sum/length;}
}
