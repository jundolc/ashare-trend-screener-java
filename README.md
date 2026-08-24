# A股上涨趋势缩量回调筛选器（Java版）

这是 Python 版本的独立 Java 8 实现，不依赖 Maven、Gradle或第三方库。

## 运行

```bash
./run.sh
```

macOS 也可以双击 `run.command`。首次运行会编译到 `build/`，结果写入 `output/screen_YYYY-MM-DD.csv`，失败请求写入 `output/errors_YYYY-MM-DD.csv`。

默认剔除 `688/689` 科创板。上涨趋势使用完整交易日判断：上一交易日 `MA10 > MA20`，上一交易日 MA20 高于3个交易日前的MA20，最近5个完整交易日至少3天收盘站上各自MA10；同时要求盘中现价在动态MA5和MA10上方、当日下跌且阴线、截至运行时成交量不超过此前5个完整交易日平均量的50%。CSV按涨跌幅从高到低排序，相关参数位于 `config.properties`。

实时行情默认东方财富、失败后切换新浪；历史行情默认东方财富、失败后切换腾讯。所有成交量统一为“股”。本工具不自动交易，盘中结果可能在收盘前变化，买入前请使用券商行情复核。

## 测试

```bash
./run-tests.sh
```

## 14:50定时运行

```cron
50 14 * * 1-5 cd /你的绝对路径/ashare-trend-screener-java && ./run.sh >> output/run.log 2>&1
```
