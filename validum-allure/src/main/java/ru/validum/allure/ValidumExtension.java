package ru.validum.allure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.qameta.allure.Allure;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ru.validum.core.common.Region;
import ru.validum.core.export.IndividualProfileFlattener;
import ru.validum.core.name.Gender;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;

/**
 * JUnit 5 Extension: перехватывает тесты с {@link ValidumSeed} или {@link ValidumGenerate},
 * генерирует профиль и прикладывает его в Allure-отчёт как JSON-attachment.
 *
 * <p>Поля attachment'а — плоские, те же, что CSV (переиспользуется
 * {@link IndividualProfileFlattener} из core). Это гарантирует, что QA видят в отчёте ровно те
 * же колонки, что в CSV.
 *
 * <p>Использование:
 *
 * <pre>{@code
 * @Test
 * @ExtendWith(ValidumExtension.class)
 * @ValidumSeed(42)
 * void testWithSeed() { ... }
 * }</pre>
 *
 * <p>Или через {@code @ExtendWith} на уровне класса — тогда все тесты с аннотациями
 * автоматически обрабатываются.
 */
public final class ValidumExtension implements BeforeEachCallback {

  private static final ObjectMapper JSON = new ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .enable(SerializationFeature.INDENT_OUTPUT);

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    ValidumSeed seedAnn = context.getRequiredTestMethod().getAnnotation(ValidumSeed.class);
    ValidumGenerate genAnn = context.getRequiredTestMethod().getAnnotation(ValidumGenerate.class);

    if (seedAnn == null && genAnn == null) {
      return; // тест без аннотаций — ничего не делаем
    }

    IndividualProfile profile = generateProfile(seedAnn, genAnn);
    attachProfile(profile);
  }

  private IndividualProfile generateProfile(ValidumSeed seedAnn, ValidumGenerate genAnn) {
    long seed;
    Integer age = null;
    Region region = null;
    Gender gender = null;
    String category = null;

    if (genAnn != null) {
      seed = genAnn.seed();
      if (genAnn.age() > 0) age = genAnn.age();
      if (!genAnn.region().isBlank()) region = Region.require(genAnn.region());
      if (!genAnn.gender().isBlank()) gender = Gender.valueOf(genAnn.gender().toUpperCase());
      if (!genAnn.categoryClient().isBlank()) category = genAnn.categoryClient();
    } else {
      seed = seedAnn.value();
    }

    IndividualProfileGenerator.Builder b = IndividualProfileGenerator.builder().seed(seed);
    if (age != null) b.age(age);
    if (region != null) b.region(region);
    if (gender != null) b.gender(gender);
    if (category != null) b.categoryClient(category);

    return b.build().generate();
  }

  private void attachProfile(IndividualProfile profile) throws IOException {
    Map<String, String> flat = IndividualProfileFlattener.flatten(profile);
    String json = JSON.writeValueAsString(flat);
    // Только статический фасад Allure: одинаково работает и в 2.29.x (allure-junit5),
    // и в 2.35.x (allure-jupiter) — см. AllureDependencyFloorTest.
    Allure.addAttachment("Validum Profile", "application/json", json, "json");
  }
}
