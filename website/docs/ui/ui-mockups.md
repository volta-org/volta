---
title: UI макеты Web интерфейса
sidebar_position: 1
description: UI макеты Web интерфейса и роуты для Volta
---

Макеты:

[https://unidraw.io/app/board/ae6343e5b7e4864c61db?allow_guest=true](https://unidraw.io/app/board/ae6343e5b7e4864c61db?allow_guest=true)

Таблица эндпоинтов:

```
| Роут экрана          | Действие                       | Метод + Эндпоинт             |
|----------------------|--------------------------------|------------------------------|
| /tests/new           | Нажатие на кнопку "Start test" | POST /api/tests              |
| /tests               | При открытии экрана            | GET /api/tests               |
| /tests               | Даблклик на поле               | GET /api/tests/{id}          |
| /users               | При открытии экрана            | GET /api/users               |
| /users               | Даблклик на поле               | GET /api/users/{id}          |
| /users/{id}          | Нажатие на кнопку "Save"       | PATCH /api/users/{id}        |
| /users/new           | Нажатие на кнопку "Save"       | POST /api/users              |
| /users/{id}          | Нажатие на кнопку "Delete"     | DELETE /api/users/{id}       |
| /tests/{id}/runtime  | При открытии экрана            | GET /api/tests/{id}          |
| /tests/{id}/runtime  | Каждую секунду                 | GET /api/tests/{id}/stats    |
| /tests/{id}/analysis | При открытии экрана            | GET /api/tests/{id}/analysis |
```
