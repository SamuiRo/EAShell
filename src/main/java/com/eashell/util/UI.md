# 🎨 Повна візуальна схема EA Shell UI

## 📐 Загальна структура вікна

```
┌────────────────────────────────────────────────────────────────────┐
│                         TopBar.java                                │
│  ⚡ Shell    Running: 2    [пробіл]    [+ NEW SCRIPT] [⏹ STOP ALL] │
├──────────────────────────────┬─────────────────────────────────────┤
│                              │                                     │
│   ScriptListPanel.java       │        OutputPanel.java             │
│   (Ліва панель - 40%)        │        (Права панель - 60%)         │
│                              │                                     │
│  ┌─────────────────────────┐ │  ┌──────────────────────────────┐  │
│  │  📋 SCRIPTS             │ │  │  📟 CONSOLE                  │  │
│  ├─────────────────────────┤ │  ├──────────────────────────────┤  │
│  │ ╔═══════════════════╗   │ │  │ [MyScript 🟢] [Build 🟢]     │  │
│  │ ║ ScriptCard.java   ║   │ │  │ ┌────────────────────────┐   │  │
│  │ ╠═══════════════════╣   │ │  │ │                        │   │  │
│  │ ║ My Script ⚫       ║   │ │  │ │  $ npm install         │   │  │
│  │ ║ 📁 /path/to/dir   ║   │ │  │ │  Installing deps...    │   │  │
│  │ ║ ▶ npm install     ║   │ │  │ │  Done!                 │   │  │
│  │ ║ ▶ npm start       ║   │ │  │ │                        │   │  │
│  │ ║                   ║   │ │  │ │  (TextArea)            │   │  │
│  │ ║ [▶RUN][✎EDIT][✖]  ║   │ │  │ │                        │   │  │
│  │ ╚═══════════════════╝   │ │  │ └────────────────────────┘   │  │
│  │                         │ │  │ [⏹ STOP] [🗑 CLEAR]          │  │
│  │ ╔═══════════════════╗   │ │  └──────────────────────────────┘  │
│  │ ║ Build App ⚫       ║   │ │                                     │
│  │ ║ 📁 /project       ║   │ │                                     │
│  │ ║ ▶ gradle build    ║   │ │                                     │
│  │ ║ [▶RUN][✎EDIT][✖]  ║   │ │                                     │
│  │ ╚═══════════════════╝   │ │                                     │
│  │                         │ │                                     │
│  │ (ScrollPane)            │ │                                     │
│  └─────────────────────────┘ │                                     │
│                              │                                     │
└──────────────────────────────┴─────────────────────────────────────┘
        MainWindow.java (BorderPane + SplitPane)
```

---

## 🔍 Детальний розбір компонентів

### 1️⃣ TopBar (Верхня панель)

```java
// TopBar.java
┌────────────────────────────────────────────────────────────┐
│ Label      Label      Region         Button      Button    │
│ "⚡ Shell"  "Running:2" [SPACER]  [+ NEW SCRIPT] [⏹ STOP ALL] │
└────────────────────────────────────────────────────────────┘
│←────────── HBox з alignment=CENTER_LEFT ─────────────────→│

Колір фону: Градієнт CARD_BG → #3d2952
Рамка знизу: 2px BORDER_GLOW
Ефект: Тінь зі світінням
```

**Що відбувається при натисканні:**
- `+ NEW SCRIPT` → `handleAddScript()` → відкривається `ScriptDialog`
- `⏹ STOP ALL` → `handleStopAll()` → зупиняються всі процеси

---

### 2️⃣ ScriptListPanel (Ліва панель)

```java
// ScriptListPanel.java
┌────────────────────────────┐
│ Label "📋 SCRIPTS"         │ ← Header
├────────────────────────────┤
│ Separator                  │
├────────────────────────────┤
│ ┌────────────────────────┐ │
│ │ ScrollPane             │ │
│ │ ┌────────────────────┐ │ │
│ │ │ VBox (container)   │ │ │
│ │ │ ┌────────────────┐ │ │ │
│ │ │ │ ScriptCard #1  │ │ │ │ ← scriptCards.get("name1")
│ │ │ └────────────────┘ │ │ │
│ │ │ ┌────────────────┐ │ │ │
│ │ │ │ ScriptCard #2  │ │ │ │ ← scriptCards.get("name2")
│ │ │ └────────────────┘ │ │ │
│ │ │ ...                │ │ │
│ │ └────────────────────┘ │ │
│ └────────────────────────┘ │
└────────────────────────────┘
       VBox (panel)

Колір фону: Градієнт SECONDARY_BG → PRIMARY_BG
```

**Структура даних:**
```java
Map<String, ScriptCard> scriptCards;
// "My Script" → ScriptCard об'єкт
// "Build App" → ScriptCard об'єкт
```

---

### 3️⃣ ScriptCard (Картка скрипта)

```java
// ScriptCard.java
┌────────────────────────────┐
│ HBox (titleBox)            │
│ ┌────────────┬───────────┐ │
│ │ "My Script"│ Label ⚫  │ │ ← statusLabel
│ └────────────┴───────────┘ │
│                            │
│ Label "📁 /path/to/dir"    │ ← pathLabel
│                            │
│ VBox (commandsBox)         │
│ ┌────────────────────────┐ │
│ │ Label "▶ npm install"  │ │
│ │ Label "▶ npm start"    │ │
│ └────────────────────────┘ │
│                            │
│ HBox (buttonBox)           │
│ [▶ RUN] [✎ EDIT] [✖ DEL]  │
└────────────────────────────┘
          VBox (card)

Колір фону: Градієнт CARD_BG → #261c33
Рамка: 1.5px BORDER_COLOR
Hover: Світліший фон + 2px BORDER_GLOW
Тінь: Зелено-фіолетове світіння
```

**Стани statusLabel:**
- `⚫` TEXT_MUTED - зупинено (без світіння)
- `🟢` ACCENT_ELECTRIC - працює (зі світінням)

**Що відбувається при натисканні:**
- `▶ RUN` → `handleRunScript()` → створюється вкладка + запускається ProcessRunner
- `✎ EDIT` → `handleEditScript()` → відкривається ScriptDialog з даними
- `✖ DELETE` → `handleDeleteScript()` → показується діалог підтвердження

---

### 4️⃣ OutputPanel (Права панель)

```java
// OutputPanel.java
┌────────────────────────────┐
│ Label "📟 CONSOLE"         │ ← Header
├────────────────────────────┤
│ Separator                  │
├────────────────────────────┤
│ ┌────────────────────────┐ │
│ │ TabPane                │ │
│ │ [Tab1] [Tab2] [Tab3]   │ │ ← Вкладки для кожного скрипта
│ │ ┌────────────────────┐ │ │
│ │ │ VBox (tabContent)  │ │ │
│ │ │ ┌────────────────┐ │ │ │
│ │ │ │ TextArea       │ │ │ │ ← outputArea (вивід консолі)
│ │ │ │ $ npm install  │ │ │ │
│ │ │ │ Installing...  │ │ │ │
│ │ │ │ Done!          │ │ │ │
│ │ │ └────────────────┘ │ │ │
│ │ │ ┌────────────────┐ │ │ │
│ │ │ │ HBox (control) │ │ │ │
│ │ │ │ [⏹ STOP]       │ │ │ │
│ │ │ │ [🗑 CLEAR]     │ │ │ │
│ │ │ └────────────────┘ │ │ │
│ │ └────────────────────┘ │ │
│ └────────────────────────┘ │
└────────────────────────────┘
          VBox (panel)

Колір фону: Градієнт PRIMARY_BG → #120e1a
TextArea фон: #0d0a12 (майже чорний)
TextArea текст: ACCENT_ELECTRIC (світло-фіолетовий)
```

**Структура вкладки:**
```
Tab "My Script 🟢"
├── userData: ProcessRunner (для зупинки при закритті)
└── content: VBox
    ├── TextArea (outputArea)
    └── HBox (controlBox)
        ├── Button "⏹ STOP"
        └── Button "🗑 CLEAR"
```

**Що відбувається:**
- Натискання `⏹ STOP` → `runner.stop()` зупиняє процес
- Натискання `🗑 CLEAR` → `outputArea.clear()` очищає текст
- Закриття вкладки → автоматично викликається `runner.stop()`

---

## 🎯 Діалогові вікна

### ScriptDialog (Додавання/Редагування)

```java
// ScriptDialog.java
┌────────────────────────────────┐
│ Add New Script / Edit Script   │ ← Заголовок
├────────────────────────────────┤
│ GridPane                       │
│                                │
│ Name:     [TextField_______]   │ ← nameField
│                                │
│ Path:     [TextField_______]   │ ← pathField
│           [Browse]             │
│                                │
│ Commands: ┌─────────────────┐  │
│           │ TextArea        │  │ ← commandsArea
│           │ npm install     │  │   (одна команда на рядок)
│           │ npm start       │  │
│           └─────────────────┘  │
│                                │
│            [OK]    [Cancel]    │
└────────────────────────────────┘

Колір фону: Градієнт CARD_BG → PRIMARY_BG
Рамка: 2px BORDER_GLOW
```

### DeleteConfirmDialog (Підтвердження видалення)

```java
// DeleteConfirmDialog.java
┌─────────────────────────────┐
│ Confirm Delete              │
├─────────────────────────────┤
│ Delete script: My Script    │
│                             │
│ Are you sure you want to    │
│ delete this script?         │
│ This action cannot be       │
│ undone.                     │
│                             │
│        [OK]    [Cancel]     │
└─────────────────────────────┘
```

---

## 🔄 Потік даних при запуску скрипта

```
1. Користувач натискає [▶ RUN] на ScriptCard
   ↓
2. MainWindow.handleRunScript(entry)
   ↓
3. Перевірка чи не запущений вже (runningProcesses)
   ↓
4. Створення ProcessRunner(entry, null, null)
   ↓
5. OutputPanel.createOutputTab(entry, runner, callback)
   ├── Створюється Tab з назвою "My Script 🟢"
   ├── Створюється TextArea для виводу
   ├── Створюються кнопки [⏹ STOP] [🗑 CLEAR]
   └── Tab.setUserData(runner) ← Зберігаємо runner
   ↓
6. runner.setOutputArea(outputArea) ← Прив'язуємо вивід
   runner.setTab(outputTab) ← Прив'язуємо вкладку
   ↓
7. runningProcesses.put(name, runner) ← Зберігаємо в мапі
   ↓
8. updateScriptStatus(name, true) ← Змінюємо ⚫ → 🟢
   ↓
9. executorService.submit(runner) ← Запускаємо в фоновому потоці
   ↓
10. ProcessRunner виконує команди та пише в outputArea
```

---

## 🎨 Кольори за призначенням

### Фони (від найтемнішого до найсвітлішого)
```
#0d0a12  ← TextArea (консоль)
#120e1a  ← OutputPanel (низ)
#1a1425  ← PRIMARY_BG (основний)
#241b2f  ← SECONDARY_BG (панелі)
#2d2139  ← CARD_BG (картки)
#3a2847  ← CARD_HOVER_BG (картки при hover)
```

### Текст
```
#e8dff5  ← TEXT_PRIMARY (основний текст)
#b8a4e8  ← TEXT_SECONDARY (другорядний текст, команди)
#7a6b8f  ← TEXT_MUTED (сірий текст, шляхи)
#d4af37  ← TEXT_ACCENT (золотий, не використовується зараз)
```

### Кнопки
```
#b8a4e8  ← ACCENT_GREEN/ELECTRIC (▶ RUN, + NEW)
#9d7bd8  ← ACCENT_BLUE/PURPLE (✎ EDIT, 🗑 CLEAR)
#e85d75  ← ACCENT_RED (✖ DELETE, ⏹ STOP)
```

### Рамки
```
#4a3960  ← BORDER_COLOR (звичайна рамка)
#8b6bb8  ← BORDER_GLOW (світіння при hover/focus)
```

---

## 💡 Корисні поради для дизайну

### Якщо хочеш змінити колір:
1. Знайди константу в `StyleManager` (наприклад `ACCENT_RED`)
2. Подивись де вона використовується (Ctrl+F)
3. Зміни значення в константі

### Якщо хочеш змінити розмір:
1. Перевір `Constants.java` для розмірів вікна
2. Для окремих елементів шукай `setPadding()`, `setSpacing()`
3. Для шрифтів шукай `font-size` в методах `StyleManager`

### Якщо хочеш додати новий ефект:
1. Використай `setEffect(new DropShadow(...))` для світіння
2. Використай градієнти в стилях: `linear-gradient(to right, color1, color2)`
3. Для hover ефектів використай `setOnMouseEntered/Exited`

### Структура відповідальності:
- `StyleManager` → ВСІ кольори та CSS стилі
- `Constants` → Розміри, тексти, таймаути
- `*Panel` класи → Розташування елементів
- `MainWindow` → Логіка роботи програми