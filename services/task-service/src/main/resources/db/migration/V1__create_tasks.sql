create table task (
    id                 uuid primary key,
    household_id       uuid         not null,
    title              varchar(200) not null,
    notes              varchar(2000),
    assignee_id        varchar(64)  not null,
    schedule_mode      varchar(10)  not null,
    task_date          date,
    start_time         time,
    duration_minutes   integer,
    deadline           date,
    importance         integer      not null default 3,
    status             varchar(10)  not null default 'OPEN',
    recurrence_kind    varchar(20)  not null default 'NONE',
    hebrew_month       varchar(10),
    hebrew_day         integer,
    leap_year_policy   varchar(10),
    missing_day_policy varchar(10),
    week_days          varchar(80),
    month_day          integer,
    recurrence_until   date,
    version            bigint       not null default 0,
    created_at         timestamp(6) with time zone not null default now(),
    updated_at         timestamp(6) with time zone not null default now(),
    constraint task_mode_chk check (schedule_mode in ('FIXED', 'DAY', 'AUTO')),
    constraint task_importance_chk check (importance between 1 and 5)
);

create index task_household_date_idx on task (household_id, task_date);

-- Completion of one occurrence of a recurring task (non-recurring tasks use task.status).
create table task_completion (
    task_id         uuid not null references task (id) on delete cascade,
    occurrence_date date not null,
    completed_at    timestamp(6) with time zone not null default now(),
    primary key (task_id, occurrence_date)
);
