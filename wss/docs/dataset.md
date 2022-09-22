# Datasets

## 1. MSR

### Download

[http://iotta.snia.org/traces/block-io/388](http://iotta.snia.org/traces/block-io/388)

### NOTE

- `Offset`作为对象的`key`， `Size`作为对象大小
- 因为是Block I/O的trace，`Size`总是512的倍数
- 相同`Offset`的对象，`Size`也是会变的，如下：
```csv
128172299571726563,prxy,0,Write,1105182720,512,2072
128172300283436651,prxy,0,Write,1105182720,1024,1624
128172301193894088,prxy,0,Write,1105182720,1024,1284
128172301957790536,prxy,0,Write,1105182720,1024,1308
128172302508563491,prxy,0,Write,1105182720,4608,2552
```

## 2. Twitter

### Download

https://github.com/twitter/cache-trace#anonymized-cache-request-traces-from-twitter-production

### NOTE

- 每个对象原本的`Key`作为`key`， 对象大小与`key`大小之和作为`Size`
- 在`get`失败时，对象大小会变成0这个需要提前处理一下trace，让get的大小是成功时的大小

```csv
0,Dk-qDeZhMTD-qDUDHUb-q55h-l.F_CUYJ4eD02CeFD2JeYs4Jeuu0kDsYl4f0O0UfFkFU,69,0,376,get,0
0,Dk-qDeZhMTD-qDUDHUb-q55h-l.F_CUYJ4eD02CeFD2JeYs4Jeuu0kDsYl4f0O0UfFkFU,69,19427,376,set,20
```

## 3. YCSB

Follow: [https://github.com/iluoeli/YCSB/tree/dump-trace/dummydb](https://github.com/iluoeli/YCSB/tree/dump-trace/dummydb)
