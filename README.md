# 分布式数据库课程实践 DDB-Project2019
## 分布式旅游预定系统 Distributed Travel Reservation System 

## 1. 系统概述

### 1.1 组件
1. 资源管理器(Resource Manager)‏
    - 数据操作
    - 查询、更新(插入，删除) 数据
    - 提供对资源访问的封装，完成对数据的实际访问和数据持久化
2. 事务管理器(Transaction Manager)‏
    - 提供事务管理功能，保证事务的ACID特性
3. 流程控制器(Workflow Controller)‏
    - 客户端看到的整个系统的调用接口，使系统的其它部分如TM,RM和实际的数据表对客户端透明

### 1.2 数据定义
系统存储以下五张表
- FLIGHTS(flightNum, price, numSeats, numAvail)‏
- HOTELS(location, price, numRooms, numAvail)‏
- CARS(location, price, numCars, numAvail)‏
- CUSTOMERS(custName)‏
- RESERVATIONS(custName, resvType, resvKey)‏

关于数据的一些假设，简化系统数据库模式设计
- 每个地点(localtion)只有一个旅馆和租车行
- 只有一个航空公司
- 一个航班上所有座位的价格相同
- 同一地点所有房间和车价格相同

### 1.3 数据操作
航班
- 添加新航班
- 给航班增加座位
- 取消航班
- 查询航班上的剩余座位数
- 查询航班价格
- 为客户预定航班的座位

租车和旅馆数据有着类似操作

需要实现的操作在接口文件`WorkflowController.java`中描述

### 1.4 目标
1. 实现数据库事务 `ACID` 性质
2. 保证在分布式情况下出现异常数据库仍然可以保持 `ACID` 性质

可能出现的错误情况
- 事务本身的错误
- 事务并发性引起的错误
- DBMS错误
- 硬件错误：如磁盘故障
- 系统崩溃
- 死机，CPU中止……

### 1.5 实现方法
- 影子页面(Shadow paging)‏
- 严格两阶段锁(strict 2PL)‏
- 两阶段提交(2PC)
- ……

## 2. 程序概述
### 2.1 环境
> Ubuntu
- Ubuntu 18.04
- openjdk version "1.8.0_212"

> Windows with Docker
```bash
docker pull ubuntu:18.04
docker run -it --name ubuntu -v /d/DDB-Project2019/:/home/DDB-Project2019 ubuntu:18.04 
docker exec -it ubuntu bash
cd home/DDB-Project2019

apt-get update
apt-get install openjdk-8-jdk-headless
```
开启多个命令行窗口
```bash
docker exec -it ubuntu bash
cd home/DDB-Project2019
```

### 2.2 目录
- src
    - lockmgr: 该部分是已提供的实现好的锁管理器
    - test: 该部分是用Java编写实现的测试用例
    - tets.part2: 该部分是参考[UCI CS223 Project 2 ](https://www.ics.uci.edu/~cs223/projects/projects2.html)修改实现的测试用例，用于脚本一键测试
        - data: 运行生成的数据文件夹
        - results: 测试案例的 log
        - scripts: 测试案例脚本
        - MASTER.xml: 选择测试的脚本
        - RunTests.java: 测试主程序
    - transaction: 该部分是主要实现部分
        - data: 运行生成的数据文件夹
        - exceptions: 自定义的可能异常
        - models: 数据实体定义，包括接口 `ResourceItem` 与类 `Car`, `Customer`, `Flight`, `Hotel`, `Reservation`, `ReservationKey`
        - Client.java: 脚本测试时调用的客户端
        - MyClient.java: 默认简单客户端
        - ResourceManager.java: `RM` 接口
        - ResourceManagerImpl.java: `RM` 接口实现
        - RMTable.java: 数据表
        - TransactionManager.java: `TM`接口（主要修改文件）
        - TransactionManagerImpl.java: `TM`接口实现（主要修改文件）
        - Utils.java: 工具类，用于简化代码
        - WorkflowController.java: `WC` 接口
        - WorkflowControllerImpl.java: `WC` 接口实现（主要修改文件）
- .gitignore
- README.md

### 2.3 测试
#### 2.3.1 脚本测试
窗口一（运行Register）
```bash
cd src/lockmgr
make clean
make 

cd ../transaction
make clean
make all

make runregistry &
```

窗口二（进行测试）
```bash
cd src/test.part2
mkdir data
mkdir results

rm -rf results/*
rm -rf data/*
export CLASSPATH=.:gnujaxp.jar
javac RunTests.java
java -DrmiPort=3345 RunTests MASTER.xml
```

注意：在结束测试后，在窗口一 `ctrl+c` 中止进程后，需要执行下述命令完全关闭 Register
```bash
kill $(lsof -t -i:3345)
```
可使用下列命令完全关闭Java程序
```bash
kill $(lsof -c java)
```
#### 2.3.2 Java程序逐个测试
窗口一（运行Register）
```bash
cd src/lockmgr
make clean
make 

cd ../transaction
make clean
make all

make runregistry &
```
窗口二（进行测试）
```bash
cd src/test
mkdir data
mkdir results

make clean
make all

make test 
```

