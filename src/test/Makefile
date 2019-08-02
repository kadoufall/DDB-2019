PROJECTROOT = ..
RMIREGPORT = 3345

clean :
	rm -rf ./data/*
	rm -rf ./results/*
	$(RM) *.class

all :
	javac -classpath $(PROJECTROOT) *.java

test :
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Start test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=StartAbort test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=StartCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAdd test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddAbort test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddCommitReserve test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforDataAddCommit test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=QueryforDataAddAbort test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieAll test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieTM test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMAfterEnlist test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieRMBeforePrepare test.RunTest