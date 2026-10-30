package ru.validum.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.validation.BikValidator;
import ru.validum.core.validation.InnValidator;
import ru.validum.core.validation.KppValidator;
import ru.validum.core.validation.OgrnipValidator;
import ru.validum.core.validation.OgrnValidator;
import ru.validum.core.validation.OkpoValidator;
import ru.validum.core.validation.SnilsValidator;

/**
 * Валидация реквизитов. Возвращает exit code 0 если валидно, 1 если нет —
 * удобно для скриптов: {@code if validum validate inn 500100732259; then ...}.
 *
 * <p>Для счёта нужен {@code --bik}: контрольный ключ по письму ЦБ № 515 считается от БИК банка
 * (клиентский счёт) или от БИК подразделения Банка России (корреспондентский счёт).
 *
 * <p>Вывод — JSON {@code {"valid":..,"type":..,"details":..}} в stdout.
 */
@Command(
  name = "validate",
  mixinStandardHelpOptions = true,
  description = "Валидация: inn | snils | ogrn | ogrnip | account | kpp | okpo | bik <value>"
)
public final class ValidateCommand implements Callable<Integer> {

  @Parameters(index = "0",
    description = "Тип: inn | snils | ogrn | ogrnip | account | kpp | okpo | bik")
  String type;

  @Parameters(index = "1", description = "Значение для проверки")
  String value;

  @Option(
    names = {"--bik"},
    description = "БИК банка (клиентский счёт) или подразделения Банка России (корреспондентский)."
      + " Для проверки счёта обязателен.")
  String bik;

  @Override
  public Integer call() {
    Result r = switch (type.toLowerCase()) {
      case "inn" -> validateInn(value);
      case "snils" -> new Result(
        SnilsValidator.isValid(value), null, "SNILS checksum");
      case "ogrn" -> validateOgrn(value);
      case "ogrnip" -> new Result(
        OgrnipValidator.isValid(value), "INDIVIDUAL_ENTREPRENEUR", "OGRNIP checksum");
      case "account" -> validateAccount(value);
      case "kpp" -> new Result(KppValidator.isValid(value), null, "KPP format");
      case "okpo" -> validateOkpo(value);
      case "bik" -> validateBik(value);
      default -> {
        System.err.println("Неизвестный тип: " + type
          + ". Допустимо: inn, snils, ogrn, ogrnip, account, kpp, okpo, bik");
        yield new Result(false, null, "unknown type");
      }
    };

    System.out.println("{\"valid\":" + r.valid
      + ",\"type\":" + (r.type == null ? "null" : "\"" + r.type + "\"")
      + ",\"details\":\"" + r.details.replace("\"", "\\\"") + "\"}");
    return r.valid ? 0 : 1;
  }

  private Result validateAccount(String v) {
    if (!AccountValidator.isWellFormed(v)) {
      return new Result(false, null,
        "Номер счёта: 20 знаков, в 6-м разряде допускается буква валюты");
    }
    if (bik == null || bik.isBlank()) {
      return new Result(false, null,
        "Укажите --bik: контрольный ключ зависит от БИК банка");
    }
    boolean valid = AccountValidator.isValidAuto(v, bik);
    return new Result(
      valid,
      "RUB",
      valid ? "Valid account checksum (письмо 515 / 809-П)" : "Invalid account checksum or BIK");
  }

  private static Result validateInn(String v) {
    if (InnValidator.isValidIndividual(v))
      return new Result(true, "INDIVIDUAL", "Valid INN checksum");
    if (InnValidator.isValidLegalEntity(v))
      return new Result(true, "LEGAL_ENTITY", "Valid INN checksum");
    return new Result(false, null, "Invalid INN checksum or length");
  }

  private static Result validateOgrn(String v) {
    if (OgrnValidator.isValidLegalEntity(v))
      return new Result(true, "LEGAL_ENTITY", "Valid OGRN checksum");
    if (OgrnValidator.isValidIndividualEntrepreneur(v))
      return new Result(true, "INDIVIDUAL_ENTREPRENEUR", "Valid OGRNIP checksum");
    return new Result(false, null, "Invalid OGRN checksum or length");
  }

  private static Result validateOkpo(String v) {
    if (v != null && v.length() == 8 && OkpoValidator.isValidLegalEntity(v))
      return new Result(true, "LEGAL_ENTITY", "Valid OKPO (ЮЛ)");
    if (v != null && v.length() == 10 && OkpoValidator.isValidIndividualEntrepreneur(v))
      return new Result(true, "INDIVIDUAL_ENTREPRENEUR", "Valid OKPO (ИП)");
    return new Result(false, null, "Invalid OKPO checksum or length");
  }

  private static Result validateBik(String v) {
    BikSource source = CliBik.source();
    BikValidator validator = new BikValidator(source);
    Bank bank = validator.getBank(v);
    if (bank != null) return new Result(true, "BANK", bank.name());
    return new Result(false, null, "Unknown or invalid BIK");
  }

  private record Result(boolean valid, String type, String details) {}
}
