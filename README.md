# DDB-Project2019 
## Distributed Travel Reservation System


## 异常处理

### WC异常
WC异常发生后，会丢失当前正在执行的transaction，因此需要在磁盘保存持久化的所有的transaction记录，
在WC重启时载入磁盘保存的记录，并且在新增和删除transaction时候保存到磁盘中


### RM异常
在data目录下，对于数据库数据，保存的有
1. Flight, Car, Customer, Hotel等四张表，
2. 每一个transaction对应的一个文件夹，文件夹下有对应的四张表

RM重启后，首先要恢复所有当前正在执行的transaction和每个transaction对应的数据表，还有四个数据主表。然后RM需要重新enlist到TM。

在RM的insert, query等的每一步，都需要更新transaction和每个transaction对应的数据表。
在commit或者abort后，删除该transaction，并更新xids和该transaction对应的数据表，以及数据主表

FdieRMAfterEnlist

RM在enlist到TM后，die。enlist发生在RM向TM注册。
此时WC会重启RM，但是会是一个新的RM实例，因此接下来对原来的RM有的transaction的任何commit都会
在prepared阶段挂掉。因此需要在TM的prepared处理这种错误。
而RM re-enlist到TM后，对应的xid已经被删除了，因此需要直接abort

FdieRM、FdieRMAfterPrepare、FdieRMBeforePrepare与上面流程一样


FdieRMBeforeCommit

此时TM已将commit信息写入log，开始执行RM的commit，但是挂了，因此在TM的commit时需要处理异常。
该异常发生时，不应该abort。在TM中，应该等待RM重启后再次enlist到TM时，commit该transaction。
因此，在TM的commit时，正常执行的RM不受影响，挂掉的RM记录下来。如果所有的RM都正常，则正常处理，
删除xid的信息。否则，在enlist时，对新连接的RM进行判断，然后再完成commit的操作。
  
docker exec -it ubuntu bash
cd home/projects/DDB-Project2019/OptionSourceCode/src/transaction
make runrmflights & make runrmrooms & make runrmcars & make runrmcustomers


















### TM异常

