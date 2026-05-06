---
title: Сценарии использования
sidebar_position: 1
description: Сценарии использования для Volta с UML диаграммами
---

## Проведение нагрузочного тестирования

Sequence диаграмма по бизнес процессу "Проведение нагрузочного тестирования"

```plantuml
@startuml
actor "QA Engineer" as qa
boundary "Web UI" as ui
participant "AI Analyzer" as ai
database "TimescaleDB" as db
control "Master" as master
participant "Agent" as agent
participant "Target Service" as target

== Запуск теста ==
qa -> ui: Настроить параметры, нажать "Start"
ui -> master: POST /tests/start (config)
master -> master: Валидация параметров
activate master
deactivate master

== Проверка агентов ==
master -> agent: Проверить статус (gRPC)
alt Агент доступен
  agent --> master: Статус READY
else Агент не отвечает (timeout 5s)
  master -> master: Пометить агент DEAD
  master --> ui: "Agent-n недоступен"
  ui --> qa: Отобразить
end

== Генерация нагрузки ==
master -> agent: Запустить LoadEngine
par Нагрузка + Метрики
  agent -> target: Кидать HTTP (Get/Post)
  target --> agent: Responses
else
  agent -> master: Стримить метрики (gRPC)
  master -> db: Записать метрики
end

== Завершение и Анализ ==
agent --> master: "TestFinished"
master -> ai: Отправить метрики на анализ
ai -> ai: Поиск аномалий
activate ai
deactivate ai
ai --> master: Отправить результат
master -> db: Сохранить отчет
master --> ui: Отдать отчет
ui --> qa: Отобразить результаты
@enduml
```

## Использование Volta

UseCase диаграмма для бизнес процесса "Использование Volta"

```plantuml
@startuml

actor "QA Engineer" as qa
actor "Admin" as admin
actor "DevOps" as devops
actor "AI Analyzer" as ai

qa <|-- admin

rectangle "Volta" {

    usecase "Запустить тест" as UC1
    usecase "Авторизоваться" as UC2
    usecase "Просмотреть отчет" as UC3
    usecase "Получить AI рекомендации" as UC4
    usecase "Управлять пользователями" as UC5
    usecase "Подключить агента к кластеру" as UC6
    usecase "Настроить параметры теста" as UC7
    usecase "Экспортировать отчет в PDF" as UC8

    UC1 ..> UC2 : <<include>>
    UC1 ..> UC7 : <<include>>
    UC3 ..> UC2 : <<include>>
    UC3 <.. UC4 : <<extend>>
    UC3 <.. UC8 : <<extend>>
}

qa --> UC1
qa --> UC3
admin --> UC5
devops --> UC6
ai --> UC4

@enduml
```
