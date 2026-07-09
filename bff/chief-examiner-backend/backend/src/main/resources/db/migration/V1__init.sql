-- Chief Examiner v0.1 — evidence reading schema

create table script (
    id            uuid primary key,
    file_name     varchar(255) not null,
    size_bytes    bigint       not null,
    storage_path  varchar(500) not null,
    paper_id      varchar(64),            -- e.g. 0580_s25_22 (calendar pipeline id)
    exam          varchar(160),           -- e.g. "Mathematics P2 — June 2025"
    paper         varchar(64),            -- e.g. "0580/22 · 2h"
    slot          varchar(32),            -- e.g. "07:00–09:00"
    day_name      varchar(16),            -- e.g. "MONDAY"
    date_label    varchar(32),            -- e.g. "6 JUL 2026"
    uploaded_by   varchar(160),
    uploaded_at   timestamptz  not null default now(),
    status        varchar(16)  not null default 'UPLOADED',  -- UPLOADED|READING|READ|FAILED
    page_count    int,
    error_message varchar(1000)
);

create table page_transcript (
    id           uuid primary key,
    script_id    uuid not null references script(id) on delete cascade,
    page_number  int  not null,
    transcript   text not null,
    confidence   numeric(3,2),            -- 0.00–1.00, model self-estimate
    illegible    boolean not null default false,
    created_at   timestamptz not null default now(),
    unique (script_id, page_number)
);

create table question_segment (
    id           uuid primary key,
    script_id    uuid not null references script(id) on delete cascade,
    label        varchar(24) not null,    -- e.g. "1(a)", "3(b)(ii)"
    page_start   int not null,
    page_end     int not null,
    transcript   text not null,
    confidence   numeric(3,2),
    flags        varchar(500),            -- e.g. "partially illegible; crossed-out working"
    seq          int not null,            -- order of appearance in the script
    created_at   timestamptz not null default now()
);

create index idx_segment_script on question_segment(script_id, seq);
create index idx_page_script on page_transcript(script_id, page_number);
