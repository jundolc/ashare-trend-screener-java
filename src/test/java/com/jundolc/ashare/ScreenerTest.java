package com.jundolc.ashare;

import java.time.LocalDate;
import java.util.*;
import static com.jundolc.ashare.Model.*;

public final class ScreenerTest {
    public static void main(String[] args){
        List<Bar> h=new ArrayList<>();LocalDate start=LocalDate.of(2026,7,1);for(int n=0;n<25;n++)h.add(new Bar(start.plusDays(n),10+n*.1,10+n*.1,1_000_000));
        Quote q=new Quote("000001","测试",12.45,12.5,12.6,500_000,LocalDate.of(2026,8,24));
        Match m=new Screener().evaluate(h,q,new Rules(.5,true,1)).orElseThrow();
        assert m.currentVolume()==500_000; assert m.average5Volume()==1_000_000; assert m.volumeLimit()==500_000; assert m.volumeRatio()==.5;
        Quote tooMuch=new Quote("000001","测试",12.45,12.5,12.6,500_001,LocalDate.of(2026,8,24));assert new Screener().evaluate(h,tooMuch,new Rules(.5,true,1)).isEmpty();
        Quote notGreen=new Quote("000001","测试",12.5,12.5,12.6,500_000,LocalDate.of(2026,8,24));assert new Screener().evaluate(h,notGreen,new Rules(.5,true,1)).isEmpty();
        System.out.println("全部 Java 测试通过");
    }
}

