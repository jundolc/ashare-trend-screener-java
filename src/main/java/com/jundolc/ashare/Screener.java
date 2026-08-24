package com.jundolc.ashare;

import java.util.*;
import java.util.stream.Collectors;
import static com.jundolc.ashare.Model.*;

final class Screener {
    Optional<Match> evaluate(List<Bar> input, Quote q, Rules rules) {
        List<Bar> history=input.stream().filter(b -> b.date().isBefore(q.date()))
                .sorted(Comparator.comparing(Bar::date)).collect(Collectors.toList());
        if(history.size()<20 || q.price()<=0 || q.open()<=0 || q.previousClose()<=0 || q.volumeShares()<0) return Optional.empty();
        List<Double> closes=new ArrayList<>(); for(Bar b:history) closes.add(b.close()); closes.add(q.price());
        List<Double> ma5=rolling(closes,5); double m5=last(ma5), m10=last(rolling(closes,10)), m20=last(rolling(closes,20));
        double avg=history.subList(history.size()-5,history.size()).stream().mapToDouble(Bar::volumeShares).average().orElse(0);
        double ratio=avg>0 ? q.volumeShares()/avg : Double.POSITIVE_INFINITY;
        boolean rising=true;
        if(rules.ma5RisingDays()>0){
            if(ma5.size()<rules.ma5RisingDays()+1) rising=false;
            else for(int i=ma5.size()-rules.ma5RisingDays();i<ma5.size();i++) if(!(ma5.get(i)>ma5.get(i-1))) rising=false;
        }
        boolean ordered=!rules.maOrder() || m5>m10 && m10>m20;
        if(!(q.price()<q.previousClose() && q.price()<q.open() && q.price()>m5 && q.price()>m10 && ordered && rising && ratio<=rules.volumeRatioMax())) return Optional.empty();
        return Optional.of(new Match(q.code(),q.name(),q.price(),(q.price()/q.previousClose()-1)*100,
                Math.round(q.volumeShares()),Math.round(avg),Math.round(avg*rules.volumeRatioMax()),ratio,m5,m10,m20));
    }
    private List<Double> rolling(List<Double> v,int n){ List<Double> out=new ArrayList<>(); double sum=0;
        for(int i=0;i<v.size();i++){sum+=v.get(i);if(i>=n)sum-=v.get(i-n);if(i>=n-1)out.add(sum/n);} return out; }
    private double last(List<Double> x){return x.get(x.size()-1);}
}
