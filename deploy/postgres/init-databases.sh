#!/bin/sh
# Runs once, when the Postgres volume is first created.
# One server, one database and one user per service: a service can only reach its own data.
set -e

create_service_db() {
  name="$1"; password="$2"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<SQL
CREATE USER ${name} WITH PASSWORD '${password}';
CREATE DATABASE ${name}_db OWNER ${name};
REVOKE ALL ON DATABASE ${name}_db FROM PUBLIC;
SQL
}

create_service_db tasks "${TASKS_DB_PASSWORD:?TASKS_DB_PASSWORD is required}"
create_service_db households "${HOUSEHOLDS_DB_PASSWORD:?HOUSEHOLDS_DB_PASSWORD is required}"
