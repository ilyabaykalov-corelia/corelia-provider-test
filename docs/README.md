# Документация test provider

`TestProviderConfiguration` публикует in-memory реализации через Spring
auto-configuration, когда выбрано `corelia.provider=test`. Это позволяет
проверять независимый старт сервисов и domain-логику без внешних PostgreSQL,
S3 или Flowable.

Не добавляйте test provider в Compose production runtime и не используйте его
как доказательство совместимости с конкретной инфраструктурной реализацией.
