PROJECTROOT = ..
RMIREGPORT = 3345

clean :
	rm -rf ./data/*
	rm -rf ./results/*
	$(RM) *.class

all :
	javac -classpath $(PROJECTROOT) *.java

test :
#Basic Test:
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Start test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=StartCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=StartAbort test.RunTest

#ACID Test:
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Atomicity test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Consistency test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Isolation test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Durability test.RunTest

#Function Test
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAdd test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddAbort test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddCommitReserve test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=AddDelete test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=AddCommitDeleteCommit test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforDataAddCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforDataAddAbort test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=AddChange test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforBill test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforAdd test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=BadXid test.RunTest

#Die Test:	
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRM test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMAfterEnlist test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMAfterPrepare test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMBeforeAbort test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMBeforeCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMBeforePrepare test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieTM test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieTMBeforeCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieTMAfterCommit test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieWC test.RunTest

	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieAll test.RunTest

#Lock Test:
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Lconc test.RunTest









