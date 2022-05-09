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

Console 2: Start CRR fhir-bridge server
```sh
docker-compose up crr-fhir-bridge
```
Access at http://localhost:8888/fhir-bridge/fhir/

Console 3: Start DIC DSF FHIR Server and wait till started
```sh
docker-compose up -d dic-fhir && docker-compose logs -f dic-fhir
```
Console 3: Disconnect from log output (Ctrl-C) if server started
Console 3: Start DIC DSF BPE Server
```sh
docker-compose up -d dic-bpe && docker-compose logs -f dic-fhir dic-bpe
````

Console 4: Start GTH DSF FHIR Server and wait till started
```sh
docker-compose up -d gth-fhir && docker-compose logs -f gth-fhir
```
Console 4: Disconnect from log output (Ctrl-C) if server started
Console 4: Start GTH DSF BPE Server
```sh
docker-compose up -d gth-bpe && docker-compose logs -f gth-fhir gth-bpe
````

Console 5: Start CRR DSF FHIR Server and wait till started
```sh
docker-compose up -d crr-fhir && docker-compose logs -f crr-fhir
```
Console 5: Dicconnect from log output (Ctrl-C) if server started
Console 5: Start CRR DSF BPE Server
```sh
docker-compose up -d crr-bpe && docker-compose logs -f crr-fhir crr-bpe
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