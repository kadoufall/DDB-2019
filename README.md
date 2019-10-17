# 分布式数据库课程实践 DDB-Project2019
## 分布式旅游预订系统 Distributed Travel Reservation System 

## 环境
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

## 目录
- src
    - lockmgr: 该部分是已提供的实现好的锁管理器
    - test: 该部分是用Java编写实现的测试用例
        - data: 运行生成的数据文件夹
        - results: 测试案例的 log
        - Connector.java: 清除数据、启动组件、获取`WC`的引用、安全退出
        - RunTest.java: 测试主程序
        - Makefile
        - 其它Java程序: 测试用例
    - test.part2: 该部分是参考[UCI CS223 Project 2 ](https://www.ics.uci.edu/~cs223/projects/projects2.html)修改实现的测试用例，用于脚本一键测试
        - data: 运行生成的数据文件夹
        - results: 测试案例的 log
        - scripts: 测试案例脚本
        - MASTER.xml: 选择测试的脚本
        - RunTests.java: 测试主程序
        - Makefile
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
        - Makefile
- .gitignore
- README.md

## 测试
### 脚本测试
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
另外若异常发生可使用下列命令完全关闭所有的Java程序
```bash
kill $(lsof -c java)
```
### Java程序逐个测试
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

