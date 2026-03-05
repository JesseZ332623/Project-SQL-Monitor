# Redis Stream 概念和命令详述

## 主要概念

- Redis Stream 可以看作 Kafka 的 Topic + Partition + Consumer Group 的简化版，
  但是更加轻量。
- 
### Stream

一个有序的、仅仅末尾追加的的日志（append-only log）， 
每条消息都有唯一 ID（时间戳 + 序号，例如 1772501760184-75），越大越新。

### 消费者组 

同一个 Stream 内可以有多个消费者组，每个组独立记录自己消费到哪里。

### 消费者

一个组内的具体消费实例（多个消费者可以并行的消费同一个组）。

### Pending Entries List（PEL）

已经投递但尚未 ACK 的消息清单，是实现 “至少一次” 语义的关键。


## 操作命令详述

### `XINFO GROUPS <stream>`

返回指定的 stream 上所有消费者组的信息列表，示例如下：

```txt
XINFO GROUPS ids:buffer:global
1) 1) "name"
   2) "id-consumers"
   3) "consumers"
   4) "0"
   5) "pending"
   6) "0"
   7) "last-delivered-id"
   8) "0-0"
   9) "entries-read"
   10) "null"
   11) "lag"
   12) "200"
```

从返回的 INFO 来看，目前 Stream ids:buffer:global 只有一个消费者组，具体解释如下：

- "name"      -> "id-consumers" 消费者组的名字
- "consumers" -> "0"            该消费者组存活的消费者数量（即调用过 XREADGROUP 的消费者）
- "pending"   -> "0"            该消费者的组 PEL 长度（即已经投递但尚未 ACK 的消息数）
- "last-delivered-id" -> "0-0"  组内最后一次成功投递给任意消费者的消息 ID（不一定被 ACK）
- "entries-read" -> "null"      表示组已经“应该读取”到的消息条数（用于辅助计算 lag）
- "lag" -> "200"                积压量（即 Stream 中还有多少条消息尚未投递给消费者组）

重点理解以下 3 个属性：

1. last-delivered-id 
   
   - 消费者组的消费位点，类似于 Kafka 的 committed offset，但粒度更粗
   - 下一次 `XREADGROUP ... >` 执行就会从这个 ID 之后开始读取
   - 只有真正调用 `XREADGROUP` 并拿到消息，这个值才会前进

2. entries-read（Redis 7.0+）

   - 试图记录 “本消费者组读过多少条消息” 的计数器，
     理想情况下约等于 XLEN - lag 的值
   - 如果有中间消息被 `XDEL` 删除，或者 Stream 被 `XTRIM` 裁剪，这个计数器和真实值会不一致

3.  lag

    - 用于判断当前是否积压的指标由 Redis 内部维护，
      公式为 XLEN - entries-read 的近似值，不同的值反应的情况不同，如下所示：
      
      - lag = 0    消费完全跟得上，无积压
      - lag > 0    有那么多条消息还没有被这个组消费到（通常是消费者慢或者干脆挂掉）
      - lag = null 无法可信的计算，通常是因为
        - Stream 中间有消息被 `XDEL` 删除
        - Stream 被 `MAXXLEN` 或者 `MAXLEN` 裁剪过旧消息
        - 消费者组中 last-delivered-id 对应的消息不存在

### `XADD <stream> [MAXLEN] ~ <max-length> * <field> <value>`

往 Stream 的末尾写入一条消息 

   - `[MAXLEN] ~ <max-length>` 表示自动裁剪掉超过 max-length 的消息，
   - `*` 表示自动生成唯一 ID

### `XTRIM key MAXLEN | MINID [= | ~] threshold [LIMIT count]`

裁剪 Stream，限制其长度 MAXLEN 或者最小 ID MINID

  - `[= | ~] threshold` 表示精确 / 大致的裁剪到临界点
  - `LIMIT count]`      限制裁剪数量

### `XLEN <stream>` 

获取指定 Stream 的消息数量

### `XRANGE <stream> <start> <end> [COUNT count]` 

读取指定 ID 范围内的消息

  - `<start> <end>` 指定了 ID 范围，
    比如 1772505999247-0 1772505999247-+ 表示 10:46:39 3 Mar 2026 范围内的所有消息
  - `[COUNT count]` 用于限制读取的条数

### `XREVRANGE key end start [COUNT count]`

倒序的读取 ID 范围内的消息，一般用于显示最新的 COUNT 条消息

### `XREAD [COUNT count] [BLOCK milliseconds] STREAMS key [key ...] id [id ...]`

普通读取（非消费者），支持阻塞

### `XGROUP CREATE key group id | $ [MKSTREAM] [ENTRIESREAD entries-read]`

创建消费者组

  - `[MKSTREAM]` 表示如果 key 不存在则新建
  - `$ / 0-0`    指示起始位点
  - `[ENTRIESREAD entries-read]` 指定起始位点

### `XGROUP SETID key group <id | $> [ENTRIESREAD entries-read]`

修改消费者组的 last-delivered-id（用于跳过或者回溯，慎用）

### `XGROUP DESTROY key group`

删除整个消费者组

### `XREADGROUP GROUP group consumer [COUNT count] [BLOCK milliseconds] [CLAIM min-idle-time] [NOACK] STREAMS key [key ...] id [id ...]`

消费组读取消息

### `XACK key group id [id ...]`

确认组中的所有消息（从 PEL 列表中移除）

### `XPENDING key group [[IDLE min-idle-time] start end count [consumer]]`

查看 PEL（待确认消息）

### `XCLAIM key group consumer min-idle-time id [id ...] [IDLE ms] [TIME unix-time-milliseconds] [RETRYCOUNT count] [FORCE] [JUSTID] [LASTID lastid]`

抢/转移 pending 消息

### `XAUTOCLAIM key group consumer min-idle-time start [COUNT count] [JUSTID]`

自动扫描并认领过期的 pending 消息

### `XDEL key id [id ...]`

删除指定 stream 中指定 id 的消息

### `XSETID key last-id [ENTRIESADDED entries-added] [MAXDELETEDID max-deleted-id]`

强制修改 Stream 的首个 entry ID（非常危险，生产环境禁用）