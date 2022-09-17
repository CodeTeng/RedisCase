# RedisCase
详细介绍了Redis的基础与高级的语法，包括集群，哨兵，雪崩，击穿等等，并且还包含了一些Redis的典型案例

# Redis

## 1. Redis入门 

### 1.1 Redis是什么 

- 基于内存的`K/V`存储中间件

- NoSQL键值对数据库

`Redis`不仅仅是数据库，它还可用作**消息队列**等等。

![image.png](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171721295.png)

﻿### 1.2 SQL与NoSQL的对比

![image-20220917172748643](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171727701.png)

- 事务特征
    - 非关系型数据库往往不支持事务，或者不能严格保证ACID的特性，只能实现基本的一致性。

- 存储方式
    - 关系型数据库基于磁盘进行存储，会有大量的磁盘IO，对性能有一定影响
    - 非关系型数据库，他们的操作更多的是依赖于内存来操作，内存的读写速度会非常快，性能自然会好一些

* 扩展性
    * 关系型数据库集群模式一般是主从，主从数据一致，起到**数据备份**的作用，称为**垂直扩展**。
    * 非关系型数据库可以将数据拆分，存储在不同机器上，可以保存海量数据，**解决内存大小**有限的问题。称为**水平扩展**。
    * 关系型数据库因为表之间存在关联关系，如果做水平扩展会给数据查询带来很多麻烦

### 1.3 Redis的特征

- 键值（key-value）型，value支持多种不同数据结构，功能丰富
- 单线程，**每个命令具备原子性**
- 低延迟，速度快（基于内存、IO多路复用、良好的编码）
- 支持数据持久化
- 支持主从集群、分片集群
- 支持多语言客户端

### 1.4 Redis的安装

建议在Linux下进行安装，直接到官网进行安装即可，注意安装完毕后注意修改`redis.conf`文件，设置`bind ip`，`requirepass`等

## 2 Redis常见命令

Redis是典型的key-value数据库，key一般是字符串，而value包含很多不同的数据类型：

![image-20220917175600954](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171756006.png)

- redis官网命令集：https://redis.io/commands/，中文版：http://www.redis.cn/commands.html

- redis-cli help 命令查看，help [command] 可以查看某个具体命令、help @xxx 可以查看某个分组下的命令、

### 2.1 Redis通用命令

通用指令是部分数据类型的，都可以使用的指令，常见的有：

- `KEYS`：查看符合模板的所有key
- `DEL`：删除一个指定的key
- `EXISTS`：判断key是否存在
- `EXPIRE`：给一个key设置有效期，有效期到期时该key会被自动删除
- `TTL`：查看一个KEY的剩余有效期

通过`help [command] `可以查看一个命令的具体用法，例如：

```sh
# 查看keys命令的帮助信息：
127.0.0.1:6379> help keys

KEYS pattern
summary: Find all keys matching the given pattern
since: 1.0.0
group: generic
```

### 2.2 String类型

`String`类型，也就是字符串类型，是Redis中最简单的存储类型。

其`value`是字符串，不过根据**字符串的格式**不同，又可以分为3类：

- `string`：普通字符串
- `int`：整数类型，可以做自增、自减操作
- `floa`t：浮点类型，可以做自增、自减操作

不管是哪种格式，底层都是字节数组形式存储，只不过是编码方式不同。

单key的value最大不能超过512M。

![image-20220917180548742](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171805786.png)

#### 2.2.1 String的常见命令

- SET：**添加或者修改**已经存在的一个String类型的键值对
- GET：根据key获取String类型的value
- MSET：批量添加多个String类型的键值对
- MGET：根据多个key获取多个String类型的value
- INCR：让一个整型的key自增1
- INCRBY:让一个整型的key自增并指定步长，例如：incrby num 2 让num值自增2，也可使用负数
- INCRBYFLOAT：让一个浮点类型的数字自增并指定步长
- SETNX：添加一个String类型的键值对，**前提是这个key不存在，否则不执行**
- SETEX：添加一个String类型的键值对，并且**指定有效期**

#### 2.2.2 Key结构

Redis没有类似MySQL中的Table的概念，我们该如何区分不同类型的key呢？

例如，需要存储用户、商品信息到redis，有一个用户id是1，有一个商品id恰好也是1，此时如果使用id作为key，那就会冲突了，该怎么办？

我们可以通过给key添加前缀加以区分，不过这个前缀不是随便加的，有一定的规范：

Redis的key允许有多个单词形成层级结构，多个单词之间用':'隔开，格式如下：

```
项目名:业务名:类型:id
```

这个格式并非固定，也可以根据自己的需求来删除或添加词条。这样以来，我们就可以把不同类型的数据区分开了。从而避免了key的冲突问题。

例如我们的项目名称叫 teng，有user和product两种不同类型的数据，我们可以这样定义key：

- user相关的key：**teng:user:1**

- product相关的key：**teng:product:1**

如果Value是一个Java对象，例如一个User对象，则可以将对象序列化为JSON字符串后存储：

| **KEY**        | **VALUE**                                  |
| -------------- | ------------------------------------------ |
| teng:user:1    | {"id":1,  "name": "Jack", "age": 21}       |
| teng:product:1 | {"id":1,  "name": "小米11", "price": 4999} |

### 2.3 Hash类型

Hash类型，也叫散列，其value是一个无序字典，类似于Java中的`HashMap`结构。

String结构是将对象序列化为JSON字符串后存储，当需要修改对象某个字段时很不方便：

![image-20220917181202086](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171812121.png)

Hash结构可以将对象中的每个字段独立存储，可以针对单个字段做CRUD：

![image-20220917181227388](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171812430.png)

常见命令：

- HSET key field value：添加或者修改hash类型key的field的值

- HGET key field：获取一个hash类型key的field的值

- HMSET：批量添加多个hash类型key的field的值

- HMGET：批量获取多个hash类型key的field的值

- HGETALL：获取一个hash类型的key中的所有的field和value
- HKEYS：获取一个hash类型的key中的所有的field
- HINCRBY:让一个hash类型key的字段值自增并指定步长
- HSETNX：添加一个hash类型的key的field值，前提是这个field不存在，否则不执行

### 2.4 List类型

Redis中的List类型与Java中的`LinkedList`类似，可以看做是一个双向链表结构。既可以支持正向检索和也可以支持反向检索。

特征也与LinkedList类似：

- 有序
- 元素可以重复
- 插入和删除快
- 查询速度一般

常用来存储一个有序数据，例如：朋友圈点赞列表，评论列表等。

![image-20220917181515097](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171815194.png)

### 2.5 Set类型

Redis的Set结构与Java中的`HashSet`类似，可以看做是一个value为null的HashMap。因为也是一个hash表，因此具备与HashSet类似的特征：

- 无序

- 元素不可重复

- 查找快

- 支持交集、并集、差集等功能

![image-20220917181720515](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171817570.png)

### 2.6 SortedSet类型

Redis的SortedSet是一个可排序的set集合，与Java中的`TreeSet`有些类似，但底层数据结构却差别很大。SortedSet中的每一个元素都带有一个`score`属性，可以基于score属性对元素排序，底层的实现是一个跳表（SkipList）加 hash表。

SortedSet具备下列特性：

- 可排序
- 元素不重复
- 查询速度快

因为SortedSet的可排序特性，经常被用来实现排行榜这样的功能。

![image-20220917182008484](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171820538.png)

- **升序**获取sorted set 中的指定元素的排名：ZRANK key member

- **降序**获取sorted set 中的指定元素的排名：ZREVRANK key memeber


练习：

将班级的下列学生得分存入Redis的SortedSet中：

Jack 85, Lucy 89, Rose 82, Tom 95, Jerry 78, Amy 92, Miles 76

并实现下列功能：

- 删除Tom同学

- 获取Amy同学的分数

- 获取Rose同学的排名

- 查询80分以下有几个学生

- 给Amy同学加2分

- 查出成绩前3名的同学

- 查出成绩80分以下的所有同学

![image-20220917191246908](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171912957.png)

## 3. Redis的Java客户端

### 3.1 主流客户端

在Redis官网中提供了各种语言的客户端，地址：https://redis.io/clients

对于Java，推荐以下三种

![image-20220917191949850](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171919918.png)

- Jedis和Lettuce：这两个主要是提供了Redis命令对应的API，方便我们操作Redis，而SpringDataRedis又对这两种做了抽象和封装，因此我们后期会直接以SpringDataRedis来学习。（注意，Jedis是线程不安全的，常配合连接池进行使用）
- Redisson：是在Redis基础上实现了分布式的可伸缩的java数据结构，例如Map.Queue等，而且支持跨进程的同步机制：Lock.Semaphore等待，比较适合用来实现特殊的功能需求。

### 3.2 SpringDataRedis

SpringData是Spring中数据操作的模块，包含对各种数据库的集成，其中对Redis的集成模块就叫做SpringDataRedis

官网地址：https://spring.io/projects/spring-data-redis

* 提供了对不同Redis客户端的整合（Lettuce和Jedis）
* 提供了**RedisTemplate**统一API来操作Redis
* 支持Redis的**发布订阅**模型
* 支持Redis**哨兵**和Redis**集群**
* 支持基于Lettuce的**响应式编程**
* 支持基于JDK.JSON.字符串.Spring对象的**数据序列化及反序列化**
* 支持基于Redis的JDKCollection实现

SpringDataRedis中提供了RedisTemplate工具类，其中封装了各种对Redis的操作。并且将不同数据类型的操作API封装到了不同的类型中：

![image-20220917192605432](https://teng-1310538376.cos.ap-chongqing.myqcloud.com/3718/202209171926478.png)

> 注意：要在Spring Data Redis 中使用Lettuce线程池的话，要额外引入apache commons-pool2依赖

相关依赖

```xml
<!--redis依赖-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--common-pool-->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
<!--Jackson依赖-->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

配置文件

```yml
spring:
  redis:
    host: 192.168.150.101
    port: 6379
    password: 806823
    lettuce:
      pool:
        max-active: 8  #最大连接
        max-idle: 8   #最大空闲连接
        min-idle: 0   #最小空闲连接
        max-wait: 100ms #连接等待时间
```

### 3.3 RedisTemplate 序列化

RedisTemplate 默认使用 JDK 原生序列化器，可读性差，内存占用大，因此可以用以下两种方式来改变序列化机制：

1. **自定义 RedisTemplate**，指定 key 和 value 的序列化器
2. **使用自带的 StringRedisTemplate**，key 和 value 都默认使用 String 序列化器，仅支持写入 String 类型的 key 和 value。因此需要自己将对象序列化成 String 来写入Redis，从 Redis读出数据时也要手动反序列化。
