docker-compose up - запустить кафка кластер и спарк кластер

./create-topics.sh - создание топиков

sbt assembly - собрать jar файл для сабмита

./deploy_log-parser_standalone.sh команда сабмита jar файла в кластер

Для мониторинга поступающих сообщений в топики
docker exec -it kafka-cluster kafka-console-consumer --topic log-parser-input-topic --bootstrap-server localhost:9092

docker exec -it kafka-cluster kafka-console-consumer --topic log-parser-output-aggregation-topic --bootstrap-server localhost:9092

docker exec -it kafka-cluster kafka-console-consumer --topic log-parser-output-alert-topic --bootstrap-server localhost:9092

тестовое приложение для генерации логов
sbt "test:runMain com.michael.log.parser.SampleLogsEmitter"