package com.jundolc.ashare;

/** Optional live, read-only connectivity check; not run by run-tests.sh. */
public final class DataSourceSmoke {
    public static void main(String[] args) throws Exception {
        var quotes = new MarketClient(1).spot();
        if (quotes.size() < 4_000) throw new AssertionError("全市场数量异常: " + quotes.size());
        var q = quotes.get(0);
        System.out.printf("实时行情检查通过：%d 只；样例 %s %s%n", quotes.size(), q.code(), q.name());
    }
}
