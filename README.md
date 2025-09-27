# Nefor Activator

Nefor Activator — это комплект из серверного Spigot/Paper плагина и SDK, который обеспечивает лицензионное управление зависимыми плагинами. Activator проверяет валидность токена, распространяет события о смене статуса и отключает плагины, подключённые через API, при потере лицензии.

## Состав
- ctivator-api — лёгкая библиотека для разработчиков защищаемых плагинов (интерфейсы, аннотации, утилиты).
- ctivator-plugin — плагин для сервера, выполняющий проверку лицензии, работу с офлайн-кешем, события, команды и CRL.
- docs/ — дополнительная документация, в том числе [integration-guide.md](docs/integration-guide.md) с пошаговой интеграцией.

## Требования
- Java 17+
- Gradle 8.1+ (в репозитории лежит Gradle Wrapper)
- Git для управления версионированием

## Быстрый старт
`ash
# Клонирование
git clone https://github.com/<your-account>/nefor-activator.git
cd nefor-activator

# Сборка (выпускает API-jar и shaded-plugin jar)
./gradlew build
`

Полученные артефакты:
- ctivator-api/build/libs/activator-api-<version>.jar
- ctivator-plugin/build/libs/activator-plugin-<version>-all.jar

## Настройка сервера
1. Скопируйте ctivator-plugin-*-all.jar в папку plugins/ на сервере.
2. Запустите сервер один раз — появится plugins/Activator/config.yml.
3. Заполните licenseKey, secret, параметры endpoint, настройки офлайн-кеша и локализации.
4. Защищаемые плагины должны указывать зависимость depend: [Activator] и использовать API (см. [integration-guide.md](docs/integration-guide.md)).

## Публикация API в GitHub Packages
1. Создайте PAT на GitHub с правами write:packages, ead:packages, epo.
2. Сохраните учётные данные для Gradle:
   - Файл %USERPROFILE%\.gradle\gradle.properties:
     `
     gpr.user=GITHUB_LOGIN
     gpr.key=GITHUB_PAT
     `
   - Или переменные окружения перед запуском:
     `powershell
      = "GITHUB_LOGIN"
         = "GITHUB_PAT"
     `
3. Проверьте блок publishing в ctivator-api/build.gradle.kts (URL вида https://maven.pkg.github.com/<owner>/<repo>).
4. Опубликуйте артефакт:
   `ash
   ./gradlew :activator-api:publish
   `
   Аналогично можно добавить публикацию для ctivator-plugin.

## Использование API из GitHub Packages (потребители)
GitHub Packages требует авторизации даже для публичных репозиториев, поэтому потребителям нужен токен с правами ead:packages.

### Gradle (Kotlin DSL)
`kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/<owner>/<repo>")
        credentials {
            username = findProperty("gpr.user")?.toString() ?: System.getenv("GITHUB_USERNAME")
            password = findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("dev.nefor.activator:activator-api:1.0.0-SNAPSHOT")
}
`

### Maven (settings.xml)
`xml
<servers>
  <server>
    <id>github</id>
    <username></username>
    <password></password>
  </server>
</servers>
`
`xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/<owner>/<repo></url>
  </repository>
</repositories>

<dependency>
  <groupId>dev.nefor.activator</groupId>
  <artifactId>activator-api</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <scope>provided</scope>
</dependency>
`

## Интеграция сторонних плагинов
- Используйте аннотации @RequiresLicense, @FeatureFlag, утилиты ActivationGuard и LicenseIntegrationSupport.
- Подробный сценарий в [docs/integration-guide.md](docs/integration-guide.md).

## Полезные команды
| Команда | Описание |
| --- | --- |
| ./gradlew build | Сборка обоих модулей |
| ./gradlew :activator-api:publish | Публикация API в GitHub Packages |
| ./gradlew :activator-plugin:shadowJar | Пересборка shaded-плагина |

## Структура репозитория
`
NeforActivator
 +-- activator-api/      # исходники SDK
 +-- activator-plugin/   # серверный плагин
 +-- docs/               # документация
 +-- build.gradle.kts    # корневой Gradle-скрипт
 +-- settings.gradle.kts
 L-- .gitignore
`

## Дополнительно
- Для Maven Central потребуется регистрация в OSSRH, домен/GPG и CI.
- Для CI/CD можно собрать GitHub Actions workflow: ./gradlew build + ./gradlew publish по тегам.

Если возникнут вопросы по публикации или интеграции — создавайте issue/PR или пишите в обсуждения.
