package ru.validum.allure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeEachCallback;

/**
 * Структурные проверки {@link ValidumExtension} — без fake {@link
 * org.junit.jupiter.api.extension.ExtensionContext}.
 *
 * <p>Поведение Extension (beforeEach с аннотациями) уже покрыто smoke-тестами в {@link
 * ValidumSeedTest} и {@link ValidumGenerateTest} через реальный JUnit Platform — там Extension
 * вызывается движком и падение было бы видно.
 */
@DisplayName("ValidumExtension — структурные проверки (P3.2)")
class ValidumExtensionDirectTest {

  @Test
  @DisplayName("implements BeforeEachCallback")
  void implementsBeforeEachCallback() {
    assertThat(BeforeEachCallback.class.isAssignableFrom(ValidumExtension.class)).isTrue();
  }

  @Test
  @DisplayName("final class")
  void isFinal() {
    assertThat(Modifier.isFinal(ValidumExtension.class.getModifiers())).isTrue();
  }

  @Test
  @DisplayName("Можно инстанцировать")
  void canInstantiate() {
    assertThat(new ValidumExtension()).isNotNull();
  }
}
