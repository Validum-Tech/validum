package ru.validum.cli;

import ru.validum.core.common.Region;

/** Разбор значения {@code --region}: имя enum ({@code MOSCOW}) или код субъекта ({@code 77}). */
final class RegionOption {

  private RegionOption() {}

  static Region parse(String value) {
    return Region.require(value);
  }
}
