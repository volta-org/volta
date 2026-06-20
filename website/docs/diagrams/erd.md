---
title: ERD
sidebar_position: 3
description: ERD диаграммы для БД на Мастере
---

## Концептуальная модель

```
User ||-----0{ Test

TestConfig ||-----|| Test

Test ||-----0{ TestStats

Test ||-----0{ TestAnalysis
```

## Логическая модель

Везде enum вместо справочника, потому что контракт фиксирован.

Помечены общие типы (потому что dbdiagram вообще без типа не позволяет указать)

```Plain Text
Enum user_rights {
  VIEWER
  EDITOR
  ADMIN
}

Enum load_type {
  CONSTANT
  SPIKE
  GRADUAL
}

Enum test_status {
  RUNNING
  FINISHED
  ERROR
  STOPPED
}

Enum analysis_status {
  PROCESSING
  COMPLETED
}

Table users {
  user_id       id          [pk]
  login         string      [unique, not null, note: 'бизнес-ключ']
  email         string      [unique, not null, note: 'бизнес-ключ']
  password_hash string      [not null]
  rights        user_rights [not null]
  name          string      [not null]
  surname       string      [not null]
}

Table test_configs {
  config_id     id         [pk]
  rps           integer    [not null]
  duration      integer    [not null]
  target_url    string     [not null]
  load_type     load_type  [not null]
}

Table tests {
  test_id         id          [pk]
  config_id       id          [not null, ref: - test_configs.config_id]
  created_by      id          [not null, ref: > users.user_id]
  status          test_status [not null]
  total_requests  integer     [not null]
  total_errors    integer     [not null]
  p50             integer
  p95             integer
  p99             integer
  created_at      datetime    [not null]
  finished_at     datetime
}

Table test_params_history {
  id            id       [pk]
  test_id       id       [not null, ref: > tests.test_id]
  rps           integer  [not null]
  changed_at    datetime [not null]
  changed_by    id       [not null, ref: > users.user_id]
}


Table test_stats {
  test_id         id       [not null, ref: > tests.test_id]
  timestamp       datetime [not null]
  rps             float    [not null]
  errors_per_sec  float    [not null]
  latency         float    [not null]
  p50             float    [not null]
  p95             float    [not null]
  p99             float    [not null]
}

Table test_analysis {
  id              id               [pk]
  test_id         id               [unique, not null, ref: - tests.test_id]
  status          analysis_status  [not null]
  summary         text
  anomalies       json
  recommendations json
  created_at      datetime         [not null]
  updated_at      datetime         [not null]
}
```

## Физическая модель

```Plain Text
Enum user_rights {
  VIEWER
  EDITOR
  ADMIN
}

Enum load_type {
  CONSTANT
  SPIKE
  GRADUAL
}

Enum test_status {
  RUNNING
  FINISHED
  ERROR
  STOPPED
}

Enum analysis_status {
  PROCESSING
  COMPLETED
}

Table users {
  user_id       uuid          [pk]
  login         varchar(64)   [unique, not null]
  email         varchar(255)  [unique, not null]
  password_hash varchar(255)  [not null]
  rights        user_rights   [not null]
  name          varchar(128)  [not null]
  surname       varchar(128)  [not null]

  indexes {
    login
    email
  }
}

Table test_configs {
  config_id     uuid         [pk]
  rps           integer      [not null]
  duration_sec  integer      [not null]
  target_url    text         [not null]
  load_type     load_type    [not null]
}

Table tests {
  test_id         uuid          [pk]
  config_id       uuid          [not null, ref: - test_configs.config_id]
  created_by      uuid          [not null, ref: > users.user_id]
  status          test_status   [not null]
  total_requests  bigint        [not null]
  total_errors    bigint        [not null]
  p50_ms          float8
  p95_ms          float8
  p99_ms          float8
  created_at      timestamptz   [not null]
  finished_at     timestamptz

  indexes {
    created_by
    status
    created_at
  }
}

Table test_params_history {
  id            uuid         [pk]
  test_id       uuid         [not null, ref: > tests.test_id]
  rps           integer      [not null]
  thread_count  integer      [not null]
  changed_at    timestamptz  [not null]
  changed_by    uuid         [not null, ref: > users.user_id]

  indexes {
    test_id
    changed_at
  }
}

Table test_stats {
  test_id         uuid         [not null, ref: > tests.test_id]
  timestamp       timestamptz  [not null]
  rps             integer      [not null]
  errors_per_sec  integer      [not null]
  latency_ms      integer      [not null]
  p50_ms          integer      [not null]
  p95_ms          integer      [not null]
  p99_ms          integer      [not null]

  indexes {
    (test_id, timestamp) [pk]
  }
}

Table test_analysis {
  id              uuid             [pk]
  test_id         uuid             [unique, not null, ref: - tests.test_id]
  status          analysis_status  [not null]
  summary         text
  anomalies       jsonb
  recommendations jsonb
  created_at      timestamptz      [not null]
  updated_at      timestamptz      [not null]

  indexes {
    test_id
  }
}
```

uuid вместо serial для PK
- Решил использовать uuid везде, потому что система агенты и мастер работают независимо. Если бы был serial, при одновременной вставке с разных агентов могли бы быть онфликты.

timestamptz вместо timestamp
- Безопаснее, так как бд и пользователь могут быть в разных часовых поясах.

anomalies и recommendations - jsonb
- Поля формируются ИИ, структура может меняться, поэтому делать отдельную таблицу для строик небезопасно и избыточно.

test_params_history - отдельная таблица
- Если не логировать изменения теста в runtime - при анализе результатов непонятно почему в какой-то момент метрики резко изменились.

bigint для total_requests и total_errors
- Если допустить, что RPS ~ 50K и тест на 30 минут - это 90 миллионов запросов. в integer влезет, но для безопасности (вдруг длительность теста будет сутки) лучше bigint.
