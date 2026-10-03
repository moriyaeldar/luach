-- Milestone 1 ran a single demo family; from milestone 2 families come from the Household service.
delete from task where household_id = '00000000-0000-0000-0000-000000000001';

-- Local copy of families and members, kept up to date from Kafka events (and fetched on demand as a fallback).
create table household_replica (
    id         uuid primary key,
    name       varchar(100) not null,
    in_israel  boolean      not null,
    demo       boolean      not null,
    version    bigint       not null,
    updated_at timestamp(6) with time zone not null default now()
);

create table member_replica (
    id           uuid primary key,
    household_id uuid        not null references household_replica (id) on delete cascade,
    display_name varchar(60) not null,
    color        varchar(9)  not null,
    role         varchar(10) not null,
    user_subject varchar(200)
);

create index member_replica_household_idx on member_replica (household_id);

-- Each Kafka event is processed once, even if it is delivered again.
create table processed_event (
    id           uuid primary key,
    processed_at timestamp(6) with time zone not null default now()
);

-- Demo families get a sample week exactly once.
create table demo_seed (
    household_id uuid primary key,
    seeded_at    timestamp(6) with time zone not null default now()
);
