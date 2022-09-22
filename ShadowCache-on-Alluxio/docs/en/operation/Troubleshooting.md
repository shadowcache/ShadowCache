---
layout: global
title: Troubleshooting
nickname: Troubleshooting
group: Operations
priority: 14
---

* Table of Contents
{:toc}

This page is a collection of high-level guides and tips regarding how to diagnose issues encountered in
Alluxio.

> Note: this doc is not intended to be the full list of Alluxio questions.
Join the [Alluxio community Slack Channel](https://www.alluxio.io/slack) to chat with users and
developers, or post questions on the [Alluxio Mailing List](https://groups.google.com/forum/#!forum/alluxio-users).

## Where are the Alluxio logs?

Alluxio generates Master, Worker and Client logs under the dir `${ALLUXIO_HOME}/logs`. They are
named as `master.log`, `master.out`, `worker.log`, `worker.out`, `job_master.log`, `job_master.out`, 
`job_worker.log`, `job_worker.out` and `user/user_${USER}.log`. Files
suffixed with `.log` are generated by log4j; File suffixed with `.out` are generated by redirection of
stdout and stderr of the corresponding process.

The master and worker logs are useful to understand what the Alluxio Master and
Workers are doing, especially when running into any issues. If you do not understand the error messages,
search for them in the [Mailing List](https://groups.google.com/forum/#!forum/alluxio-users),
in the case the problem has been discussed before. 
You can also join our [Slack channel](https://slackin.alluxio.io/) and seek help there.
You can find more details about the Alluxio server logs [here]({{ '/en/operation/Basic-Logging.html#server-logs' | relativize_url }}).

The client-side logs are also helpful when Alluxio service is running but the client cannot connect to the servers.
Alluxio client emits logging messages through log4j, so the location of the logs is determined by the client side
log4j configuration used by the application.
You can find more details about the client-side logs [here]({{ '/en/operation/Basic-Logging.html#application-logs' | relativize_url }}).

The user logs in `${ALLUXIO_HOME}/logs/user/` are the logs from running Alluxio shell.
Each user will have separate log files.

For more information about logging, please check out
[this page]({{ '/en/operation/Basic-Logging.html' | relativize_url }}).

## Alluxio remote debug

Java remote debugging makes it easier to debug Alluxio at the source level without modifying any code. You
will need to append the JVM remote debugging parameters and start a debugging server. There are several ways to append
the remote debugging parameters; you can export the following configuration properties in shell or `alluxio-env.sh`:

```bash
export ALLUXIO_WORKER_JAVA_OPTS="$ALLUXIO_JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6606"
export ALLUXIO_MASTER_JAVA_OPTS="$ALLUXIO_JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6607"
export ALLUXIO_USER_DEBUG_JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=6609"
```

If you want to debug shell commands, you can add the `-debug` flag to start a debug server with the JVM debug
parameters `ALLUXIO_USER_DEBUG_JAVA_OPTS`, such as `alluxio fs -debug ls /`.

`suspend = y/n` will decide whether the JVM process wait until the debugger connects. If you want to debug with the
shell command, set the `suspend = y`. Otherwise, you can set `suspend = n` to avoid unnecessary waiting time.

After starting the master or worker, use Eclipse, IntelliJ IDEA, or another java IDE, create a new java remote configuration,
set the debug server's host and port, and start the debug session. If you set a breakpoint which can be reached, the IDE
will enter debug mode and you can inspect the current context's variables, call stack, thread list, and expression
evaluation.

## Alluxio collectInfo command

Alluxio has a `collectInfo` command that collect information to troubleshoot an Alluxio cluster.
`collectInfo` will run a set of sub-commands that each collects one aspect of system information, as explained below.
In the end the collected information will be bundled into one tarball which contains a lot of information regarding your Alluxio cluster.
The tarball size mostly depends on your cluster size and how much information you are collecting.
For example, `collectLog` operation can be costly if you have huge amounts of logs. Other commands
typically do not generate files larger than 1MB. The information in the tarball will help you troubleshoot your cluster.
Or you can share the tarball with someone you trust to help troubleshoot your Alluxio cluster.

The `collectInfo` command will SSH to each node and execute the set of sub-commands.
In the end of execution the collected information will be written to files and tarballed.
Each individual tarball will be collected to the issuing node.
Then all the tarballs will be bundled into the final tarball, which contains all information about the Alluxio cluster.

> NOTE: Be careful if your configuration contains credentials like AWS keys!
You should ALWAYS CHECK what is in the tarball and REMOVE the sensitive information from the tarball before sharing it with someone!

### Collect Alluxio cluster information
`collectAlluxioInfo` will run a set of Alluxio commands that collect information about the Alluxio cluster, like `bin/alluxio fsadmin report` etc.
When the Alluxio cluster is not running, this command will fail to collect some information.
This sub-command will run both `alluxio getConf` which collects local configuration properties, 
and `alluxio getConf --master --source` which prints configuration properties that are received from the master.
Both of them mask credential properties. The difference is the latter command fails if the Alluxio cluster is not up. 

### Collect Alluxio configuration files
`collectConfig` will collect all the configuration files under `${alluxio.work.dir}/conf`.
From Alluxio 2.4, the `alluxio-site.properties` file will not be copied,
as many users tend to put their plaintext credentials to the UFS in this file.
Instead, the `collectAlluxioInfo` will run a `alluxio getConf` command
which prints all the configuration properties, with the credential fields masked.
The [getConf command]({{ '/en/operation/User-CLI.html#getconf' | relativize_url }}) will collect all the current node configuration.

So in order to collect Alluxio configuration in the tarball,
please make sure `collectAlluxioInfo` sub-command is run.

> WARNING: If you put credential fields in the configuration files except alluxio-site.properties (eg. `alluxio-env.sh`), 
DO NOT share the collected tarball with anybody unless you have manually obfuscated them in the tarball!

### Collect Alluxio logs
`collectLog` will collect all the logs under `${alluxio.work.dir}/logs`.

> NOTE: Roughly estimate how much log you are collecting before executing this command!

### Collect Alluxio metrics
`collectMetrics` will collect Alluxio metrics served at `http://${alluxio.master.hostname}:${alluxio.master.web.port}/metrics/json/` by default.
The metrics will be collected multiple times to see the progress.

### Collect JVM information
`collectJvmInfo` will collect information about the existing JVMs on each node.
This is done by running a `jps` command then `jstack` on each found JVM process.
This will be done multiple times to see if the JVMs are making progress.

### Collect system information
`collectEnv` will run a set of bash commands to collect information about the running node.
This runs system troubleshooting commands like `env`, `hostname`, `top`, `ps` etc.

> WARNING: If you stored credential fields in environment variables like AWS_ACCESS_KEY or in process start parameters
like -Daws.access.key=XXX, DO NOT share the collected tarball with anybody unless you have manually obfuscated them in the tarball!

### Collect all information mentioned above
`all` will run all the sub-commands above.

### Command options

The `collectInfo` command has the below options.

```console
$ bin/alluxio collectInfo 
    [--max-threads <threadNum>] 
    [--local] 
    [--help]
    [--additional-logs <filename-prefixes>] 
    [--exclude-logs <filename-prefixes>] 
    [--include-logs <filename-prefixes>] 
    [--start-time <datetime>] 
    [--end-time <datetime>]
    COMMAND <outputPath>
```

`<outputPath>` is the directory you want the final tarball to be written into.

Options:
1. `--max-threads threadNum` option configures how many threads to use for concurrently collecting information and transmitting tarballs.
When the cluster has a large number of nodes, or large log files, the network IO for transmitting tarballs can be significant.
Use this parameter to constrain the resource usage of this command.

1. `--local` option specifies the `collectInfo` command to run only on `localhost`.
That means the command will only collect information about the `localhost`. 
If your cluster does not have password-less SSH across nodes, you will need to run with `--local`
option locally on each node in the cluster, and manually gather all outputs.
If your cluster has password-less SSH across nodes, you can run without `--local` command,
which will essentially distribute the task to each node and gather the locally collected tarballs for you. 

1. `--help` option asks the command to print the help message and exit.

1. `--additional-logs <filename-prefixes>` specifies extra log file name prefixes to include.
By default, only log files recognized by Alluxio will be collected by the `collectInfo` command.
The recognized files include below:
```
logs/master.log*, 
logs/master.out*, 
logs/job_master.log*, 
logs/job_master.out*, 
logs/master_audit.log*, 
logs/worker.log*, 
logs/worker.out*, 
logs/job_worker.log*, 
logs/job_worker.out*, 
logs/proxy.log*, 
logs/proxy.out*, 
logs/task.log*, 
logs/task.out*, 
logs/user/*
```
Other than mentioned above, `--additional-logs <filename-prefixes>` specifies that files 
whose names start with the prefixes in `<filename-prefixes>` should be collected.
This will be checked after the exclusions defined in `--exclude-logs`.
`<filename-prefixes>`  specifies the filename prefixes, separated by commas.

1. `--exclude-logs <filename-prefixes>` specifies file name prefixes to ignore from the default list.

1. `--include-logs <filename-prefixes>` specifies only to collect files whose names start
with the specified prefixes, and ignore all the rest.
You CANNOT use `--include-logs` option together with either `--additional-logs` or
`--exclude-logs`, because it is ambiguous what you want to include.

1. `--end-time <datetime>` specifies a datetime after which the log files can be ignored.
A log file will be ignore if the file was created after this end time.
The first couple of lines of the log file will be parsed, in order to infer when the log
file started.
The `<datetime>` is a datetime string like `2020-06-27T11:58:53`.
The parsable datetime formats include below:
```
"2020-01-03 12:10:11,874"
"2020-01-03 12:10:11"
"2020-01-03 12:10"
"20/01/03 12:10:11"
"20/01/03 12:10"
2020-01-03T12:10:11.874+0800
2020-01-03T12:10:11
2020-01-03T12:10
```

1. `--start-time <datetime>` specifies a datetime before with the log files can be ignored.
A log file will be ignored if the last modified time is before this start time.

## Setup FAQ

### Q: I'm new to Alluxio and cannot set up Alluxio on my local machine. What should I do?

A: Check `${ALLUXIO_HOME}/logs` to see if there are any master or worker logs. Look for any errors
in these logs. Double check if you missed any configuration
steps in [Running-Alluxio-Locally]({{ '/en/deploy/Running-Alluxio-Locally.html' | relativize_url }}).

Typical issues:
- `ALLUXIO_MASTER_MOUNT_TABLE_ROOT_UFS` is not configured correctly.
- If running `ssh localhost` fails, make sure the public SSH key for the host is added in `~/.ssh/authorized_keys`.

### Q: I'm trying to deploy Alluxio in a cluster with Spark and HDFS. Are there any suggestions?

A: Please follow [Running-Alluxio-on-a-Cluster]({{ '/en/deploy/Running-Alluxio-On-a-Cluster.html' | relativize_url }}),
[Configuring-Alluxio-with-HDFS]({{ '/en/ufs/HDFS.html' | relativize_url }}),
and [Configuring-Spark-with-Alluxio]({{ '/en/compute/Spark.html' | relativize_url }}).

Tips:

- The best performance gains occur when Alluxio workers are co-located with the nodes of the computation frameworks.
- If the under storage is remote (like S3 or remote HDFS), using Alluxio can be especially beneficial.

### Q: What Java version should I use when I deploy Alluxio?

A: Alluxio requires Java 8 or 11 runtime to function properly.
You can find more details about the system requirements [here]({{ '/en/deploy/Requirements.html' | relativize_url }}).

## Usage FAQ

### Q: Why do I see exceptions like "No FileSystem for scheme: alluxio"?

A: This error message is seen when your applications (e.g., MapReduce, Spark) try to access
Alluxio as an HDFS-compatible file system, but the `alluxio://` scheme is not recognized by the
application. Please make sure your HDFS configuration file `core-site.xml` (in your default hadoop
installation or `spark/conf/` if you customize this file for Spark) has the following property:

```xml
<configuration>
  <property>
    <name>fs.alluxio.impl</name>
    <value>alluxio.hadoop.FileSystem</value>
  </property>
</configuration>
```

See the doc page for your specific compute framework for detailed setup instructions.

### Q: Why do I see exceptions like "java.lang.RuntimeException: java.lang.ClassNotFoundException: Class alluxio.hadoop.FileSystem not found"?

A: This error message is seen when your applications (e.g., MapReduce, Spark) try to access
Alluxio as an HDFS-compatible file system, the `alluxio://` scheme has been
configured correctly, but the Alluxio client jar is not found on the classpath of your application.
Depending on the computation frameworks, users usually need to add the Alluxio
client jar to their class path of the framework through environment variables or
properties on all nodes running this framework. Here are some examples:

- For MapReduce jobs, you can append the client jar to `$HADOOP_CLASSPATH`:

```console
$ export HADOOP_CLASSPATH={{site.ALLUXIO_CLIENT_JAR_PATH}}:${HADOOP_CLASSPATH}
```
See [MapReduce on Alluxio]({{ '/en/compute/Hadoop-MapReduce.html' | relativize_url }}) for more details.

- For Spark jobs, you can append the client jar to `$SPARK_CLASSPATH`:

```console
$ export SPARK_CLASSPATH={{site.ALLUXIO_CLIENT_JAR_PATH}}:${SPARK_CLASSPATH}
```
See [Spark on Alluxio]({{ '/en/compute/Spark.html' | relativize_url }}) for more details.

Alternatively, add the following lines to `spark/conf/spark-defaults.conf`:

```properties
spark.driver.extraClassPath {{site.ALLUXIO_CLIENT_JAR_PATH}}
spark.executor.extraClassPath {{site.ALLUXIO_CLIENT_JAR_PATH}}
```

- For Presto, put Alluxio client jar `{{site.ALLUXIO_CLIENT_JAR_PATH}}` into the directory
`${PRESTO_HOME}/plugin/hive-hadoop2/`
Since Presto has long running processes, ensure they are restarted after the jar has been added.
See [Presto on Alluxio]({{ '/en/compute/Presto.html' | relativize_url }}) for more details.

- For Hive, set `HIVE_AUX_JARS_PATH` in `conf/hive-env.sh`:

```console
$ export HIVE_AUX_JARS_PATH={{site.ALLUXIO_CLIENT_JAR_PATH}}:${HIVE_AUX_JARS_PATH}
```
Since Hive has long running processes, ensure they are restarted after the jar has been added.

If the corresponding classpath has been set but exceptions still exist, users can check
whether the path is valid by:

```console
$ ls {{site.ALLUXIO_CLIENT_JAR_PATH}}
```
See [Hive on Alluxio]({{ '/en/compute/Hive.html' | relativize_url }}) for more details.

### Q: I'm seeing error messages like "Frame size (67108864) larger than max length (16777216)". What is wrong?

A: This problem can be caused by different possible reasons.

- Please double check if the port of Alluxio master address is correct. The default listening port for Alluxio master is port 19998,
while a common mistake causing this error message is due to using a wrong port in master address (e.g., using port 19999 which is the default Web UI port for Alluxio master).
- Please ensure that the security settings of Alluxio client and master are consistent.
Alluxio provides different approaches to [authenticate]({{ '/en/operation/Security.html' | relativize_url }}#authentication) users by configuring `alluxio.security.authentication.type`.
This error happens if this property is configured with different values across servers and clients
(e.g., one uses the default value `NOSASL` while the other is customized to `SIMPLE`).
Please read [Configuration-Settings]({{ '/en/operation/Configuration.html' | relativize_url }}) for how to customize Alluxio clusters and applications.

### Q: I'm copying or writing data to Alluxio while seeing error messages like "Failed to cache: Not enough space to store block on worker". Why?

A: This error indicates insufficient space left on Alluxio workers to complete your write request.
This is either because the worker fails to evict enough space or the block size is too large to fit in any of the worker's storage directories.

- Check if you have any files unnecessarily pinned in memory and unpin them to release space.
See [Command-Line-Interface]({{ '/en/operation/User-CLI.html#pin' | relativize_url }}) for more details.
- Increase the capacity of workers by updating the
[worker tier storage configurations]({{ '/en/core-services/Caching.html#configuring-alluxio-storage' | relativize_url }}).

### Q: I'm writing a new file/directory to Alluxio and seeing journal errors in my application

A: First you should check if you are running Alluxio with UFS journal or Embedded journal.
See the difference [here]({{ '/en/operation/Journal.html#embedded-journal-vs-ufs-journal' | relativize_url }}).

Also you should verify that the journal you are using is compatible with the current configuration.
There are a few scenarios where the journal compatibility is not guaranteed and you need to either
[restore from a backup]({{ '/en/operation/Journal.html#restoring-from-a-backup' | relativize_url }}) or
[format the journal]({{ '/en/operation/Journal.html#formatting-the-journal' | relativize_url }}):

1. Alluxio 2.X is not compatible with 1.X journals.
1. UFS journal and embedded journal files are not compatible.
1. Journals for `ROCKS` and `HEAP` metastore are not compatible.

If you are using UFS journal and see errors like "Failed to replace a bad datanode on the existing pipeline due to no more good datanodes being available to try",
it is because Alluxio master failed to update journal files stored in a HDFS directory according to
the property `alluxio.master.journal.folder` setting. There can be multiple reasons for this type of errors, typically because
some HDFS datanodes serving the journal files are under heavy load or running out of disk space. Please ensure the
HDFS deployment is connected and healthy for Alluxio to store journals when the journal directory is set to be in HDFS.

If you do not find the answer above, please post a question following [here](#posting-questions).

### Q: I added some files in under file system. How can I reveal the files in Alluxio?

A: By default, Alluxio loads the list of files the first time a directory is visited.
Alluxio will keep using the cached file list regardless of the changes in the under file system.
To reveal new files from under file system, you can use the command
`alluxio fs ls -R -Dalluxio.user.file.metadata.sync.interval=${SOME_INTERVAL} /path` or by setting the same
configuration property in masters' `alluxio-site.properties`.
The value for the configuration property is used to determine the minimum interval between two syncs.
You can read more about metadata sync from under file systems
[here]({{ '/en/core-services/Unified-Namespace.html' | relativize_url }}#ufs-metadata-sync).

### Q: I see an error "Block ?????? is unavailable in both Alluxio and UFS" while reading some file. Where is my file?

A: When writing files to Alluxio, one of the several write type can be used to tell Alluxio worker how the data should be stored:

`MUST_CACHE`: data will be stored in Alluxio only

`CACHE_THROUGH`: data will be cached in Alluxio as well as written to UFS

`THROUGH`: data will be only written to UFS

`ASYNC_THROUGH`: data will be stored in Alluxio synchronously and then written to UFS asynchronously

By default the write type used by Alluxio client is `ASYNC_THROUGH`, therefore a new file written to Alluxio is only stored in Alluxio
worker storage, and can be lost if a worker crashes. To make sure data is persisted, either use `CACHE_THROUGH` or `THROUGH` write type,
or increase `alluxio.user.file.replication.durable` to an acceptable degree of redundancy.

Another possible cause for this error is that the block exists in the file system, but no worker has connected to master. In that
case the error will go away once at least one worker containing this block is connected.

### Q: I'm running an Alluxio shell command and it hangs without giving any output. What's going on?

A: Most Alluxio shell commands require connecting to Alluxio master to execute. If the command fails to connect to master it will
keep retrying several times, appearing as "hanging" for a long time. It is also possible that some command can take a long time to
execute, such as persisting a large file on a slow UFS. If you want to know what happens under the hood, check the user log (stored
as `${ALLUXIO_HOME}/logs/user_${USER_NAME}.log` by default) or master log (stored as `${ALLUXIO_HOME}/logs/master.log` on the master
node by default).

If the logs are not sufficient to reveal the problem, you can [enable more verbose logging]({{ '/en/operation/Basic-Logging.html#enabling-advanced-logging' | relativize_url }}).

### Q: I'm getting unknown gRPC errors like "io.grpc.StatusRuntimeException: UNKNOWN"

A: One possible cause is the RPC request is not recognized by the server side.
This typically happens when you are running the Alluxio client and master/worker with different versions where the RPCs are incompatible.
Please double check and make sure all components are running the same Alluxio version.

If you do not find the answer above, please post a question following [here](#posting-questions).

## Performance FAQ

### Q: I tested Alluxio/Spark against HDFS/Spark (running simple word count of GBs of files). Why is there no discernible performance difference?

A: Alluxio accelerates your system performance by leveraging temporal or spatial locality using distributed in-memory storage
(and tiered storage). If your workloads don't have any locality, you will not see noticeable performance boost.

**For a comprehensive guide on tuning performance of Alluxio cluster, please check out [this page]({{ '/en/operation/Performance-Tuning.html' | relativize_url }}).**

## Environment

Alluxio can be configured under a variety of modes, in different production environments.
Please make sure the Alluxio version being deployed is update-to-date and supported.

## Posting Questions

When posting questions on the [Mailing List](https://groups.google.com/forum/#!forum/alluxio-users)
or [Slack channel](https://alluxio.io/slack), please attach the full environment information, including
- Alluxio version
- OS version
- Java version
- UnderFileSystem type and version
- Computing framework type and version
- Cluster information, e.g. the number of nodes, memory size in each node, intra-datacenter or cross-datacenter
- Relevant Alluxio configurations like `alluxio-site.properties` and `alluxio-env.sh`
- Relevant Alluxio logs and logs from compute/storage engines
- If you face a problem, please try to include clear steps to reproduce it