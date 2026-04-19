# Handoff

Этот файл нужен, чтобы быстро продолжить работу над `RepFlow` на другом компьютере или в новой сессии с агентом.

## Что Это За Проект
- Android-приложение `RepFlow`
- package / namespace / applicationId: `com.vaslit.repflow`
- стек: Kotlin, Jetpack Compose, Room, Navigation Compose, ViewModel + Flow
- репозиторий: `git@github.com:vaslit/repflow.git`

## Что Уже Реализовано
- выбор программы: подтягивания / отжимания;
- стартовая оценка уровня;
- генерация 6-недельного плана;
- контрольные тесты внутри плана;
- пошаговая тренировка с паузами между подходами;
- сохранение фактических результатов;
- история тренировок;
- экран техники;
- unit-тесты на `AssessmentEngine` и `PlannerEngine`.

## Важные Пути
- [README.md](/home/vase/Projects/repflow/README.md)
- [AGENTS.md](/home/vase/Projects/repflow/AGENTS.md)
- [CONTRIBUTING.md](/home/vase/Projects/repflow/CONTRIBUTING.md)
- [DomainModels.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/domain/DomainModels.kt)
- [TrainingCatalog.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/domain/TrainingCatalog.kt)
- [PlannerEngine.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/domain/PlannerEngine.kt)
- [AppRepository.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/data/AppRepository.kt)
- [RepFlowApp.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/ui/RepFlowApp.kt)
- [MainActivity.kt](/home/vase/Projects/repflow/app/src/main/java/com/vaslit/repflow/MainActivity.kt)

## Как Поднять На Другом Компьютере
1. Клонировать репозиторий:
```bash
git clone git@github.com:vaslit/repflow.git
cd repflow
```
2. Открыть проект в Android Studio.
3. Убедиться, что установлен Android SDK.
4. Создать локальный `local.properties`, если IDE не сделала это сама:
```properties
sdk.dir=/path/to/Android/Sdk
```
5. Запустить сборку:
```bash
./gradlew testDebugUnitTest assembleDebug
```

## Что Важно Знать Про Запуск На Телефоне
- `./gradlew app:installDebug` работает только если телефон или emulator виден через `adb`.
- В прошлой сессии сборка проходила, но установка не шла, потому что телефон был подключен к другому компьютеру, а не к машине, где запускался `adb`.

## Правила Для Следующего Агента
- использовать Conventional Commits;
- использовать SemVer-теги `vX.Y.Z`;
- не пушить сырой код без релевантной проверки;
- для ручных локальных запусков учитывать, что `local.properties` не хранится в git;
- package name должен оставаться `com.vaslit.repflow`, если отдельно не принято другое решение.

## Следующие Полезные Задачи
- улучшить UX стартового теста и экрана тренировки;
- добавить больше техники и пояснений по переходам между уровнями;
- подготовить release signing и нормальный release build;
- добавить instrumentation/UI tests;
- оформить первый стабильный релиз после ручной проверки на реальном Android-устройстве.

## Быстрый Промпт Для Новой Сессии С Агентом
```text
Открой HANDOFF.md, README.md и AGENTS.md. Это Android-проект RepFlow на Kotlin/Compose с package com.vaslit.repflow. Продолжаем разработку из текущего состояния репозитория. Сначала проверь git status, затем опиши текущее состояние проекта и предложи следующий минимальный полезный шаг.
```
