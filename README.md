# corelia-provider-test

In-memory provider для unit-, integration- и startup-тестов Corelia. Модуль
предоставляет fixture document/version/type/attachment/workflow/task/
permission capability и отдельную test-binary capability. Его
auto-configuration активируется при `corelia.provider=test`.

Provider не хранит данные после завершения JVM, не имитирует семантику
PostgreSQL, S3 или Flowable полностью и не предназначен для production
profile. Используйте реальные provider integration/system tests для проверки
пограничных случаев инфраструктуры.

```bash
mvn -pl corelia-provider-test -am test
```

Контракт интерфейсов: [provider SPI](../docs/provider-spi.md).
