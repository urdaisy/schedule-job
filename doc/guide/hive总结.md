MapReduce框架（或系统）通常由三个操作（或步骤）组成：
1. Map：每个工作节点将map函数应用于本地数据，并将输出写入临时存储。主节点确保只处理一份冗余的输入数据。
2. Shuffle：工作节点根据输出键（由map函数生成）重新分配数据，使得属于同一键的所有数据都位于同一个工作节点上。
3. Reduce：工作节点现在按键并行处理每组输出数据。（聚合中间数据Shuffle并输出结果，在Reduce时，可以选择处理的中间节点）
来自于Google2004年论文，原文：https://static.googleusercontent.com/media/research.google.com/zh-CN//archive/mapreduce-osdi04.pdf

使用场景：
1、Hive和Pig查询系统，使用类似SQL语句从HDFS（Hadoop分布式文件系统）中检索数据，其内部使用的大数据处理模型MapReduce
2、Hadoop框架所使用的MapReduce详细介绍：https://hadoop.apache.org/docs/r1.2.1/mapred_tutorial.html