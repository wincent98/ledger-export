# ledger-export

账本导出服务。用 keyset 分页游标遍历全表，把记录分批交给下游对账。

## 结构

| 类 | 说明 |
|----|------|
| `Record` | 一行账本记录，按 (updatedAtNanos, id) 排序，这一对是唯一的 |
| `Cursor` | 分页续传点，序列化成不透明 token 交给下游在批次之间持有 |
| `RecordTable` | 目标表的内存替身，`page(cursor, limit)` 返回游标之后的若干行 |
| `Exporter` | 驱动扫描，每次返回一批记录和下一批的 token |
| `ExportApp` | 跑一次完整导出并和表内容比对 |

## 构建与运行

```bash
mvn -o package
java -jar target/ledger-export-1.6.0.jar        # 默认 batchSize=5
java -jar target/ledger-export-1.6.0.jar 8      # 指定 batchSize
mvn -o test
```

退出码 0 表示每行恰好导出一次，1 表示有漏或有重。

## 运维记录

- 2026-09-14 起下游对账反馈导出结果对不上，有记录没出现在导出里，也有记录出现两次。
  写入越密集越容易发生，日志无异常。
