package com.jundolc.ashare;

import java.time.LocalDate;

final class Model {
    private Model() {}

    record Quote(String code, String name, double price, double open,
                 double previousClose, double volumeShares, LocalDate date) {}

    record Bar(LocalDate date, double open, double close, double volumeShares) {}

    record Rules(double volumeRatioMax, boolean maOrder, int ma5RisingDays) {}

    record Match(String code, String name, double price, double changePercent,
                 long currentVolume, long average5Volume, long volumeLimit,
                 double volumeRatio, double ma5, double ma10, double ma20) {}
}

