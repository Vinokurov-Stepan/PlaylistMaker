PlaylistMaker

Приложение для поиска музыки через iTunes API, прослушивания отрывков треков, создания плейлистов и сохранения избранных композиций.

Проект разработан в рамках обучения в Яндекс.Практикуме и демонстрирует современный подход к Android-разработке: Clean Architecture, MVVM, Jetpack Compose, Coroutines/Flow и интеграцию с REST API.

Особенности
Поиск музыки — поиск треков через iTunes API с debounce-задержкой для оптимизации запросов
Аудиоплеер — прослушивание 30-секундных отрывков с управлением (плей/пауза, прогресс-бар)
Избранное — сохранение треков в локальную базу Room
Плейлисты — создание, редактирование и просмотр плейлистов
Тёмная тема — поддержка светлой и тёмной тем
Адаптивный UI — интерфейс на Jetpack Compose с Material 3
История поиска — сохранение последних запросов в SharedPreferences

Стек технологий
Язык: Kotlin
UI: Jetpack Compose, Material 3
Архитектура: Clean Architecture, MVVM
Асинхронность: Coroutines, Flow
Сеть: Retrofit, OkHttp, iTunes API
База данных: Room, SharedPreferences
Аудиоплеер: MediaPlayer
DI: Koin
Навигация: Navigation Component
Сборка: Gradle (Kotlin DSL)

Структура проекта
Проект построен на Clean Architecture с разделением на слои:
com.practicum.playlistmaker/
├── app/
│ ├── search/ - Экран поиска
│ │ ├── data/ - Data-слой (репозитории, DTO, API)
│ │ ├── domain/ - Domain-слой (модели, use cases)
│ │ └── presentation/ - Presentation-слой (ViewModel, UI-состояния)
│ ├── media/ - Экран медиатеки
│ │ ├── data/ - Data-слой
│ │ ├── domain/ - Domain-слой
│ │ └── presentation/ - Presentation-слой
│ ├── player/ - Экран аудиоплеера
│ │ ├── data/ - Data-слой
│ │ ├── domain/ - Domain-слой
│ │ └── presentation/ - Presentation-слой
│ ├── settings/ - Экран настроек
│ │ ├── data/ - Data-слой
│ │ ├── domain/ - Domain-слой
│ │ └── presentation/ - Presentation-слой
│ ├── playlist/ - Экран плейлистов
│ │ ├── data/ - Data-слой
│ │ ├── domain/ - Domain-слой
│ │ └── presentation/ - Presentation-слой
│ ├── core/ - Общие компоненты
│ │ ├── di/ - Модули DI
│ │ ├── network/ - Retrofit, OkHttp
│ │ ├── database/ - Room, DAO
│ │ └── utils/ - Утилиты, расширения
│ └── ui/ - Общие UI-компоненты
│ ├── theme/ - Темы, цвета, шрифты
│ └── components/ - Переиспользуемые Compose-компоненты

Основные экраны
Поиск — Поисковая строка с debounce, список найденных треков
Аудиоплеер — Воспроизведение отрывка трека (плей/пауза, прогресс-бар)
Избранное — Список треков, добавленных в избранное (Room)
Плейлисты — Список созданных плейлистов
Создание плейлиста — Форма с названием, описанием и выбором обложки
Детали плейлиста — Просмотр треков внутри плейлиста
Настройки — Переключение темы, тёмная/светлая

Установка и запуск
Требования:
Android Studio Meerkat или новее
JDK 17+
Android SDK (minSdk 29, targetSdk 34)

Клонирование и сборка:
git clone https://github.com/Vinokurov-Stepan/PlaylistMaker.git
cd PlaylistMaker
./gradlew assembleDebug
./gradlew installDebug

Запуск в Android Studio:
Открыть проект в Android Studio
Дождаться синхронизации Gradle
Выбрать эмулятор или подключить физическое устройство
Нажать Run

Ветка с XML-версией
Исходная версия интерфейса на XML / View-системе доступна в ветке ui-old-view-xml.
git checkout ui-old-view-xml
Внимание: ветка содержит устаревшую реализацию и не обновляется. Основная разработка ведётся на Jetpack Compose в ветке main.

Лицензия
Этот проект создан в рамках учебного курса Яндекс.Практикума и предназначен для демонстрации навыков Android-разработки.

Контакты
GitHub: https://github.com/Vinokurov-Stepan
Telegram: @Stepan_Vin
Email: svv.vinokurov@mail.ru
