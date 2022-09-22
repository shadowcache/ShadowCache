## 容器化部署测试

### 前提条件

- git
- docker

### 构造容器镜像

1. 拷贝git仓库

```bash
git clone git@git.nju.edu.cn:iluoeli/wss-estimation.git
```

2. 构建测试镜像

```bash
cd wss-estimation
docker build -t bench-cuki .
```

### 运行测试

1. 准备数据集

将数据集准备到`/datasets`路径下

2. 启动容器

```bash
docker run -it \
	-v /home/lighthouse/.m2:/root/.m2 \
	-v /datasets:/datasets \
	--entrypoint /bin/bash \
	--name test-cuki \
	bench-cuki
```

NOTE:
> 在宿主机的/datasets下存放实验所需的数据集，实验测试结果也会更新到此目录下

如果你只是想简单的测试脚本和代码是否能够跑通，本项目的`datasets-tiny`提供了小数据集，
可在项目根路径下执行如下路径挂载测试小数据集:

```bash
docker run -it \
        -v /home/lighthouse/.m2:/root/.m2 \
        -v $(pwd)/datasets-tiny:/datasets \
        --entrypoint /bin/bash \
        --name test-cuki \
        bench-cuki
```

3. 启动测试

参考`bin`和`benchmark_scripts`路径下的测试脚本。
如，进行Cuki的准确性测试，运行：

```bash
bash ./bin/accuracy_bench.sh
```
