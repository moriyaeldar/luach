create table app_user (
    id          uuid primary key,
    subject     varchar(200) not null unique,
    email       varchar(320),
    name        varchar(200),
    picture_url varchar(1000),
    created_at  timestamp(6) with time zone not null default now()
);

create table household (
    id               uuid primary key,
    name             varchar(100) not null,
    in_israel        boolean      not null default true,
    demo             boolean      not null default false,
    snapshot_version bigint       not null default 0,
    created_at       timestamp(6) with time zone not null default now()
);

create table member (
    id           uuid primary key,
    household_id uuid        not null references household (id) on delete cascade,
    display_name varchar(60) not null,
    color        varchar(9)  not null,
    role         varchar(10) not null,
    user_id      uuid references app_user (id),
    created_at   timestamp(6) with time zone not null default now(),
    constraint member_role_chk check (role in ('ADMIN', 'MEMBER', 'CHILD')),
    constraint member_one_per_user unique (household_id, user_id)
);

create table invite (
    token             varchar(40) primary key,
    household_id      uuid        not null references household (id) on delete cascade,
    member_id         uuid references member (id) on delete cascade,
    display_name      varchar(60),
    role              varchar(10) not null,
    created_by_member uuid        not null,
    expires_at        timestamp(6) with time zone not null,
    accepted_at       timestamp(6) with time zone,
    accepted_by_user  uuid
);

-- Transactional outbox: written in the same transaction as the change, relayed to Kafka afterwards.
create table outbox_event (
    id           uuid primary key,
    household_id uuid         not null,
    type         varchar(40)  not null,
    payload      text         not null,
    created_at   timestamp(6) with time zone not null default now(),
    published_at timestamp(6) with time zone
);

create index outbox_unpublished_idx on outbox_event (created_at) where published_at is null;
