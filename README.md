# Denis map-test AWS Supplier 2018
Event supplier for Test map application developed with AWS SQS (Simple Queue Service) and SNS (Simple Notification Service).

A small application that generates around 20000 risk areas (Heat map) events updated every 10 seconds.

The risk areas are transmitted asynchronously via AWS SNS and SQS services and consumed by the companion application "Denis map-test AWS Consumer".

*server command line:
=====================
./denis-map-test_aws_supplier-1.0.0/bin/denis-map-test_aws_supplier {inputFile} {fullWorkWindow} {sleepInterval}
<br>*inputFile = Name of the file containing the risk areas locations. Default: ./files/test.csv
<br>*fullWorkWindow = Time window in seconds to keep emitting events after being notifed of a new consumer. Default: 120
<br>*sleepInterval = Interval in seconds between event emissions. Default: 10
<br>*example: ./denis-map-test_aws_supplier-1.0.0/bin/denis-map-test_aws_supplier ./files/test1.csv 120 10
