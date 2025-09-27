# Nefor Activator

Nefor Activator — это набор из серверного Spigot/Paper плагина и SDK, который обеспечивает лицензионное управление зависимыми плагинами. Activator проверяет токен, рассылает события и отключает плагины, подключённые через API, при потере лицензии.

## Состав
- activator-api — библиотека для разработчиков защищаемых плагинов (интерфейсы, аннотации, утилиты).
- activator-plugin — серверный плагин: проверка лицензии, офлайн-кеш, события, команды, CRL.
- docs/ — документация, включая [integration-guide.md](docs/integration-guide.md).

## Требования
- Java 17+
- Gradle 8.1+ (в комплект входит Gradle Wrapper)
- Git

## Быстрый старт
```bash
# клонирование
git clone https://github.com/Muhtat/nefor-activator.git
cd nefor-activator

# сборка (API-jar и shaded-plugin jar)
./gradlew build
```

Полученные артефакты:
- activator-api/build/libs/activator-api-<version>.jar
- activator-plugin/build/libs/activator-plugin-<version>-all.jar

## Настройка сервера
1. Поместите activator-plugin-*-all.jar в plugins/.
2. Запустите сервер один раз — появится plugins/Activator/config.yml.
3. Заполните licenseKey, secret, endpoint, офлайн-параметры и локализацию.
4. Защищаемые плагины должны указывать depend: [Activator] и использовать API (см. [integration-guide.md](docs/integration-guide.md)).

## Использование API из GitHub Packages
GitHub Packages требует авторизацию даже для публичных репозиториев, поэтому потребителям нужен токен с правами 
ead:packages.

### Gradle (Kotlin DSL)
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Muhtat/nefor-activator")
        credentials {
            username = findProperty("gpr.user")?.toString() ?: System.getenv("GITHUB_USERNAME")
            password = findProperty("gpr.key")?.toString() ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    compileOnly("dev.nefor.activator:activator-api:1.0.0-SNAPSHOT")
}
```

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
    <url>https://maven.pkg.github.com/Muhtat/nefor-activator</url>
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
- Используйте аннотации @RequiresLicense, @FeatureFlag, утилиты ActivationGuard, LicenseIntegrationSupport.
- Подробности — в [docs/integration-guide.md](docs/integration-guide.md).

## Полезные команды
| Команда | Описание |
| --- | --- |
| ./gradlew build | Сборка обоих модулей |
| ./gradlew :activator-api:publish | Публикация API в GitHub Packages |
| ./gradlew :activator-plugin:shadowJar | Пересборка shaded-плагина |

## Структура

NeforActivator
 ├── activator-api/
 ├── activator-plugin/
 ├── docs/
 ├── build.gradle.kts
 ├── settings.gradle.kts
 └── .gitignore


## Дополнительно
- Для публикации на Maven Central нужны OSSRH, домен/GPG, CI.
- CI/CD легко настроить через GitHub Actions (build + publish по тегам).

Возникли вопросы — создавайте issue или PR.
