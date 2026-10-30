package ru.validum.allure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Метаданные аннотаций @ValidumSeed / @ValidumGenerate (P3.2)")
class ValidumAnnotationsTest {

  @Test
  @DisplayName("@ValidumSeed: RUNTIME retention, METHOD target")
  void validumSeedMetadata() {
    Retention retention = ValidumSeed.class.getAnnotation(Retention.class);
    Target target = ValidumSeed.class.getAnnotation(Target.class);

    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);

    assertThat(target).isNotNull();
    assertThat(target.value()).containsExactly(ElementType.METHOD);
  }

  @Test
  @DisplayName("@ValidumGenerate: RUNTIME retention, METHOD target")
  void validumGenerateMetadata() {
    Retention retention = ValidumGenerate.class.getAnnotation(Retention.class);
    Target target = ValidumGenerate.class.getAnnotation(Target.class);

    assertThat(retention).isNotNull();
    assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);

    assertThat(target).isNotNull();
    assertThat(target.value()).containsExactly(ElementType.METHOD);
  }

  @Test
  @DisplayName("@ValidumGenerate: дефолты age=-1, region=\"\", gender=\"\", categoryClient=\"\"")
  void validumGenerateDefaults() throws NoSuchMethodException {
    var method = ValidumGenerate.class.getDeclaredMethod("age");
    var def = method.getDefaultValue();
    assertThat(def).isEqualTo(-1);

    assertThat(ValidumGenerate.class.getDeclaredMethod("region").getDefaultValue()).isEqualTo("");
    assertThat(ValidumGenerate.class.getDeclaredMethod("gender").getDefaultValue()).isEqualTo("");
    assertThat(ValidumGenerate.class.getDeclaredMethod("categoryClient").getDefaultValue())
      .isEqualTo("");
  }

  @Test
  @DisplayName("@ValidumSeed.value() — long, обязательный (без дефолта)")
  void validumSeedValueIsMandatory() throws NoSuchMethodException {
    var method = ValidumSeed.class.getDeclaredMethod("value");
    assertThat(method.getReturnType()).isEqualTo(long.class);
    assertThat(method.getDefaultValue()).isNull(); // нет дефолта
  }

  @Test
  @DisplayName("@ValidumGenerate.seed() — long, обязательный (без дефолта)")
  void validumGenerateSeedIsMandatory() throws NoSuchMethodException {
    var method = ValidumGenerate.class.getDeclaredMethod("seed");
    assertThat(method.getReturnType()).isEqualTo(long.class);
    assertThat(method.getDefaultValue()).isNull();
  }
}
