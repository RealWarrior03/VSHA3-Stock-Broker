# How to execute our code
- mvn package 
- ignore the warnings, they are expected and don't influence the program in a negative way
- you have to have activemq running with tcp://localhost:61616 as a transport connector
- then you can run the program with
- java -jar ./target/broker.jar
- java -jar ./target/client.jar