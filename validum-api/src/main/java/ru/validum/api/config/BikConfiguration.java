package ru.validum.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;

/**
 * Конфигурация источника банковских реквизитов.
 *
 * <p>Цепочка выбора — в core {@link BikSources}: свойство {@code validum.bik.file} → переменная
 * окружения {@code VALIDUM_BIK_FILE} → {@code data/ED807.xml}. Встроенного демо-набора нет: без
 * файла ошибка {@link ru.validum.core.bank.BikSourceNotFoundException} (HTTP 503).
 *
 * <p>Источник ленивый: приложение стартует без справочника (например, чтобы отдавать health и
 * документацию), а файл ищется при первом запросе к банковским данным.
 */
@Configuration
public class BikConfiguration {

  private static final Logger log = LoggerFactory.getLogger(BikConfiguration.class);

  @Value("${validum.bik.file:}")
  private String bikFile;

  @Bean
  public BikSource bikSource() {
    return BikSources.lazy(bikFile, log::info);
  }
}
