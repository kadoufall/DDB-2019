PROJECTROOT = ..
RMIREGPORT = 3345

clean :
	rm -rf ./data/*
	rm -rf ./results/*
	$(RM) *.class

all :
	javac -classpath $(PROJECTROOT) *.java

test :
	#java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAdd test.RunTest
	#java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DataAddAbort test.RunTest
	#java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieAll test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=DieTM test.RunTest