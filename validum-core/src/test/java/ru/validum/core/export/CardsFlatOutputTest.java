package ru.validum.core.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.validum.core.TestProfiles;
import ru.validum.core.card.MirCard;
import ru.validum.core.profile.IndividualProfile;

/** Карта в плоской выдаче: одна карта в профиле, ноль карт, круговая операция. */
class CardsFlatOutputTest {

  @Test
  @DisplayName("Одна карта по умолчанию: pan, срок, исход и счётчик")
  void defaultCardIsInProfile() {
    IndividualProfile profile = TestProfiles.individual(9_101);
    Map<String, String> row = IndividualProfileFlattener.flatten(profile);

    assertThat(profile.cards()).hasSize(1);
    assertThat(row.get("cardPan")).isEqualTo(profile.cards().get(0).pan());
    assertThat(row.get("cardExpiry")).matches("\\d{2}/\\d{2}");
    assertThat(row.get("cardOutcome")).isNotEmpty();
    assertThat(row.get("cardCount")).isEqualTo("1");
  }

  @Test
  @DisplayName("--cards 0: карт нет, колонки пустые, счётчик 0")
  void noCards() {
    IndividualProfile profile = TestProfiles.builder(9_102).cards(0).build().generate();
    Map<String, String> row = IndividualProfileFlattener.flatten(profile);

    assertThat(profile.cards()).isEmpty();
    assertThat(row.get("cardPan")).isEmpty();
    assertThat(row.get("cardCount")).isEqualTo("0");
  }

  @Test
  @DisplayName("Несколько карт: в строке первая, счётчик показывает количество")
  void severalCards() {
    IndividualProfile profile = TestProfiles.builder(9_103).cards(3).build().generate();
    Map<String, String> row = IndividualProfileFlattener.flatten(profile);

    assertThat(profile.cards()).hasSize(3);
    assertThat(row.get("cardPan")).isEqualTo(profile.cards().get(0).pan());
    assertThat(row.get("cardCount")).isEqualTo("3");
  }

  @Test
  @DisplayName("Круговая операция flatten → parse сохраняет карту")
  void roundTripKeepsCard() {
    IndividualProfile profile = TestProfiles.individual(9_104);
    MirCard card = profile.cards().get(0);

    IndividualProfile restored = ProfileFromCsv.parse(IndividualProfileFlattener.flatten(profile));

    assertThat(restored.cards()).hasSize(1);
    assertThat(restored.cards().get(0).pan()).isEqualTo(card.pan());
    assertThat(restored.cards().get(0).outcome()).isEqualTo(card.outcome());
  }
}
