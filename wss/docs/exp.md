# 如何跑Benchmark测试？
1. 写测试脚本
- 按照`benchmark_scripts/msr_ccf_clock.sh`这样的模板修改，最主要是注意参数部分，不同方法只需要控制影响自己的那部分参数

需要特别注意：
1）一定要注意参数，每个方法，每个数据集参数都要调整，具体参数设置，看excel实验表
2）保存结果的文件夹命名规范点，每个实验的结果单独放在一个文件夹下，后面方便汇总检查

2. 跑脚本

```bash
bash ./benchmark_scripts/msr_ccf_clock.sh
```

3. 查看结果

```bash
chmod a+x ./benchmark_scripts/parse-log.sh

./benchmark_scripts/parse-log.sh `ls -tr /datasets/benchmarks/mbf/accuracy/twitter/*.log`
```

输出6列，分别是Memory，ARE(Byte),FinalARE(PageHit),FPR(Byte),FNR(Byte),ER(Byte)
```csv
1024kb,0.5467%,0.4354%,0.2501%,0.0007%,0.2507%
1024kb,0.5668%,0.8721%,0.5003%,0.0007%,0.5010%
1024kb,0.9237%,0.8721%,0.5003%,0.0007%,0.5010%
1024kb,0.8303%,0.8721%,0.5003%,0.0007%,0.5010%
```

把结果复制到excel里面，可以自动分列，然后填充到最终结果位置
