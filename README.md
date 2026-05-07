# Newton Field App

Android-приложение для полевых геодезических работ с приёмником «Ньютон». Offline-first, под Российский рынок, ориентировано на совместимость с ГСК-2011, СК-42, СК-95 и стандартные форматы DXF/CSV для передачи результатов в офис.

---

## С чего начать

1. **Установите инструменты:**
   - JDK 21 (Temurin или аналог)
   - Android Studio последней стабильной версии (Narwhal 2025.3+ или новее)
   - Android SDK: platform-tools, build-tools, SDK 36
   - Claude Code CLI (для AI-ассистированной разработки)

2. **Откройте проект:**
   ```bash
   git init
   git add .
   git commit -m "chore: initial import"
   ```
   Откройте папку в Android Studio → `File → Open`. Студия сама запустит Gradle sync.

3. **Запустите Claude Code в корне проекта:**
   ```bash
   cd /path/to/newton-field-app
   claude
   ```
   Claude автоматически прочитает `CLAUDE.md` — там записан весь контекст проекта, архитектурные правила, стиль и текущий спринт.

4. **Откройте sprint plan:**
   - `docs/sprint-plan.md` — активный спринт и задачи.
   - Первая задача спринта 1: `INFRA-001` (настройка Gradle/Hilt/Compose). Проектный скелет уже есть — нужно убедиться, что проект собирается, и пройтись по минорным доработкам.

---

## Документация

Внутри репозитория:

| Файл | Что там |
|---|---|
| `CLAUDE.md` | Контекст для Claude Code: стек, архитектура, конвенции, что нельзя делать |
| `docs/architecture.md` | Модульная структура, слои, диаграммы потоков данных |
| `docs/protocol-newton.md` | Полный справочник по командам «Ньютона» (из прошивочного мануала) |
| `docs/conventions.md` | Нейминг, стиль Kotlin/Compose, git workflow |
| `docs/crs.md` | Системы координат, Helmert параметры, геоиды |
| `docs/sprint-plan.md` | План MVP по 8 двухнедельным спринтам |

Полная документация (для людей, не для Claude) — в сопровождающих файлах:
- `Newton_FieldApp_DevHandbook_v1.docx` — полный dev handbook (~60–80 страниц)
- `Newton_FieldApp_ProductReview_v1.docx` — продуктовый обзор для PM
- `Newton_FieldApp_ScreenMap_v6.xlsx` — полная карта 160 экранов с приоритетами MVP

---

## Работа с Claude Code

В проекте настроены специализированные агенты и slash-команды. Примеры типовых запросов:

```
# Получить экспертный разбор протокольного кода
> Попроси newton-protocol-expert проверить :gnss:command

# Создать скелет нового экрана
> /new-screen SET-010

# Сгенерировать парсер новой NMEA-строки с тестом
> /gen-nmea-parser GPVTG

# Аудит протокольных ошибок в кодовой базе
> /check-protocol

# Статус текущего спринта
> /sprint-status

# Написать тесты для нового функционала
> Попроси test-writer покрыть тестами :domain/usecase/SurveyPointUseCase
```

Полный перечень — в `.claude/agents/` и `.claude/commands/`.

---

## Правила работы (короткая версия)

1. Ни один `:features:*` модуль не зависит от другого `:features:*`. Общий код → в `:domain` или `:core:ui`.
2. `:domain` — чистый Kotlin. Никаких `import android.*` / `import androidx.*`.
3. Команды «Ньютону» ВСЕГДА через `NewtonCommandBuilder`. Прямая конкатенация строк запрещена.
4. Разделитель источников — `NewtonCommandBuilder.SRC_SEP` (U+2502), никогда не `|`.
5. RTCM от NTRIP пишется в `@CommandSpp`, не `@DataSpp`.
6. Все настройки приёмника идут через `PendingChangesService` → `CommandQueue` → `ApplyReceiverConfigUseCase`. Прямого вызова `CommandSession.send` вне этого пайплайна быть не должно.
7. Новый NMEA-парсер = фикстура в `src/test/resources/fixtures/` + unit-тест. Без исключений.
8. Room-миграция пишется с первого дня, даже в MVP. Используйте `/add-migration`.
9. Никогда не коммитьте секреты (пароли NTRIP, API-ключи).
10. KSP, не KAPT. kotlinx.serialization, не Moshi/Gson. StateFlow, не LiveData.

Полная версия — в `CLAUDE.md` → секция «What NOT to do».

---

## Сборка и тесты

```bash
# Сборка debug-APK
./gradlew :app:assembleDebug

# Все unit-тесты
./gradlew testDebugUnitTest

# Тесты одного модуля
./gradlew :gnss:data:test

# Стиль
./gradlew ktlintCheck

# Android Lint
./gradlew lint

# Установка на подключённое устройство
./gradlew :app:installDebug
```

Перед пушем:

```bash
./gradlew ktlintCheck testDebugUnitTest lint
```

---

## Структура проекта

```
newton-field-app/
├── app/                           # application module — 3 tabs, nav, DI wiring
├── core/
│   ├── common/                    # result types, formatters — pure Kotlin
│   ├── ui/                        # design system, reusable composables
│   ├── logging/                   # AppLog with categories
│   └── bluetooth/                 # SPP transport (x2 channels)
├── gnss/
│   ├── data/                      # NMEA parsers, GnssStatusStore
│   ├── command/                   # Newton protocol, command builder, session
│   └── ntrip/                     # NTRIP 2.0 client on OkHttp
├── crs/                           # coordinate systems, geoids — pure Kotlin
├── domain/                        # use cases, sealed states, repo interfaces
├── data/                          # Room, DAOs, repository implementations
└── features/
    ├── project/                   # "Проект" tab: projects, CRS, CAD, export
    ├── settings/                  # "Настройки" tab: connection, rover, NTRIP
    ├── survey/                    # "Съёмка" tab: map, point/line, stakeout
    ├── cad/                       # CAD view, DXF handling
    └── export/                    # import/export wizards
```

Зависимости между слоями показаны в `docs/architecture.md`.

---

## Лицензия

Проект внутренний. Лицензионные условия будут определены отдельно при релизе.
