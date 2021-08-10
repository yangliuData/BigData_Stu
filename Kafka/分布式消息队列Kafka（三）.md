# 分布式消息订阅发布系统Kafka（三）

## 一、回顾

1. Topic的管理

   - --create：用于指定创建

   - --delete：指定删除

   - --describe：指定描述

   - --alter：修改

   - --list：列举

   - --topic：create、delete、describe、alter需要指定操作具体的哪个topic

   - --partition：指定分区个数

   - --replication-factor：指定副本个数【指的是每个分区的总个数：3，这个分区总共有3份】

   - --zookeeper：指定zookeeper的地址的

   - 生产者：

     - --topic：生产到哪个topic中
     - --broker-list：指定KafkaServer的地址

   - 消费者

     - --topic：消费哪个topic

     - --bootstrap-server：指定KafkaServer的地址

       - 0.9版本之后：两种方式都提供了

         - 可以指定--zookeeper：直接连接zookeeper

           - 自动将消费者提交的offset记录在zookeeper中

         - 可以指定--bootstrap-server：直接连接Kafka

           - 将消费者提交的offset记录在一个topic中

             ```
             __consumer_offsets：这个topic记录了所有Kafka中消费者组消费的偏移量
             ```

       - 0.9版本之前老的消费者不指定kafka地址，指定zookeeper地址

2. Kafka Java API

   - API分类
     - High Level API：高级API【基于原生的API做了封装，所有的offset由Kafka自己管理】
     - Simple API：原生API【工作中主要使用的方式】
   - 消费的两种模式
     - 推：kafka将数据发送给消费者【性能最大化：但是数据丢失比较常见】
       - Kafka默认不断推送下一条数据给消费者，不论消费者有没有消费成功，需要的是哪个数据
     - 拉：消费者主动到kafka中获取数据【工作中使用这种方式消费Kafka中的数据】
       - 消费者可以根据自己的需求来获取对应的偏移量的数据
   - 实际的API的应用
     - 与Spark/Flink集成，消费Kafka的数据，与我们当前写的API不一样
     - Spark等工具会将读写Kafka的 API封装到自己的API中，封装成对应的方法
     - 开发Spark程序的时候，调用对应的方法即可
   - 生产者API
     - KafkaProducer<Properties>
     - send(new ProducerRecord(topic,partition,key,value))
   - 消费者API
     - KafkaConsumer<Properties>
     - poll(100)：定时从Kafka中拉取数据
       - ConsumerRecord
         - topic
         - partion
         - offset
         - key
         - value
   - offset：管理commit offset
     - 是Kafka中用于标记每一条数据的逻辑编号，对每个分区中的数据记性标记
       - 每个分区的 第一条数据对应的偏移量：0
       - 偏移量是分区级别的
     - 目的
       - 提供顺序读写，保证顺序消费
       - 保证数据消费的一次性语义：exactly once：有且仅有一次
         - at leasted once：至少一次，容易导致数据重复
         - at most once：最多一次，可能导致数据丢失
       - 什么情况下会导致数据丢失或者重复消费？
         - 消费者程序在运行的时候是不会有这个问题的，消费者会自动不断获取下一个offset
         - 如果消费者故障，重启以后，要保证一次性语义
     - offset的分类
       - consumer offset：消费者默认消费的offset
         - 程序正常运行时使用的offset
         - 每次消费者正常消费，都会提交下一个要消费的偏移量给Kafka
         - Kafka根据消费者提交的 偏移量来返回数据
         - 维护在consumer端，消费者会自动记录刚刚消费的位置，不断向kafka获取下一个位置
       - commit offset：消费者提交给Kafka的offset
         - 维护在kafka中，记录每个消费者组消费的位置
         - 消费者从故障中恢复，消费者不知道自己上一次的位置，不会提交上一次的偏移量
         - Kafka会判断消费者有没有提交consumer offset，没有就按照commit offset来返回
     - 自动提交commit offset
       - 由消费者定时向Kafka提交Commit offset
       - 问题：
       - 数据丢失：如果刚消费，就提交了，然后消费者故障，消费失败，重启，根据commit返回下一条，上一批次的数据丢失了
       - 数据重复：如果消费完成，立马消费者故障了，没有及时的提交offset，重启以后，根据上一次commit的位置来消费，上一批次已经消费过了，数据重复
     - 手动提交commit offset
       - 原则：消费一次提交一次
       - 可以按照批次提交整个topic的offset【容易导致数据重复】 ，也可以提交每个分区的offset
       - 由于offset是分区级别的
       - 实现：每个分区消费成功一次，就立即提交一次
     - Kafka中offset的唯一标记
       - Group id + Topic + Partition + Offset
       - 整个消费者组消费这个topic的这个分区的这个位置
       - 工作中：要将这样的一个结构存储在外部系统：mysql等数据库中，用于恢复

## 二、课程目标

1. 存储结构与检索过程
2. 数据安全
   - 生产数据安全：不丢失
   - 消费数据安全：不丢失不重复
3. CAP
   - 所有的分布式存储中都有的一个概念
   - 一般分布式存储都只能满足其中两个条件
4. Kafka中常见属性的配置
5. Kafka与Flume集成
   - Flume：source、channel、sink  => Kafka
   - Kafka sink
6. Kafka-eagle
   - 类似于NviaCat这样的工具，可视化终端工具

## 三、Kafka的存储结构

### 1、存储过程

- 逻辑+物理存储

- 所有生产者将数据生产到Kafka的Topic的中

- Kafka读取Zookeeper中的元数据，获取该Topic的所有分区的信息

- Topic：多个分区，存储在多台KafkaServer

  ![image-20200305101231396](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305101231396.png)

- Partition：根据分区规则来决定这条数据会被写入哪个分区

  - 物理上表现为一个目录
  - 分区规则
    - 1-如果指定了topic、key、value，根据key的hash取余分区个数
    - 2-如果指定指定了topic,value，根据轮询写入分区
    - 3-如果执行了topic,part,key,value，写入对应part
    - 4-自定义分区
  - Replication：分区有多个副本，但数据只读写主副本，通过元数据获取主副本的位置
  - 根据分区的信息，追加分配offset
  - 将生成好的这条数据的 信息
    - key、value、topic、part、offset
  - 写入PageCache：基于内存的缓存区域，类似于Hbase中的memstore

- Flush：将数据从缓从中写入segment文件

  - 异步的将pageCache中的数据写入segment中

- segment：分区段/文件段，将整个分区的数据再次划分，划分为多个segment

  - 在同一时刻，只会有一个segment文件段是可用的，其他的segment文件段都是以前老的数据

  - 每一个逻辑上的segment就是两个文件

  - .log：存储这条数据以及元数据：key,value【String】 【经过序列化以后的二进制文件数据】

  - .index：对应的.log文件的索引信息

  - 消费者消费的数据必须是segment文件中有的数据

    

### 2、segment分段规则

- 每个segment都有两个文件：.log和对应的.index文件

- 这两个文件的文件名除了后缀，其他是一致的

  ```
  00000000000000000000.index			对应的.log文件的索引信息，会被加载到内存中
  00000000000000000000.log			生产者生产的 数据
  00 00 00 00 00 00 00 00 00 00
  ```

- 分段的规则：同一时刻只有一个segment文件段

  - 根据文件大小

    ```
    log.segment.bytes=1073741824=1G
    ```

  - 根据时间长度

    ```
    log.roll.hours=168/24 = 7天
    ```

  - 当.log文件达到条件，该.log文件便不再被写，会生成新的segment文件段，新的数据写入新的.log文件

- 命名规则

  - partition：Topic-partNumber

  - segment的命名规则：总共20位数字

    - 第一个segment的文件名称以0标记，不足20位左补0

      ```
      0000000000000000000 0.log
      ```

    - 例如该文件当写入了20000条数据以后，就达到1G了，该文件不再写了生成了新的segment

      - 这个文件的偏移量范围：0 ~ 19999

    - 新的segment的 命名规则

      - 上一个segment文件的记录条数
      - 上一个segment文件的最后一条数据的偏移量【数据在文件中的偏移量】+1
      - 当前这个segment最小的数据偏移量
      - 如果你在网上或者别的地方：看到：上一个segment文件的最后偏移量【消费者请求的偏移量】

      ```
      00000000000000 20000.log
      00000000000000 20000.index
      ```

- 回去可以做测试

  ```
  log.segment.bytes=10240，每10kb生成一个文件
  ```

  - 重启Kafka
  - 创建一个topic，一个分区，两个副本
  - Java生产者，往里面写数据，建议每一条数据写大一点【一条数据不能超过1M】
    - 测试，写入多少条会生成新的文件
    - 新的文件的命名

- 思考：为什么 要将一个分区的数据划分为多个segment呢？
  - 不分：
    - 这个分区的所有数据全部写在一个.log文件中，这个.log文件会非常大，对于存储就会非常麻烦
    - ==**消息队列：临时的缓存数据**==，如果数据永远不再需要的了，可以删除，kafka可以配置自动删除
    - 如果一个分区的所有数据都在一个文件中，怎么删除过期的数据？牺牲的代价就比较高
      - 以数据如果超过7天，就过期了 ，希望自动删除
  - 分：多个segment，segment是有序的，同一时间只有一个读写的segment，其他的segment只提供读
    - 写的 segment比较小，容易操作
    - ==如果数据过期，我们可以以segment为单位，删除过期数据==





## 四、kafka的数据检索

### 1、检索过程

- 访问元数据，获取该Topic所对应的所有分区的信息，找到leader分区

- 请求消费指定的数据，第几条数据

- Kafka中拥有的数据以偏移量管理

- topic：分区0：leader副本

  ```
  0000000000000000000  0.log			offset ： 0 ~ 199999
  00000000000000 200000.log			offset ： 200000 ~ 368768
  00000000000000368769.log			offset ： 368769 ~ 399999
  00000000000000400000.log			offset :  400000 ~ 499999
  ```

- 需求：查找整个Kafka中第368777条数据

  - message：指的是第多少条数据或者下一条要请求的数据，不是偏移量 message - 1  = offset

- 第一步：转换单位：统一使用offset来访问

  - 查找的offset = 368777 - 1 = 368776

- 第二步：通过二分比较来判断这条数据在哪个segment文件中

  - 根据segment文件名来判断对应的文件
    - 大于当前的segment的最小的offset并且小于下个segment的最小offset
    - 数据就在当前的segment文件中

- 第三步：找到对应的segment编号以后，从内存中访问该segment的index文件

  - 通过index进行二分查找想要的这条数据在.log文件中的位置

### 2、index文件

 ![index](Day65_20200305_分布式消息队列Kafka（三）.assets/index.png) 

- index与log文件成对出现，用于记录对应的log文件数据的位置索引，是==稀疏索引==，不是完整索引

  - 完整索引：log文件中的每一条数据都在索引中有记录
  - 稀疏索引：og文件中的数据只有部分数据有索引存储在index中
  - 为什么要构建稀疏索引：数据量特别大，如果要用全量索引，索引所占用的内存会比较高，检索索引的性能也会下降
  - index中记录的是这条数据在log文件中的第几条，和行对应的偏移位置，而不是全局的偏移量
    - 为什么不记录偏移量，不就不用转换计算了么？
      - 如果写了偏移量，索引的检索性能会差

- 需求：读取offset= 368776

  - 先读取整个log文件的 索引信息

  - 计算要读取的数据在文件中offset

    ```
    368776 - 368769 = offset的偏差为7 
    ```

  - 计算再文件中的物理位置

    ```
    7 + 1 = 第8条数据 
    ```

  - 通过二分对索引表进行查找，找到举例最近最小的那个

    ```
    8,1686
    ```

  - 读取.log文件，从第1686位置开始读取数据

- 需求：读取第 368776条数据 ： offset =  368775
  - 计算offset差值：368775 - 368769 = 6
  - 计算物理位置：6 + 1 = 7
  - 二分匹配索引：取6，得到1407
  - 从1407找到下一条开始读取

### 3、Kafka性能

- 相比与hbase，Hbase主动使用大量内存，随机读写内存，所以快，Kafka本身数据是在磁盘中持久化的 ，为什么这么快？
- PageCache：页缓存，将数据先写入内存进行缓存，后台通过线程不断刷写到segment文件中
- Zero copy ：零拷贝，硬件机制，数据只在内存中传递
- 顺序读写硬盘：避免了寻址使用随机读写来提高读写速度
  - 数据永远以追加的形式写入
  - 顺序：按照一定连续地址
    - 存储：100个位置
    - 文件：3条
      - 第一条：5
      - 第二条：6
      - 第三条：7
  - 随机：地址不是连续的
    - 存储：100个位置
    - 文件：3条
      - 第一条：5
      - 第二条：19
      - 第三条：45
  - 硬盘的读写比较慢：随机读写比较慢
  - 读写的分类
    - 内存顺序读写：第一快的
    - 内存随机读写：第二慢的
    - 硬盘顺序读写：第二快的，要比内存的随机读写还要快
    - 硬盘随机读写：第一慢的



回顾

1. Kafka中数据的读和写的过程
   - Topic：对数据的分类存放
   - Partition：进行分布式的存储
     - Replication：保证分区可用性
   - Segment：提高磁盘利用率【为了方便删除历史数据】
     - .log：存储数据
     - .index：存储对应的 .log文件的索引的
   - 写
     - pageCache：不断从后台flush到segment中
     - segment命名规则：当文件存储到一定大小或者到达一定时间就会产生一个新的segment
       - 新的segment的命名就是这个segment中数据在整个分区中的最小偏移量
   - 读
     - 根据segement文件名称来判断要读取的数据在哪个文件中
       - 二分
     - 读取对应的.index文件来获取索引信息【稀疏索引】，得到数据在文件中的偏移位置
     - 读取到对应的数据位置
2. Kafka中为什么读写磁盘性能很高？
   - 顺序读写：提高很高的写的性能，会比随机读写内存还要高
   - pagCache：数据缓存
   - Zero Copy：数据直接内存对内存





## 五、Kafka的数据安全

### 1、消息队列的问题

- 怎么保证消息队列某台机器故障以后，整个系统依旧可用？
  - Kafka是分布式集群结构，自动发现有机器宕机，会通知所有的broker，生产和消费，不能再请求对应的故障节点
- 怎么保证机器故障以后，数据照样可读？
  - 分区的副本来实现的，每一个分区的相同副本都不在一台机器，所以一台机器故障，该分区的副本在别的机器上还有
  - 如果故障的 机器上有分区的leader副本，其他机器上都是从副本，从副本会选举出一个新的leader副本提供 读写

### 2、消息队列消费语义

- 数据一次性语义：所有的消息队列都在解决这个问题

- at most once ：最多一次，0次或者1次，消息可能会丢失，但不会重复

  - 统计PV
    - 产生的 数据

      ```
      1			2020-03-05 12:00:00		www.baidu.com
      2			2020-03-05 12:00:00		www.baidu.com
      3			2020-03-05 12:00:00		www.baidu.com
      ```

    - 消费中间产生了丢失的数据

      ```
      1			2020-03-05 12:00:00		www.baidu.com
      3			2020-03-05 12:00:00		www.baidu.com
      ```

    - 结果就少了

      ```
      本来应该是3
      
      实际统计是2
      ```

- at least once  ：至少一次，1次或者N次，消息可能会重复，但不会丢失

  - 大多数的消息队列都是满足这一点：数据重复可以去重，但数据丢失无法重新获取

  - 统计PV

    - 产生的 数据

      ```
      1			2020-03-05 12:00:00		www.baidu.com
      2			2020-03-05 12:00:00		www.baidu.com
      3			2020-03-05 12:00:00		www.baidu.com
      ```

    - 消费中间产生了重复的数据

      ```
      1			2020-03-05 12:00:00		www.baidu.com
      1			2020-03-05 12:00:00		www.baidu.com
      2			2020-03-05 12:00:00		www.baidu.com
      3			2020-03-05 12:00:00		www.baidu.com
      ```

    - 导致了PV的结果多了

      ```
      本来应该是3
      
      实际统计是4
      ```

    - 工作中的解决方案

      - 在生产的时候要考虑到数据重复的问题
      - 建议通过一个唯一字段来标识每条数据，最后得到数据以后按照唯一字段来去重

- Exactly Once：只且一次，消息不丢失不重复，只消费成功一次

  - 完美的理想状态，在程序中尽量保证这一点

### 3、保证生产数据不丢失

- 生产者到Kafka之间的数据不丢失
  - 生产者生产的数据是唯一的，不会同一条数据发送两次，不会产生重复问题
- 主要的保证
  - acks：同步或者异步的确认属性
    - 0：生产者将数据发送给Kafka，不需要返回对应的确认，直接发送下一条
      - 异步的发送，不管Kafka有没有接收成功
      - 性能是最快的
      - 容易产生数据丢失的情况
    - 1：生产者将数据发送给Kafka，Kafka将数据存储在对应分区的主副本中，一旦存储成功，就返回ack，生产者发送下一条
      - 数据写入了分区：数据没有丢失
      - 只要写一个副本：性能比较快
      - 缺点：
        - 如果写入的主副本在从副本还没有同步成功之前就故障了，容易导致数据丢失
    - all/-1：生产者将数据发送给Kafka，Kafka将数据存储到对应的分区主副本，通知所有从副本过来同步，所有副本中都同步有了以后，返回整体的ack给生产者，生产者发送下一条
      - 完全同步的状态，相对能保证数据不会丢失
      - 这是串行的数据传输，必须一条传输完成，才能发送下一条
      - 经常会因为副本同步时间比较长，同步超时，最终导致发送失败，重新发送
        - 一般会搭配：min.insync.replicas
        - 这个参数可以指定，只要几个副本同步成功，就发送下一条
      - 数据量小还好，数据量一旦比较大，副本之间的同步就比较吃力，负载比较高 
      - 如果数据量不大：实时架构中：每s的数据万级别
      - 如果数据量大，而且要求性能和数据安全性，只能通过机器扩容来解决

### 4、保证消费数据一次性语义

- 保证

  - 通过commit offset来保证数据不丢失不重复
  - 过程
    - 1-消费
    - 2-成功
    - 3-提交commit offset
    - 不断重复123这个过程，消费成功与提交commit offset一定要是一个同步的过程
  - 手动提交commit offset
  - commit offset存储在Kakfa中
  - commit offset用于保证消费者故障重启以后，通过commit offset来返回数据给消费者

- 如果消费者第一次启动，第一次在zookeeper中进行注册 ，也是没有下一条的offset

  - 以什么区分第一次启动：group id

  - auto.offset.reset：决定第一次启动从Topic的分区的什么位置开始消费

  - none：报错

  - latest：从最新的数据开始消费，默认的

    - 之前演示命令行的消费者的时候，如果topic中已经存在的数据默认不会消费
    - 默认只消费最新的数据，消费者启动以后产生的数据
    - 除非加上--from-begging才能从头开始消费

  - earliest：从最早的数据开始消费，从偏移量为0开始消费

  - 这个参数决定的是consumer offset，与commit offset无关

    

![image-20200305152926915](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305152926915.png)



## 六、CAP理论[了解]

### 1、CAP概念

- Consistency：一致性
  - 在分布式结构中，我们通过其中一个节点来实现写或者更改操作
  - 在分布式其他的节点中，也能读到对应的更改
- Availability：可用性
  - 如果分布式结构中，某一个节点发生故障
  - 其他任何一个没有发生故障的节点，必须在一定的时间内能返回合理的结果【能正常提供读写所有的数据】
- Partition Tolerance：分区容错性
  - 如果任何 一个分区故障，必然要有对应的容错机制来保证分区数据的正常提供
  - Kafka中的分区的主副本与从副本是存在数据差的，如果leader副本故障，从副本不一定能提供对应的数据

### 2、Kafka中的CAP

- Kafka满足CA
  - C：在kafka的任意节点上做对应的修改操作，在其他任意节点上都能看到对应 的更改
  - A：机器宕机，其他机器上还有对应的副本
  - P：kafka中主副本与从副本之间存在数据差，甚至可能会没有可用的从副本

- Kafka中央控制器：kafka Controler【理解为Kafka是一个主从架构，Kafka会选举一个 主】

  - Kafka类似于主从架构，Kafka所有的broker在启动的时候会通过zookeeper选举出一个broker作为中央控制器，负责监控和管理真个Kafka集群中broker和分区的状态
  - 选举规则 ：所有brokerid在zookeeper会创建一个临时的节点，谁创建成功谁就是Controler，其他的broker会watch这个节点，如果Control故障，该临时节点会消失，所有broker重新抢注，产生新的Control
  - 负责
    - 负责 监听所有broker节点，所有的broker会在zookeeper注册一个节点，如果broker故障，控制器会发现该节点丢失，会广播通知所有的节点
    - 会根据元数据，通知将该节点上的丢失的分区的leader副本所对应的分区的ISR列表，进行重新选举

- ISR与OSR

  - AR：所有的副本
    - ISR+OSR
  - ISR：in sync replication 可用副本，与主副本不断同步的副本
  - OSR：out sync replication 不可用副本，没有进行与主副本的同步
    - 判断规则
      - 老版本中有两个参数
        - 时间：replica.lag.time.max.ms
          - 超过这个时间还没有同步，就会列入OSR列表
        - 差值：replica.lag.max.messages
          - 与主副本之间的记录数差值超过了这个参数，就会列入OSR列表
          - 假设这个值是100
          - 主副本：2000
          - 从副本1:1980  =》 ISR
          - 从副本2:1700  =》 OSR
          - 这个规则在我们当前使用的版本被弃用了
            - 原因：在并发量比较高的场景下，这个参数会导致一个现象
              - 刚开始：主从都是1千万条
              - 并发写入主副本：1亿条
              - 第一次判断：从副本同步不过来，与主副本差了接近1亿条
                - 该从副本被列入OSR
              - 第二次判断：从副本已经同步完全的，也新增了1亿条
                - 将该从副本从OSR移除列入ISR列表
            - 现象：有些从副本，不断的在ISR和OSR之间交替
      - 新版本中保留了时间，丢弃了差值的规则

- 分区主从选举

  - 副本的选举：所有ISR列表中的副本会在zookeeper的分区的leader节点下创建一个序列节点，序列最小的成为leader，抢注

- ISR副本同步

  - 从副本是怎么与主副本进行同步的

  - 所有的读写都是操作主副本

    - LEO：整个日志最后 一个offset，最新的offset，
    - HW：最高水位，指的是消费者能够消费的偏移量位置
      - 等于所有副本共同的最新位置
      - r1：主副本：2020
      - r2：从副本：2019
      - r3：从副本：2018
      - 对外提供的HW是2018
    - 数据写入主副本
      - 更新主副本的LEO
      - 所有从副本不断与主副本进行同步，不断更新自己的LEO
      - 从副本的LEO是肯定小于等于主副本的LEO

    - 消费者读数据：
      - 在所有副本没有与主副本同步完成这条数据之前，HW不等于主副本的LEO
      - 直到所有的从副本的LEO与主副本的LEO一致的情况下，消费看到的HW才等于LEO
    - 可能会出现，所有的副本都是 OSR副本，然后主副本故障以后，没有副本可选
      - kafka保证的机制：只要有一个ISR副本就用ISR副本
      - 如果没有ISR副本，只要AR中有任何一个副本也都可以



## 七、Kafka常用配置

注意：教案中的有些配置是老版本的写法

### 1、生产者配置

| 属性                | 值            | 含义                                    |
| ------------------- | ------------- | --------------------------------------- |
| bootstrap.servers   | hostname:9092 | KafkaServer端地址                       |
| poducer.type        | sync          | 同步或者异步发送，0,1，all              |
| min.insync.replicas | 3             | 如果为同步，最小成功副本数              |
| buffer.memory       | 33554432      | 配置生产者本地发送数据的 缓存大小       |
| compression.type    | none          | 配置数据压缩，可配置snappy              |
| partitioner.class   | Partition     | 指定分区的类                            |
| acks                | 1             | 指定写入数据的保障方式                  |
| request.timeout.ms  | 10000         | 等待ack确认的时间，超时发送失败         |
| retries             | 0             | 发送失败的重试次数                      |
| batch.size          | 16384         | 批量发送的大小                          |
| metadata.max.age.ms | 300000        | 更新缓存的元数据【topic、分区leader等】 |

### 2、消费者配置

| 属性                    | 值            | 含义                                    |
| ----------------------- | ------------- | --------------------------------------- |
| bootstrap.servers       | hostname:9092 | 指定Kafka的server地址                   |
| group.id                | id            | 消费者组的 名称                         |
| consumer.id             | 自动分配      | 消费者id                                |
| auto.offset.reset       | latest        | 新的消费者从哪里读取数据latest,earliest |
| auto.commit.enable      | true          | 是否自动commit当前的offset              |
| auto.commit.interval.ms | 1000          | 自动提交的时间间隔                      |



### 3、Kafka集群管理配置

|              属性               | 值                  | 含义                                                     |
| :-----------------------------: | ------------------- | :------------------------------------------------------- |
|            broker.id            | int类型             | Kafka服务端的唯一id，用于注册zookeeper，一般一台机器一个 |
|            host.name            | hostname            | 绑定该broker对应的机器地址                               |
|              port               | 端口                | Kafka服务端端口                                          |
|            log.dirs             | 目录                | kafka存放数据的路径                                      |
|        zookeeper.connect        | hostname:2181       | zookeeper的地址                                          |
|  zookeeper.session.timeout.ms   | 6000                | zookeeper会话超时时间                                    |
| zookeeper.connection.timeout.ms | 6000                | zookeeper客户端连接超时时间                              |
|         num.partitions          | 1                   | 分区的个数                                               |
|   default.replication.factor    | 1                   | 分区的副本数                                             |
|      ==log.segment.bytes==      | ==1073741824==      | ==单个log文件的大小，默认1G生成一个==                    |
|    log.index.interval.bytes     | 4096                | log文件每隔多大生成一条index                             |
|       ==log.roll.hours==        | ==168==             | ==单个log文件生成的时间规则，默认7天一个log==            |
|     ==log.cleaner.enable==      | ==false==           | ==false表示删除过期数据，如果为true，进行compact==       |
|     ==log.cleanup.policy==      | ==delete，compact== | ==默认为delete，删除过期数据==                           |
|    ==log.retention.minutes==    | ==分钟值==          | ==segment生成多少分钟后删除==                            |
|     ==log.retention.hours==     | ==小时值==          | ==segment生成多少小时候删除==                            |
|   log.flush.interval.messages   | Long.MaxValue       | 消息的条数达到阈值，将触发flush缓存到磁盘                |
|      log.flush.interval.ms      | Long.MaxValue       | 隔多长时间将缓存数据写入磁盘                             |
|    auto.create.topics.enable    | false               | 是否允许自动创建topic，不建议开启                        |
|       delete.topic.enable       | true                | 允许删除topic                                            |
|     replica.lag.time.max.ms     | 10000               | 可用副本的同步超时时间                                   |
|    replica.lag.max.messages     | 4000                | 可用副本的同步记录差，该参数在0.9以后被删除              |
| unclean.leader.election.enable  | true                | 允许不在ISR中的副本成为leader                            |
|       num.network.threads       | 3                   | 接受客户端请求的线程数                                   |
|         num.io.threads          | 8                   | 处理读写硬盘的IO的线程数                                 |
|       background.threads        | 4                   | 后台处理的线程数，例如清理文件等                         |

- Kafka中是允许数据compact的合并log数据

  - 如果进行compact需要将以下参数调整

    ```
    log.cleaner.enable=true
    log.cleanup.policy=compact
    ```

  - 将相同的key进行合并，保留最新的value

    - Kafka中也是按照keyvalue进行存储

      ```
      key		value
      1		itcast1
      1		itcast2
      1		itcast3
      1		itcast4
      ```

    - compact

      ```
      1		itcast4
      ```

      

## 八、Kafak集成Flume

### 1、场景

- 对比

  - Flume：实现数据采集的
    - 如果把Flume中的channel当做消息队列的话，就是一个点对点模式的消息队列
    - 一个channel要对应一个sink，如果一个channel对应多个sink，每个sink只能取到一部分数据
  - Kafka：消息队列用于缓存数据，相当于Flume中的channel

- 集成

  -  [Kafka Source](http://flume.apache.org/releases/content/1.9.0/FlumeUserGuide.html#kafka-source) ：采集Kafka中的数据，相当于消费者的角色
  -  [Kafka Channel](http://flume.apache.org/releases/content/1.9.0/FlumeUserGuide.html#kafka-channel) ：将flume采集到的数据缓存在Kafka中
  -  [Kafka Sink](http://flume.apache.org/releases/content/1.9.0/FlumeUserGuide.html#kafka-sink) ：将 Flume采集到的数据生产到Kafka中，由实时计算进行计算，最常用的
    - Flume作为生产者
    - Kafka：消息队列
    - Spark/Flink作为消费者

  

### 2、Kafka sink

```properties
# The configuration file needs to define the sources, 
# the channels and the sinks.
# Sources, channels and sinks are defined per agent, 
# in this case called 'agent'

a1.sources = s1
a1.channels = c1
a1.sinks = k1

#define the source s1
a1.sources.s1.type = spooldir
#监控目录
a1.sources.s1.spoolDir = /export/datas/flume/tomcat
#如果该文件已经被采集完成，后缀名是什么
a1.sources.s1.fileSuffix = .end
#设置过滤.tmp结尾的文件
a1.sources.s1.ignorePattern = ([^ ]*\\.tmp$)

#define the channels c1
a1.channels.c1.type = memory

#define the sinks k1
a1.sinks.k1.type = org.apache.flume.sink.kafka.KafkaSink
a1.sinks.k1.kafka.bootstrap.servers = node-01:9092,node-02:9092,node-03:9092
a1.sinks.k1.kafka.topic = bigdata1801
a1.sinks.k1.flumeBatchSize = 100
a1.sinks.k1.kafka.producer.acks = 1


#bind
a1.sources.s1.channels = c1
a1.sinks.k1.channel = c1
```



## 九、Kafka可视化工具

### 1、介绍

​	Kafka的监控运维管理工具，Kafka的客户端 ，类似于Navicat

### 2、安装

* 下载解压

  ```shell
  tar -zxvf kafka-eagle-bin-1.2.4.tar.gz -C /export/servers/
  cd /export/servers/kafka-eagle-bin-1.2.4/
  tar -zxvf kafka-eagle-web-1.2.4-bin.tar.gz 
  ```

* 修改配置

  * 准备数据库：存储eagle的元数据【mysql，低版本不兼容，建议使用5.5及以上版本】

    ```sql
create database eagle;
    ```
    
    ![image-20200305164643419](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305164643419.png)
  
    
    
  * 修改配置文件：
  
    ```properties
    cd /export/servers/kafka-eagle-bin-1.2.4/kafka-eagle-web-1.2.4
    vim  conf/system-config.properties
    #配置zookeeper集群的名称
    kafka.eagle.zk.cluster.alias=cluster1
    #配置zookeeper集群的地址
    cluster1.zk.list=node-01:2181,node-02:2181,node-03:2181
    #配置连接MySQL的参数
    kafka.eagle.driver=com.mysql.jdbc.Driver
    kafka.eagle.url=jdbc:mysql://192.168.134.1:3306/eagle
    kafka.eagle.username=root
  kafka.eagle.password=123456
    ```
  
    ![image-20200305165002423](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305165002423.png)
  
    ![image-20200305165009656](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305165009656.png)
  
    注意：如果你连接windows的mysql ，检查你的windows的mysql有没有做授权

    ```sql
    --如果MySQL没有做授权，要先做授权，在MySQL中配置允许访问，注意修改自己的用户密码：
    GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY '123456' WITH GRANT OPTION;
    FLUSH PRIVILEGES;
    ```
  
  * 配置环境变量
  
    ```shell
  vim /etc/profile
    
  #KE_HOME
    export KE_HOME=/export/servers/kafka-eagle-bin-1.2.4/kafka-eagle-web-1.2.4
    export PATH=$PATH:$KE_HOME/bin
    
    source /etc/profile
    ```
  
  * 添加执行权限
  
    ```shell
  cd /export/servers/kafka-eagle-bin-1.2.4/kafka-eagle-web-1.2.4
    chmod u+x bin/ke.sh
    ```
  
* 启动服务

  ```
  ke.sh start
  #关闭
  ke.sh stop
  ```

* 测试:webUI

  ```
  node-03:8048/ke
  
  用户名：admin
  密码：123456
  ```

* 自己操作

  * 如果要删除topic：输入：keadmin
  
    ![image-20200305165441356](Day65_20200305_分布式消息队列Kafka（三）.assets/image-20200305165441356.png)

