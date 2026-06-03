# Newton · Field Blue — Design System

> **Спецификация визуального стиля для Newton Field App** (Android · Jetpack Compose).
> Референс-мокапы: `newton-field-blue.html` (приложен отдельно).
> Использовать как авторитетный источник при генерации UI-кода.

---

## 1. Философия и характер

Приложение для геодезистов в поле. Стиль наследует **картографическую эстетику** — то, что узнаётся в топографических картах, навигационных приборах, GPS-планшетах. Чистота, технический язык, фирменная узнаваемость.

**Три принципа:**
1. **Field-ready** — крупные числа, высокая читаемость под солнцем, моноширные цифры для координат, дробных значений, метрик.
2. **Brand-recognizable** — компасная роза, монохромная синяя палитра, картографические hairline-границы — узнаваемо с одного взгляда.
3. **Information density without clutter** — белые поверхности, hairline-разделители, разреженная типографика, никаких декоративных шумов.

Это **НЕ** Material You / Material 3 Expressive. Это нечто между утилитарным военным/инженерным UI и современной чистой типографикой. Скорее Garmin/Trimble field-software с современным touch.

---

## 2. Цветовая система

Монохромная синяя палитра. Без cyan/teal (это цвет конкурентов). Единственное исключение — cyan-blue градиенты во *внутренних* SVG-элементах (стрелка компаса, прогресс-кольцо), потому что там нужна светимость — на общую идентичность не влияет.

### 2.1 Brand-blues

| Токен | Hex | Назначение |
|---|---|---|
| `brand` | `#1A5DD8` | основной — кнопки, активные индикаторы, основные иконки |
| `brand-deep` | `#0E2A57` | navy — крупный текст, заголовки, hero-фоны, тёмные иконки |
| `brand-mid` | `#4F7FCE` | промежуточный синий — редкие декоративные акценты |
| `brand-soft` | `#DCE7FB` | светло-голубой — фон иконок, активные пилюли, контейнеры |
| `brand-faint` | `#EEF3FC` | очень светлый — фон accent-плиток |
| `brand-glow` | `rgba(26,93,216,0.22)` | тень/glow под кнопками |

### 2.2 Surface

| Токен | Hex | Назначение |
|---|---|---|
| `bg` | `#F5F8FC` | фон страницы (за плитками) |
| `surface` | `#FFFFFF` | карточки, плитки, top-bar |
| `surface-tint` | `#F7FAFD` | вспомогательные поверхности |
| `surface-deep` | `#EFF3FA` | hover-состояния, фоны переключателей |

### 2.3 Text

| Токен | Hex/RGBA | Назначение |
|---|---|---|
| `text` | `#0E2A57` | основной текст (=brand-deep) |
| `text-2` | `rgba(14,42,87,0.66)` | вторичный — подписи, мета |
| `text-3` | `rgba(14,42,87,0.42)` | третичный — placeholder, dim labels |

### 2.4 Hairlines

| Токен | RGBA | Назначение |
|---|---|---|
| `hairline-soft` | `rgba(14,42,87,0.06)` | едва видимые |
| `hairline` | `rgba(14,42,87,0.10)` | стандартные границы карточек, плиток |
| `hairline-strong` | `rgba(14,42,87,0.22)` | акцентированные границы |

### 2.5 Semantic

| Токен | Hex | Назначение |
|---|---|---|
| `success` | `#16A35B` | RTK Fix, успешные операции |
| `success-soft` | `#D8F1E2` | bg для success-пилюль |
| `warning` | `#D08400` | Float, несохранённые изменения |
| `warning-soft` | `#FCEDD0` | bg для warning-баннеров |
| `error` | `#C0273D` | критические ошибки |

### 2.6 Правило применения

- **brand** — только для активных интерактивных элементов (primary buttons, активные таб-пилюли, основные иконки).
- **brand-deep** — крупный typography (числа, заголовки), глубокие hero-фоны, тёмные контейнеры иконок в outlined-стиле.
- **brand-soft** — заполнение контейнеров иконок, активные тонкие пилюли, легкие подложки.
- **brand-faint** — отличительный фон для accent-плиток.
- **success** — только статусы (RTK Fix dot, маркер-полоска status-card).
- **warning** — только баннеры о несохранённых изменениях, Float-точки.

Никогда: cyan, teal, фиолетовый, розовый как акценты. Только синие оттенки + семантические (зелёный/жёлтый/красный) для статусов.

---

## 3. Типографика

### 3.1 Шрифты

- **UI**: `Inter` (Google Fonts) — все веса 400–800. Fallback: system-ui, -apple-system.
- **Числа/моноширный**: `JetBrains Mono` — для координат, метрик, дистанций, дельт, σ-значений, азимутов. Веса 500–700. Всегда с `font-feature-settings: 'tnum' 1` (tabular figures — одинаковая ширина цифр).

В Compose: подключить через GoogleFont API. `GoogleFont("Inter")`, `GoogleFont("JetBrains Mono")`.

### 3.2 Шкала

| Роль | Размер | Вес | Letter-spacing | Пример |
|---|---|---|---|---|
| Display-XL | 40px | 700 | -0.025em | Большие дистанции (Stakeout `2.347 м`) |
| Display-L | 30px | 800 | -0.025em | Page title h1 (на promo) |
| Display-M | 24px | 800 | -0.022em | Hero tile label (`Снять точку`) |
| Title-L | 22px | 700 | -0.01em | Заголовки модальных |
| Title-M | 17px | 700 | -0.005em | Top-bar title |
| Title-S | 15px | 700 | 0 | Project name, list-tile title |
| Body | 14px | 500-600 | 0 | Основной текст, form-labels |
| Body-S | 13px | 500-600 | 0 | Метаданные |
| Caption | 12px | 500 | 0.01em | Подзаголовки, sub-labels |
| Micro-cap | 11px | 800 | 0.14–0.18em uppercase | Section labels, статусы (`RTK FIX`) |
| Mono-XL | 40px JBM | 700 | -0.025em | Дистанция, эпохи прогресса |
| Mono-L | 22px JBM | 700 | -0.01em | Metric pill values |
| Mono-M | 14–15px JBM | 600 | 0 | Координаты в live-card, в списке точек |
| Mono-S | 11–12px JBM | 600–700 | 0.02em | Тех. мета (SAT 18 · HDOP 0.8) |

### 3.3 Правила

- Числа, метрики, измерения, time, тех-параметры → **всегда** моноширный.
- Названия, описания, кнопки, лейблы → Inter.
- ALL CAPS используется только для micro-cap (≤11px, letter-spacing ≥0.14em). Не для крупного текста.
- Bold (700–800) допустим в крупном тексте display/title. В body — не выше 600.

---

## 4. Spacing, радиусы, тени

### 4.1 Spacing (в dp/px)

База — 4. Используемые значения: `4, 8, 10, 12, 14, 16, 18, 20, 24, 32`. 

Стандарт промежутков:
- Между крупными секциями экрана: `14–16dp`
- Между плитками в гриде: `10dp`
- Padding внутри карточки: `14–16dp` по горизонтали, `12–16dp` по вертикали
- Padding screen-edge: `18dp` (узкий — мобильный лист контента)

### 4.2 Радиусы

| Токен | Px | Применение |
|---|---|---|
| `radius-pill` | 999px | Пилюли, статусы, FAB, кнопки-капсулы |
| `radius-card-lg` | 18px | Тайлы, status-card, distance-card |
| `radius-card` | 16px | Form-rows, list-tiles, общие карточки |
| `radius-input` | 14px | Text-field, form-row, settings-rows |
| `radius-icon-cap` | 12–13px | Контейнер иконки в плитке (rounded square) |
| `radius-icon-cap-lg` | 16px | Контейнер иконки в hero-tile |
| `radius-phone` | 44–48px | Phone frame (только в мокапах) |

### 4.3 Тени

Минимум. Карточки преимущественно с hairline-border, без теней. Тени только для:
- **FAB / primary button**: `0 4px 12px brand-glow + inset 0 1px 0 rgba(255,255,255,0.20)`
- **Hero tile**: `0 6px 18px rgba(26,93,216,0.28)`
- **Hero icon container**: `0 4px 14px rgba(14,42,87,0.20) + inset 0 0 0 1px rgba(255,255,255,0.30)`
- **Tile-badge**: `0 1px 2px rgba(0,0,0,0.08)`

Запрещены: blur 30+px, мягкие drop shadows на обычных карточках, glow эффекты на больших поверхностях, neumorphism.

---

## 5. Иконография

- **Семейство**: Material Symbols Outlined (Google Fonts). В Compose: `androidx.compose.material.icons.outlined.*` или Material Symbols через `androidx.compose.material:material-icons-extended`.
- **Стиль строго outlined** — никаких filled/rounded/sharp.
- **Размеры**: 22dp inline · 24dp в плитках · 30dp в hero · 18dp в кнопках · 16dp в status-bar.
- **Цвет**: наследует от parent (`tint = brand` или `tint = brand-deep` в зависимости от контекста).

### 5.1 Соответствия для основных экранов

| Действие | Material Symbol |
|---|---|
| Снять точку | `add_location_alt` |
| Линия | `timeline` |
| Вынос | `flag` |
| Карта | `map` |
| Точки | `apps` |
| CAD/DXF | `architecture` |
| Приёмник | `satellite_alt` |
| NTRIP | `cloud` (или `cloud_download` в list-tile) |
| Проекты | `folder_open` |
| Антенна | `straighten` |
| Search | `search` |
| Settings | `settings` |
| RTK Fix dot | (без иконки — цветной кружок) |

---

## 6. Компонентные паттерны

### 6.1 Плитки (Tiles) — 4 уровня

Грид 3×N. `grid-template-columns: repeat(3, 1fr); gap: 10dp`. Минимум `min-height: 96dp`. У всех `border-radius: 18dp`.

#### Hero (2×2 — главное действие, «Снять точку»)
- BG: `linear-gradient(135deg, #1A5DD8 0%, #0E3F95 60%, #0E2A57 100%)`
- Декорация: концентрические радиальные кольца справа сверху (топо-эффект, opacity 0.05–0.10)
- Иконка-контейнер: `54dp`, `brand-soft` фон, `brand-deep` глиф, тень
- Label-big: `24dp`, weight 800, цвет белый
- Sub-label: `10dp` uppercase, opacity 0.72, с пульсирующей точкой слева

#### Accent (Линия / Вынос — частые операции)
- BG: `brand-faint` (`#EEF3FC`)
- Border: `1.5dp solid brand`
- Иконка-контейнер: solid `brand` фон, `brand-soft` глиф
- Label: `brand-deep`

#### Data (Карта / Точки / CAD)
- BG: `surface` (белый)
- Border: `1dp solid hairline`
- Иконка-контейнер: `brand-soft` фон, `brand` глиф (filled-стиль)
- Label: `brand-deep`
- Опционально: badge в правом верхнем углу (число точек, e.g. «142»)

#### System (Приёмник / NTRIP / Проекты)
- BG: `surface` (белый)
- Border: `1dp solid hairline`
- Иконка-контейнер: `transparent` фон, `1.5dp solid brand` обводка, `brand` глиф (outlined-стиль)
- Label: `brand-deep`

**Принцип:** filled (Data) vs outlined (System) визуально группирует разные категории действий, не делая ни одну группу «менее важной».

### 6.2 Status-card

Карточка статуса GNSS в верхней части экрана.
- BG: `surface`
- Border: `1dp solid hairline-strong`
- **Левая marker-полоска**: `4dp`, `border-radius: 0 4dp 4dp 0`, цвет `success` (когда RTK Fix). При Float — `warning`, при offline — `text-3`.
- **Watermark в правом углу**: SVG-роза компаса, opacity 0.07, размер ~90×90dp, off-canvas (negative inset).
- Содержимое: top-row с fix-pill + sc-meta (моноширный SAT/HDOP/age), затем sc-project (название объекта), затем mini-coords (моноширные текущие координаты).
- **Fix-pill**: только текст `RTK FIX`, uppercase, 11dp, weight 800, letter-spacing 0.14em, цвет `success`, с зелёной точкой слева и пульсирующим pinging кольцом (CSS `animation: ping 2s infinite`).

### 6.3 Pill-row (Metric pills)

Три равных капсулы под status-card.
- Внутри: micro-cap label сверху, mono-L число снизу.
- Базовый: `surface` bg, `hairline` border.
- **Accent-вариант** (одна из трёх — самая важная метрика): `linear-gradient(180deg, brand-soft, #EEF3FC)`, `border-color: rgba(26,93,216,0.20)`, `value color: brand`.

### 6.4 Кнопки

#### Primary (`btn-primary`)
- Высота: `52dp`, radius: `16dp`
- BG: `linear-gradient(180deg, brand 0%, #134EBC 100%)`
- Цвет: белый
- Тень + inset highlight
- Иконка + UPPERCASE текст 13dp weight 800 letter-spacing 0.06em

#### Outline (`btn-outline`)
- BG: `surface`, color: `brand-deep`
- Border: `1.5dp solid hairline-strong`

#### Full-width FAB
- Высота: `60dp`, radius: `20dp`
- Аналогично primary, но крупнее и иконка 24dp, текст 14dp

### 6.5 Settings-rows

#### Radio-group
- Контейнер: `surface` bg, `hairline` border, `radius: 16dp`, overflow:hidden
- Каждая строка: `13dp padding`, hairline-разделитель снизу
- Active-row: `linear-gradient(90deg, brand-soft, brand-faint)` bg, weight 700
- Radio-dot: `20dp`, `2dp solid hairline-strong`. Active: `2dp solid brand`, внутри `10dp brand circle`

#### Switch-row
- Аналогично radio, но без border-радиуса (внутри radio-group-контейнера)
- Switch: `42×24dp`, off=`surface-deep` bg / hairline border, on=`brand` bg
- Thumb: `18dp`, белый, тень `0 1dp 3dp rgba(14,42,87,0.22)`
- Доп: справа от label можно показать `ss-meta` — моноширное число (например, кол-во спутников), 10dp, цвет `text-3`

#### List-tile
- BG: `surface`, border: `1dp solid hairline`, radius: `16dp`
- Padding: `12dp 14dp`
- Слева: иконка в `40dp` контейнере (`brand-soft` фон + `brand` глиф). Variant: `.navy` (`brand-deep` фон + `brand-soft` глиф) для специальных элементов (антенна).
- Справа: `chevron_right`, цвет `text-3`

#### Form-row (с значением)
- BG: `surface`, border: `1dp solid hairline`, radius: `14dp`, padding: `12dp 16dp`
- Слева — label `14dp weight 600`. Справа — value в **пилюле**: `brand-soft` bg, `brand` text, моноширный, `radius: 999px`, `padding: 5dp 12dp`, `font-size: 13dp`

### 6.6 Text-field (focused)

- BG: `surface`
- Border: `1.5dp solid brand`
- Box-shadow: `0 0 0 4dp rgba(26,93,216,0.10)` (focus ring)
- Радиус: `14dp`
- Внутри: micro-cap floating label (10dp uppercase, цвет `brand`), значение 18dp weight 700 моноширное

### 6.7 Now-pill (Активно · режим)

Тонкая капсула, отображающая текущий режим работы.
- BG: `brand-soft`
- Color: `brand-deep`
- Border: `1dp solid rgba(26,93,216,0.30)`
- Radius: `999px`, padding: `7dp 14dp 7dp 10dp`
- Слева — точка `8dp brand` с halo `0 0 0 3dp rgba(26,93,216,0.22)`
- Текст: 11dp weight 800 uppercase letter-spacing 0.10em

### 6.8 Pending-banner (несохранённые изменения)

Внизу экрана settings.
- BG: `linear-gradient(135deg, #FFF6E0 0%, #FCEAB8 100%)` (тёплый warning)
- Border-left: `4dp solid warning`
- Radius: `16dp`
- Иконка-кружок `36dp warning` + белый `priority_high`
- Action button: `pb-action` — `warning` solid pill, белый текст uppercase 11dp weight 800

### 6.9 Live-card (real-time координаты)

- BG: `surface`, hairline-strong border, radius: `16dp`
- Левая marker-полоска `4dp success` (как у status-card)
- Тройка координат `N / E / H` моноширно, маленький label слева (12dp Inter)
- Внизу — `sigma`-строка через dashed-divider, левая часть мета (`σ 11 / 16 mm · HRMS/VRMS`), правая — `fix-mini` пилюля (`RTK Fix` weight 800 uppercase success)

### 6.10 Inline-status

Узкая капсула под distance-card.
- BG: `surface`, hairline border, radius: `14dp`, padding: `10dp 14dp`
- Моноширный текст с разделителями `·`
- Зелёная точка слева, ключевое слово (`RTK FIX`) uppercase weight 800 success

---

## 7. Сигнатурные визуальные элементы

### 7.1 Компасная роза (Stakeout)

**Центральный визуальный якорь приложения.** Полноценная SVG-композиция `280×280dp`.

Слои (изнутри наружу):
1. Радиальный градиент фона (`#F7FAFD → #FFFFFF → #F0F4FA`)
2. Внешний круг: `r=132`, stroke `brand-deep` 2dp
3. Внутренний круг: `r=122`, stroke `brand-deep` 0.5dp
4. **Dashed-кольца**: `r=100` и `r=70`, `stroke-dasharray="2,3"`, opacity 0.30–0.45
5. **Кардинальные tick-метки** (N/S/E/W): жирные `1.4dp`, S/E/W с opacity 0.5
6. **Tick-метки 30°/60°**: средние `1dp`, vice-cardinals тоже dim
7. **Минорные tick-метки 10°**: тонкие `0.4dp`, opacity 0.4 — создают плотный геодезический риск
8. **Cardinal labels** (N/S/E/W): `Inter 12–14dp` weight 700–900. N полнотонная, остальные opacity 0.5
9. **4-конечная роза** в центре (декоративная): polygons brand-deep opacity 0.10
10. **Target circle** в направлении bearing: `r=14` dashed `brand-deep`, fill `brand` opacity 0.22, центр `brand` solid
11. **Стрелка bearing**: polygon с **градиентом cyan-bright → brand** (`#22D3EE → #1A5DD8`), navy stroke 1.2dp. Сзади — затемнённый хвост opacity 0.35.
12. **Центральная точка**: белый круг 11dp, navy border 2dp, внутри brand 5dp, внутри белый 2dp (концентрический bullseye)

В Compose реализовать через `Canvas` с `drawCircle`, `drawArc`, `drawPath`, `drawLine` и custom `Brush.linearGradient` для стрелки.

### 7.2 Progress-ring (Survey averaging)

`200×200dp` кольцо для индикации эпох усреднения.
- **Внешний guide-circle**: `r=94`, stroke `brand-deep` 0.5dp dashed `2,3` opacity 0.25 (топо-маркер)
- **Track**: `r=82`, stroke `#EFF3FA` 12dp
- **Progress arc**: stroke с тем же **cyan→blue градиентом** (`#22D3EE → #1A5DD8`), 12dp, `stroke-linecap: round`, поворот `-90°`, dashOffset рассчитывается из прогресса
- **Tick-метки** на 12/3/6/9 часах: 1.4dp / 0.8dp+opacity
- **Центр**: моноширный `XX / 30` (XX крупно `44dp brand-deep`, /30 мельче `18dp text-2`), снизу `micro-cap` лейбл «эпох»

### 7.3 Status-card rose watermark

Маленький SVG (`~90×90dp`) роза в правом верхнем углу status-card, opacity 0.07, частично за краем (negative `right: -10dp; top: -10dp`). Добавляет картографическую идентичность всем экранам.

### 7.4 Pinging-dot (live-индикатор)

Зелёная точка статуса с расходящимся кольцом.
- Точка `8dp success` + `box-shadow: 0 0 0 3dp rgba(22,163,91,0.18)` (статичный glow)
- **Ping-ring**: `::after` псевдоэлемент `inset: -6dp`, border `1dp rgba(22,163,91,0.30)`, анимация `scale(0.6) opacity 0.8 → scale(1.4) opacity 0` за 2s бесконечно.

Использовать только на «живых» индикаторах (RTK Fix в работе). Не использовать массово.

### 7.5 Dashed-разделители

Тонкие пунктирные линии для секций.
- В CSS: `background-image: linear-gradient(to right, hairline-strong 50%, transparent 50%); background-size: 6px 1px; background-repeat: repeat-x`
- В Compose: `Canvas` или `Modifier.drawBehind` с `pathEffect = PathEffect.dashPathEffect`

Использовать для:
- `divider` в distance-card между big-числом и row-info
- `section-label::after` (горизонталь после заголовка секции)
- `border-top` в live-card перед sigma-строкой

---

## 8. Топ-бар, навигация

### 8.1 Status-bar (системный)
- Высота `34dp`, padding `8dp 24dp 0`
- Время слева моноширное 13dp `brand-deep`
- Справа — иконки сигнала/wifi/батареи 16dp

### 8.2 Top-bar (app)
- Высота `58dp`, центрированный title
- Слева/справа — icon-buttons `42dp` (back/menu/close/more)
- Title: 16dp weight 700 `brand-deep`. Под ним small (11dp weight 500 text-2) — контекст экрана.
- Border-bottom: `1dp hairline`

### 8.3 Nav-bar (нижняя)
- Высота `76dp`
- BG: `surface`, border-top `1dp hairline`
- 4 пункта: Главная / Карта / Точки / Настройки
- Каждый: иконка в icon-cap (`56×30dp`, radius `16dp`) + label 11dp weight 600
- Active: `icon-cap` background = `brand-soft`, color = `brand`

---

## 9. Анти-паттерны (НЕ делать)

- ❌ Cyan/teal/turquoise как акценты — это цвета конкурентов
- ❌ Material You dynamic theming, blob-shapes, expressive curved cuts
- ❌ Drop-shadows на обычных карточках (используем hairline-border)
- ❌ Blur эффекты, frosted glass
- ❌ Любые градиенты на больших поверхностях кроме hero-tile и primary buttons
- ❌ Эмодзи в UI (только Material Symbols)
- ❌ Filled-иконки (только outlined)
- ❌ Жирный body-text weight 700+ (мax 600 для body)
- ❌ ALL CAPS для крупного текста (только micro-cap ≤11dp)
- ❌ Шрифт с засечками
- ❌ Цветные plain-блоки фуксии/фиолетового/оранжевого в декоративных целях
- ❌ Skeuomorphism (3D-кнопки, реалистичные текстуры)
- ❌ Декоративные иконки рядом с заголовками без функции
- ❌ Round-corner у односторонних бордеров (если border-left, то `border-radius: 0`)

---

## 10. Замечания по реализации в Jetpack Compose

### 10.1 Material 3 vs кастом

Не использовать Material 3 `ColorScheme` напрямую — он навязывает контейнерные цвета и dynamic theming. Создать **собственную тему**:

```kotlin
data class NewtonColors(
  val brand: Color = Color(0xFF1A5DD8),
  val brandDeep: Color = Color(0xFF0E2A57),
  val brandMid: Color = Color(0xFF4F7FCE),
  val brandSoft: Color = Color(0xFFDCE7FB),
  val brandFaint: Color = Color(0xFFEEF3FC),
  val bg: Color = Color(0xFFF5F8FC),
  val surface: Color = Color.White,
  val surfaceDeep: Color = Color(0xFFEFF3FA),
  val text: Color = Color(0xFF0E2A57),
  val text2: Color = Color(0x66_0E2A57),  // 0.66 alpha — adjust syntax
  val text3: Color = Color(0x40_0E2A57),
  val hairline: Color = Color(0x1A_0E2A57),
  val hairlineStrong: Color = Color(0x38_0E2A57),
  val success: Color = Color(0xFF16A35B),
  val warning: Color = Color(0xFFD08400),
  val error: Color = Color(0xFFC0273D),
)
val LocalNewtonColors = staticCompositionLocalOf { NewtonColors() }
```

Аналогично — `NewtonTypography`, `NewtonShapes`, `NewtonSpacing`.

Использование: `NewtonTheme.colors.brand`, `NewtonTheme.typo.monoXl` и т.д.

### 10.2 Шрифты

```kotlin
val provider = GoogleFont.Provider(
  providerAuthority = "com.google.android.gms.fonts",
  providerPackage = "com.google.android.gms",
  certificates = R.array.com_google_android_gms_fonts_certs
)
val Inter = FontFamily(Font(GoogleFont("Inter"), provider, FontWeight.W400 …))
val JetBrainsMono = FontFamily(Font(GoogleFont("JetBrains Mono"), provider, FontWeight.W500 …))
```

Для mono-чисел использовать `TextStyle` с `fontFeatureSettings = "tnum 1"`.

### 10.3 Иконки

Подключить `androidx.compose.material:material-icons-extended` (или использовать Symbols через AndroidView, если важна точность). Большинство нужных иконок есть в `Icons.Outlined.*`.

### 10.4 Кастомные SVG → Canvas

Компас и progress-ring рисовать через `Canvas { ... }` модификатор. Стрелка компаса с градиентом — `Brush.linearGradient`. Кардинальные ticks — отдельные `drawLine` в цикле с поворотом через `rotate(degrees)`.

Заготовка для компаса: основной модуль `CompassRose(modifier, bearing: Float, sizeDp: Dp)` принимает `bearing` в градусах и рендерит весь стек слоёв.

### 10.5 Анимации

- Pinging-кольцо у RTK Fix → `rememberInfiniteTransition` + `animateFloat` для масштаба и opacity.
- Hero tile нажатие → `Modifier.scale(0.98)` на pressed через `interactionSource`.
- Switch-thumb перемещение → `animateDpAsState`.

### 10.6 Локализация

- Все строки на русском.
- Числа форматировать через `NumberFormat.getInstance(Locale("ru"))` для разделителей разрядов и десятичной запятой.
- Координаты в формате DMS (`55°45′12.347″`) — кастомный formatter, без локали.

### 10.7 Адаптивность

- Все мокапы — `380×800` (узкий phone). Минимальный поддерживаемый width 360dp.
- На широких экранах (планшет/landscape) — sidebar-навигация вместо bottom-nav, грид 4×N вместо 3×N. Это вне scope текущего spec — отдельная задача.
- Все размеры в dp, не sp (кроме шрифтов — sp).

---

## 11. Проверочный список (для каждого экрана)

При генерации/проверке нового экрана пройти:

- [ ] BG — `bg` (не белый, светло-голубой `#F5F8FC`)
- [ ] Карточки — `surface` с hairline или hairline-strong border, никаких теней (кроме hero/FAB)
- [ ] Числа — JetBrains Mono + `tnum`
- [ ] Текст — Inter
- [ ] Статус RTK Fix — зелёный success + pinging-dot, не статичный
- [ ] Координаты — DMS-формат, моноширно
- [ ] Иконки — Material Outlined, не filled
- [ ] Primary action — `brand`-градиент, uppercase letter-spacing 0.06em
- [ ] Cyan присутствует ТОЛЬКО в стрелке компаса и progress-ring (internal SVG-gradient)
- [ ] Border-radius у карточек 16–18dp, у пилюль 999px
- [ ] Section-labels всегда uppercase 11dp weight 800 letter-spacing 0.18em с dashed-tail справа
- [ ] Hover/pressed состояния — `surface-deep` или `scale(0.98)`, не интенсивные

---

## 12. Контрольные референсы

- **Файл мокапов**: `newton-field-blue.html` — открыть в браузере, реальное представление палитры и компонентов в трёх экранах (Home / Stakeout / Survey / Settings).
- **Шрифты онлайн**: fonts.google.com/specimen/Inter и /specimen/JetBrains+Mono
- **Иконки**: fonts.google.com/icons (filter: Outlined)
- **Близкие по духу референсы**: Garmin GPSMAP UI, Trimble Access, ArcGIS Field Maps (только дизайн-язык, не UX-структура).

---

*Этот документ — единый источник правды по визуальному стилю Newton Field App. При расхождении с реализацией ориентироваться на этот документ + приложенный HTML-референс.*
