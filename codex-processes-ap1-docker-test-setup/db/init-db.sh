#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE dic_fhir;
    GRANT ALL PRIVILEGES ON DATABASE dic_fhir TO liquibase_user;
    CREATE DATABASE dic_bpe;
    GRANT ALL PRIVILEGES ON DATABASE dic_bpe TO liquibase_user;
    CREATE DATABASE gth_fhir;
    GRANT ALL PRIVILEGES ON DATABASE gth_fhir TO liquibase_user;
    CREATE DATABASE gth_bpe;
    GRANT ALL PRIVILEGES ON DATABASE gth_bpe TO liquibase_user;
    CREATE DATABASE crr_fhir;
    GRANT ALL PRIVILEGES ON DATABASE crr_fhir TO liquibase_user;
    CREATE DATABASE crr_bpe;
    GRANT ALL PRIVILEGES ON DATABASE crr_bpe TO liquibase_user;
EOSQL