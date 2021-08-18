# Testing using Docker Setup

Build the project from the root directory of this repository by executing the following command.

```sh
mvn clean package
```

Add entries to your hosts file
```
127.0.0.1	dic
127.0.0.1	gth
127.0.0.1	crr
```

*Start docker-compose commands from sub-folder:* `codex-processes-ap1/codex-processes-ap1-docker-test-setup`

Console 1: Start DIC HAPI FHIR Server or DIC blaze FHIR Server
```sh
docker-compose up dic-fhir-store-hapi
docker-compose up dic-fhir-store-blaze
```
Access at http://localhost:8080/fhir/

Console 2: Start CRR fhir-bridge Server
```sh
docker-compose up crr-fhir-bridge
```
Access at http://localhost:8888/fhir-bridge/fhir/

Console 3: Start DIC DSF FHIR Server and wait till started
```sh
docker-compose up -d dic-fhir-proxy dic-fhir-app dic-fhir-db && docker-compose logs -f dic-fhir-app
```
Console 3: Disconnect from log output (Ctrl-C) if Server started
Console 3: Start DIC DSF BPE Server
```sh
docker-compose up -d dic-bpe-app dic-bpe-db && docker-compose logs -f dic-fhir-app dic-bpe-app
````

Console 4: Start GTH DSF FHIR Server and wait till started
```sh
docker-compose up -d gth-fhir-proxy gth-fhir-app gth-fhir-db && docker-compose logs -f gth-fhir-app
```
Console 4: Disconnect from log output (Ctrl-C) if Server started
Console 4: Start GTH DSF BPE Server
```sh
docker-compose up -d gth-bpe-app gth-bpe-db && docker-compose logs -f gth-fhir-app gth-bpe-app
````

Console 5: Start CRR DSF FHIR Server and wait till started
```sh
docker-compose up -d crr-fhir-proxy crr-fhir-app crr-fhir-db && docker-compose logs -f crr-fhir-app
```
Console 5: Dicconnect from log output (Ctrl-C) if Server started
Console 5: Start CRR DSF BPE Server
```sh
docker-compose up -d crr-bpe-app crr-bpe-db && docker-compose logs -f crr-fhir-app crr-bpe-app
````

<!--
Webbrowser at http://localhost:8080/fhir/: Add Demo Data to DIC HAPI FHIR Server via Transaction-Bundle at
[dic_fhir_store_demo_psn_create.json](../codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_psn_create.json) or
[dic_fhir_store_demo_bf_create.json](../codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_bf_create.json)
-->

*Start curl commands from root-folder:* `codex-processes-ap1`

Console 6: Execute Demo Transaction-Bundle for HAPI
```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+json" \
-d @codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_psn.json \
http://localhost:8080/fhir
```
or
```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+json" \
-d @codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_bf.json \
http://localhost:8080/fhir
```

Console 6: Execute Demo Transaction-Bundle for blaze
```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+json" \
-d @codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_psn_create.json \
http://localhost:8080/fhir
```
or
```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+json" \
-d @codex-process-data-transfer/src/test/resources/fhir/Bundle/dic_fhir_store_demo_bf_create.json \
http://localhost:8080/fhir
```


Console 6: Start Data Trigger Process at DIC using the following command
```sh
curl -H "Accept: application/xml+fhir" -H "Content-Type: application/fhir+xml" \
-d @codex-process-data-transfer/src/test/resources/fhir/Task/TaskStartDataTrigger.xml \
--ssl-no-revoke --cacert codex-processes-ap1-test-data-generator/cert/ca/testca_certificate.pem \
--cert codex-processes-ap1-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_certificate.pem \
--key codex-processes-ap1-test-data-generator/cert/Webbrowser_Test_User/Webbrowser_Test_User_private-key.pem \
https://dic/fhir/Task
```

Console 6: Check data transfered to CRR fhir-bridge
```sh
curl http://localhost:8888/fhir-bridge/fhir/Patient
curl http://localhost:8888/fhir-bridge/fhir/Condition
curl http://localhost:8888/fhir-bridge/fhir/Observation
```

Console X: Stop everything
```sh
docker-compose down -v
```