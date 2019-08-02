PROJECTROOT = ..
RMIREGPORT = 3345

clean :
	rm -rf ./data/*
	rm -rf ./results/*
	$(RM) *.class

all :
	javac -classpath $(PROJECTROOT) *.java

test :
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Badd test.RunTest
	java -classpath $(PROJECTROOT) -DrmiPort=$(RMIREGPORT) -DtestName=Baddabt test.RunTest