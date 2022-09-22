# 多Scope的工作集大小估计测试方案

之前的测试是假设所有数据属于同一个scope，所以是单scope的测试。

假设缓存数据来源是不同的scope，这就是多scope的测试。

## 数据集

MSR包含了来自13个服务器，总共36个卷的trace。

我们分别挑选其中4、8、16、32个卷的trace作为MSR-4，MSR-8，MSR-16和MSR-32数据集。

每一行record格式：
```
Timestamp, Scope, Offset, Size
```
其中，Timestamp、Offset和Size都来自于原本数据集，
Scope是来源于数据集名字，如来自prxy_0数据集的数据，scope就是`msr.prxy.0`。

注意处理数据集时要按照Timestamp排序。


## 测试代码

1. 按照上面数据集格式定义一个新的`EntryGenerator`，读取上述格式的数据集

2. CCF要设置SCOPE_BITS为恰好能够装下所有scope的大小，如MSR-4对应设置scope_bits为2；
其他方法都是一个scope对应一个shadowcache（在测试代码里判断一个scope，然后装入对应scope,直接在分配内存的时候除以4），每个shadowcache大小相等，所有方法中内存大小相等

3. 写一个多Scope的测试代码，类似于TimeBasedAccuracyBenchmark，不过分scope各自统计每个scope的各种指标

4. 最后实验结果是能够得到一个表格，记录每个方法在每个scope上估计的准确性。

