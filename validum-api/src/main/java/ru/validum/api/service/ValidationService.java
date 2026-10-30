package ru.validum.api.service;

import org.springframework.stereotype.Service;
import ru.validum.api.dto.*;
import ru.validum.core.account.AccountValidator;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikSource;
import ru.validum.core.validation.*;

@Service
public class ValidationService {

  private final BikValidator bikValidator;

  public ValidationService(BikSource bikSource) {
    this.bikValidator = new BikValidator(bikSource);
  }

  public ValidationResponse validateInn(InnValidationRequest req) {
    if (req == null || req.inn() == null) return new ValidationResponse(false, null, "Empty INN");
    if (InnValidator.isValidIndividual(req.inn()))
      return new ValidationResponse(true, "INDIVIDUAL", "Valid INN checksum");
    if (InnValidator.isValidLegalEntity(req.inn()))
      return new ValidationResponse(true, "LEGAL_ENTITY", "Valid INN checksum");
    return new ValidationResponse(false, null, "Invalid INN checksum or length");
  }

  public ValidationResponse validateSnils(SnilsValidationRequest req) {
    if (req == null || req.snils() == null) return new ValidationResponse(false, null, "Empty SNILS");
    if (SnilsValidator.isValid(req.snils()))
      return new ValidationResponse(true, null, "Valid SNILS checksum");
    return new ValidationResponse(false, null, "Invalid SNILS checksum or length");
  }

  public ValidationResponse validateOgrn(OgrnValidationRequest req) {
    if (req == null || req.ogrn() == null) return new ValidationResponse(false, null, "Empty OGRN");
    if (OgrnValidator.isValidLegalEntity(req.ogrn()))
      return new ValidationResponse(true, "LEGAL_ENTITY", "Valid OGRN checksum");
    if (OgrnValidator.isValidIndividualEntrepreneur(req.ogrn()))
      return new ValidationResponse(true, "INDIVIDUAL_ENTREPRENEUR", "Valid OGRNIP checksum");
    return new ValidationResponse(false, null, "Invalid OGRN checksum or length");
  }

  public ValidationResponse validateAccount(AccountValidationRequest req) {
    if (req == null || req.account() == null) return new ValidationResponse(false, null, "Empty account");
    String account = req.account();
    if (!AccountValidator.isWellFormed(account)) {
      return new ValidationResponse(
        false, null, "Invalid account format: 20 знаков, в 6-м разряде допускается буква валюты");
    }
    if (req.bik() == null || req.bik().isBlank()) {
      return new ValidationResponse(false, null, "Укажите BIK: контрольный ключ зависит от БИК банка");
    }
    if (AccountValidator.isValidAuto(account, req.bik())) {
      return new ValidationResponse(true, "RUB", "Valid account checksum (письмо 515 / 809-П)");
    }
    return new ValidationResponse(false, null, "Invalid account checksum or BIK");
  }

  public ValidationResponse validateKpp(KppValidationRequest req) {
    if (req == null || req.kpp() == null) return new ValidationResponse(false, null, "Empty KPP");
    if (KppValidator.isValid(req.kpp()))
      return new ValidationResponse(true, null, "Valid KPP format");
    return new ValidationResponse(false, null, "Invalid KPP format or region code");
  }

  public ValidationResponse validateBik(BikValidationRequest req) {
    if (req == null || req.bik() == null) return new ValidationResponse(false, null, "Empty BIK");
    Bank bank = bikValidator.getBank(req.bik());
    if (bank != null) return new ValidationResponse(true, "BANK", bank.name());
    return new ValidationResponse(false, null, "Unknown or invalid BIK");
  }

  // ── P2.3 — ОКПО / ОГРНИП ──────────────────────────────────────────────────

  /**
   * Валидация ОКПО. Тип определяется по длине: 8 цифр — ЮЛ, 10 цифр — ИП.
   */
  public ValidationResponse validateOkpo(OkpoValidationRequest req) {
    if (req == null || req.okpo() == null)
      return new ValidationResponse(false, null, "Empty OKPO");
    String okpo = req.okpo();
    if (okpo.length() == 8 && OkpoValidator.isValidLegalEntity(okpo)) {
      return new ValidationResponse(true, "LEGAL_ENTITY", "Valid OKPO checksum (ЮЛ)");
    }
    if (okpo.length() == 10 && OkpoValidator.isValidIndividualEntrepreneur(okpo)) {
      return new ValidationResponse(true, "INDIVIDUAL_ENTREPRENEUR", "Valid OKPO checksum (ИП)");
    }
    return new ValidationResponse(false, null, "Invalid OKPO checksum or length");
  }

  /**
   * Валидация ОГРНИП (15 цифр). Отдельный эндпоинт от {@link #validateOgrn}, чтобы не путать
   * с ОГРН ЮЛ.
   */
  public ValidationResponse validateOgrnip(OgrnipValidationRequest req) {
    if (req == null || req.ogrnip() == null)
      return new ValidationResponse(false, null, "Empty OGRNIP");
    if (OgrnipValidator.isValid(req.ogrnip()))
      return new ValidationResponse(true, "INDIVIDUAL_ENTREPRENEUR", "Valid OGRNIP checksum");
    return new ValidationResponse(false, null, "Invalid OGRNIP checksum or length");
  }
}
