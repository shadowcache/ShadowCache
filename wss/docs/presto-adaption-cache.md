# 在Presto上的TPC-DS查询缓存自适应实验

## 前提条件
- Docker
- Docker Compose
- Java 1.80
- maven 3.54
- Git

## 步骤

### 注意事项

- 默认下述各个git仓库都在同一路径下，比如：

```
./git_repo
./git_repo/alluxio
./git_repo/presto
./git_repo/presto-iceberg-bundle
```

### 编译Alluxio

1. clone Alluxio并切换到实验分支

```bash
git clone git@git.nju.edu.cn:iluoeli/alluxio.git
git check presto-clockcuckoo-adaption-fix
```

2. 编译Allxuio

```bash
mvn clean install -Dmaven.javadoc.skip=true -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -Prelease
```

### 编译Presto

1. clone Presto并切换到实验分支

```bash
git clone git@git.nju.edu.cn:iluoeli/presto.git
git checkout shadowcache
```

2. 编译Presto

`mvnw`这个工具需自行谷歌下载

```bash
./mvnw clean install -T2C -o -DskipTests -Dlicense.skip=true -Dcheckstyle.skip=true -Dfindbugs.skip=true -pl '!presto-docs'
```

如果Presto已经编译过，仅需要替换Alluxio版本时，也可直接用下面命令替换jar包，避免重新编译Presto
```bash
find . -name "alluxio-shaded-client-2.7.0-SNAPSHOT.jar" -exec cp ../alluxio/shaded/client/target/alluxio-shaded-client-2.7.0-SNAPSHOT.jar {} \;
```

### 运行Presto

1. clone测试相关的git仓库

```bash
git clone git@git.nju.edu.cn:iluoeli/presto-iceberg-bundle.git
```

2. 启动Presto

进入该git仓库，并启动Presto环境

```bash
# 启动
./docker_compose/multinode-presto/compose.sh up

# 或者使用下述方式
source ./docker_compose/base/base.sh ./docker_compose/multinode-presto/compose.sh
docker-compose -f docker_compose/base/docker-compose.yml -f docker_compose/multinode-presto/docker-compose.yml up
```

检查各个容器是否正常启动：

```bash
source ./docker_compose/base/base.sh ./docker_compose/multinode-presto/compose.sh
docker-compose -f docker_compose/base/docker-compose.yml -f docker_compose/multinode-presto/docker-compose.yml ps
```

应该有presto、hbase和prometheus的容器在运行

检查日志：

```bash
docker-compose -f docker_compose/base/docker-compose.yml -f docker_compose/multinode-presto/docker-compose.ym logs | less
```

### 配置Prometheus

1. 配置端口转发

下面的`192.168.100.10`需要是你可直接访问的内网IP地址，即堡垒机的内网IP地址

```bash
# prometheus
socat tcp-listen:17323,reuseaddr,fork tcp:192.168.100.102:9090

# presto
socat tcp-listen:17324,reuseaddr,fork tcp:192.168.100.102:8080
```

2. 确认presto和Prometheus的web界面可访问

下面的IP地址换成堡垒机的外网IP地址，即你在自己电脑上能ping通的地址

```bash
# presto
http://210.28.132.13:17324/

# prometheus
http://210.28.132.13:17323/
```

### 实验测试

1. 加载TPC-DS测试数据

在`presto-iceberg-bundle`项目根路径下执行：

```bash
# PRESTO为编译好的presto jar包位置
PRESTO="../presto/presto-cli/target/presto-cli-0.266-SNAPSHOT-executable.jar"

# 加载数据
${PRESTO} -f create_from_tpcds_10.sql

# 检查加载是否成功，应该包含了TPC-DS的数据表
${PRESTO} --catalog hive --schema tpcds10 --execute "show tables;"
```

2. 实验测试

```bash
# 清理缓存
docker-compose -f docker_compose/base/docker-compose.yml -f docker_compose/multinode-presto/docker-compose.yml exec presto-worker rm -rf /tmp/alluxio/LOCAL

# 启动测试
bash test_cache_adaption_q1_4.sh
```

3. Web界面查看实验结果

进入Prometheus Web界面`http://210.28.132.13:17323/`，
然后检查需要的metrics：

```bash
# metrics
## ShadowCacheHR
com_facebook_alluxio_Client_CacheShadowCacheBytesHit_presto_worker_Count/com_facebook_alluxio_Client_CacheShadowCacheBytesRead_presto_worker_Count

## RealCacheHR
com_facebook_alluxio_Client_CacheBytesReadCache_presto_worker_Count/(com_facebook_alluxio_Client_CacheBytesReadCache_presto_worker_Count + com_facebook_alluxio_Client_CacheBytesReadExternal_presto_worker_Count)
```

3. 导出实验结果

注意:
- 把下面命令里面的ip地址换成堡垒机的外部IP地址，即你验证过的可访问的Prometheus的IP地址和端口
- 把起止时间`start=1645059552.6836703&end=1645060370.9545844`换成你需要的时间段
- `step=1`越大，得到的曲线越平滑

```bash
ShadowCache命中率
curl "http://210.28.132.13:17323/api/v1/query_range?query=com_facebook_alluxio_Client_CacheShadowCacheBytesHit_presto_worker_Count/com_facebook_alluxio_Client_CacheShadowCacheBytesRead_presto_worker_Count&start=1645059552.6836703&end=1645060370.9545844&step=1" > /mnt/g/git_repo/plot-wss/data2/cache_adaption/ad-shr.json

LocalCache命中率
curl "http://210.28.132.13:17323/api/v1/query_range?query=com_facebook_alluxio_Client_CacheBytesReadCache_presto_worker_Count%2F%28com_facebook_alluxio_Client_CacheBytesReadCache_presto_worker_Count+%2B+com_facebook_alluxio_Client_CacheBytesRequestedExternal_presto_worker_Count%29&start=1645059552.6836703&end=1645060370.9545844&step=1"

ShadowCache空间大小
curl "http://210.28.132.13:17323/api/v1/query_range?query=com_facebook_alluxio_Client_CacheShadowCacheBytes_presto_worker_Count&start=1645059552.6836703&end=1645060370.9545844&step=1" > /mnt/g/git_repo/plot-wss/data2/cache_adaption/ad-sbytes.json

真实缓存空间大小
curl "http://210.28.132.13:17323/api/v1/query_range?query=com_facebook_alluxio_Client_CacheSpaceUsed_presto_worker_Value%2B+com_facebook_alluxio_Client_CacheSpaceAvailable_presto_worker_Value&start=1645059552.6836703&end=1645060370.9545844&step=1" > /mnt/g/git_repo/plot-wss/data2/cache_adaption/ad-rbytes.json
```
