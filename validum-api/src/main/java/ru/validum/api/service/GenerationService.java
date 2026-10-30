package ru.validum.api.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.validum.api.dto.*;
import ru.validum.api.mapper.ProfileMapper;
import ru.validum.core.bank.BikSource;
import ru.validum.core.common.Country;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.common.Region;
import ru.validum.core.document.DocumentType;
import ru.validum.core.name.FamilyStatus;
import ru.validum.core.name.FioScript;
import ru.validum.core.name.Gender;
import ru.validum.core.organization.IpProfile;
import ru.validum.core.organization.IpProfileGenerator;
import ru.validum.core.organization.OrganizationProfile;
import ru.validum.core.organization.OrganizationProfileGenerator;
import ru.validum.core.passport.PassportType;
import ru.validum.core.profile.CorruptField;
import ru.validum.core.profile.IndividualProfile;
import ru.validum.core.profile.IndividualProfileGenerator;
import ru.validum.core.profile.Residency;

@Service
public class GenerationService {

  private final ProfileMapper mapper;
  private final BikSource bikSource;

  public GenerationService(ProfileMapper mapper, BikSource bikSource) {
    this.mapper = mapper;
    this.bikSource = bikSource;
  }

  // ── ФЛ ────────────────────────────────────────────────────────────────────

  public IndividualProfileDto generateIndividual(IndividualGenerationRequest request) {
    return mapper.toDto(generateIndividualRaw(request));
  }

  /** Raw-версия — core-модель без маппинга в DTO. Используется для CSV-пути. */
  public IndividualProfile generateIndividualRaw(IndividualGenerationRequest request) {
    IndividualProfileGenerator.Builder builder =
      IndividualProfileGenerator.builder().bikSource(bikSource);

    if (request.seed() != null) {
      builder.seed(request.seed());
    }
    if (request.asOf() != null) {
      builder.asOf(request.asOf());
    }
    if (request.gender() != null) {
      builder.gender(Gender.valueOf(request.gender()));
    }
    if (request.age() != null) {
      builder.age(request.age());
    }
    if (request.passportWasExchanged() != null) {
      builder.passportWasExchanged(request.passportWasExchanged());
    }
    if (request.residencyCountry() != null && !request.residencyCountry().isBlank()) {
      Country country = Country.valueOf(request.residencyCountry());
      builder.residency(Residency.nonResident(country));
      builder.passportType(PassportType.FOREIGN).passportCountry(country);
    }
    if (request.passportType() != null) {
      builder.passportType(PassportType.valueOf(request.passportType()));
    }
    if (request.fioScript() != null) {
      builder.fioScript(FioScript.valueOf(request.fioScript()));
    }
    if (request.corrupt() != null && !request.corrupt().isEmpty()) {
      builder.corrupt(
        request.corrupt().stream().map(CorruptField::valueOf).toArray(CorruptField[]::new));
    }
    if (request.region() != null && !request.region().isBlank()) {
      builder.region(Region.require(request.region()));
    }
    if (request.categoryClient() != null && !request.categoryClient().isBlank()) {
      builder.categoryClient(request.categoryClient());
    }
    if (request.accountPackage() != null && !request.accountPackage().isBlank()) {
      builder.accountPackage(request.accountPackage());
    }
    if (request.familyStatus() != null && !request.familyStatus().isBlank()) {
      builder.familyStatus(FamilyStatus.valueOf(request.familyStatus()));
    }
    if (request.terrorist() != null) {
      builder.terrorist(request.terrorist());
    }
    if (request.blocked() != null) {
      builder.blocked(request.blocked());
    }
    // P3.3: явный БИК
    if (request.bik() != null && !request.bik().isBlank()) {
      builder.bik(request.bik());
    }
    // ДУЛ: дополнительные документы (СПДУЛ ФНС)
    if (request.documents() != null && !request.documents().isEmpty()) {
      builder.documents(request.documents().stream().map(DocumentType::valueOf).toList());
    }

    return builder.build().generate();
  }

  public IndividualBatchResponse generateIndividualBatch(IndividualBatchRequest request) {
    long baseSeed = resolveBaseSeed(request.seed());
    List<IndividualProfile> raw = generateIndividualBatchRaw(request, baseSeed);
    List<IndividualProfileDto> dtos = raw.stream().map(mapper::toDto).toList();
    return new IndividualBatchResponse(request.count(), baseSeed, dtos);
  }

  /** Raw-версия батча — core-модели без маппинга. Используется для CSV-пути. */
  public List<IndividualProfile> generateIndividualBatchRaw(IndividualBatchRequest request) {
    return generateIndividualBatchRaw(request, resolveBaseSeed(request.seed()));
  }

  private List<IndividualProfile> generateIndividualBatchRaw(
    IndividualBatchRequest request, long baseSeed) {
    List<IndividualProfile> profiles = new ArrayList<>(request.count());
    for (int i = 0; i < request.count(); i++) {
      IndividualGenerationRequest single = new IndividualGenerationRequest(
        request.gender(), baseSeed + i, request.asOf(), request.age(),
        request.passportWasExchanged(),
        request.residencyCountry(), request.passportType(), request.fioScript(),
        request.corrupt(), request.region(), request.categoryClient(),
        request.accountPackage(), request.familyStatus(), request.terrorist(), request.blocked(),
        request.bik(), null
      );
      profiles.add(generateIndividualRaw(single));
    }
    return profiles;
  }

  // ── ИП ────────────────────────────────────────────────────────────────────

  public IpProfileDto generateIp(IpGenerationRequest request) {
    return mapper.toDto(generateIpRaw(request));
  }

  /** Raw-версия — core-модель без маппинга в DTO. */
  public IpProfile generateIpRaw(IpGenerationRequest request) {
    long seed = resolveBaseSeed(request.seed());

    IndividualProfileGenerator.Builder builder =
      IndividualProfileGenerator.builder().bikSource(bikSource).seed(seed);

    if (request.asOf() != null) {
      builder.asOf(request.asOf());
    }
    if (request.gender() != null) {
      builder.gender(Gender.valueOf(request.gender()));
    }
    if (request.age() != null) {
      builder.age(request.age());
    }
    if (request.region() != null && !request.region().isBlank()) {
      builder.region(Region.require(request.region()));
    }
    if (request.categoryClient() != null && !request.categoryClient().isBlank()) {
      builder.categoryClient(request.categoryClient());
    }
    if (request.accountPackage() != null && !request.accountPackage().isBlank()) {
      builder.accountPackage(request.accountPackage());
    }
    if (request.terrorist() != null) {
      builder.terrorist(request.terrorist());
    }
    if (request.blocked() != null) {
      builder.blocked(request.blocked());
    }
    // P3.3: явный БИК
    if (request.bik() != null && !request.bik().isBlank()) {
      builder.bik(request.bik());
    }

    if (request.okved() != null && !request.okved().isBlank()) {
      return IpProfileGenerator.generate(builder.build(), seed, request.okved());
    }
    return IpProfileGenerator.generate(builder.build(), seed);
  }

  public IpBatchResponse generateIpBatch(IpBatchRequest request) {
    long baseSeed = resolveBaseSeed(request.seed());
    List<IpProfile> raw = generateIpBatchRaw(request, baseSeed);
    List<IpProfileDto> dtos = raw.stream().map(mapper::toDto).toList();
    return new IpBatchResponse(request.count(), baseSeed, dtos);
  }

  /** Raw-версия батча — core-модели без маппинга. */
  public List<IpProfile> generateIpBatchRaw(IpBatchRequest request) {
    return generateIpBatchRaw(request, resolveBaseSeed(request.seed()));
  }

  private List<IpProfile> generateIpBatchRaw(IpBatchRequest request, long baseSeed) {
    List<IpProfile> profiles = new ArrayList<>(request.count());
    for (int i = 0; i < request.count(); i++) {
      IpGenerationRequest single = new IpGenerationRequest(
        baseSeed + i, request.asOf(), request.gender(), request.age(), request.region(),
        request.categoryClient(), request.accountPackage(), request.terrorist(), request.blocked(),
        request.bik(), request.okved()
      );
      profiles.add(generateIpRaw(single));
    }
    return profiles;
  }

  // ── ЮЛ (без bank в профиле — P3.3 не трогает) ────────────────────────────

  public OrganizationProfileDto generateOrganization(OrganizationGenerationRequest request) {
    return mapper.toDto(generateOrganizationRaw(request));
  }

  /** Raw-версия — core-модель без маппинга в DTO. */
  public OrganizationProfile generateOrganizationRaw(OrganizationGenerationRequest request) {
    OrganizationProfileGenerator.Builder builder = OrganizationProfileGenerator.builder();

    if (request.seed() != null) {
      builder.seed(request.seed());
    }
    if (request.organizationType() != null && !request.organizationType().isBlank()) {
      builder.organizationType(OrganizationType.valueOf(request.organizationType()));
    }
    if (request.organizationName() != null && !request.organizationName().isBlank()) {
      builder.organizationName(request.organizationName());
    }
    if (request.region() != null && !request.region().isBlank()) {
      builder.region(Region.require(request.region()));
    }
    if (request.okved() != null && !request.okved().isBlank()) {
      builder.okved(request.okved());
    }
    if (request.bik() != null && !request.bik().isBlank()) {
      builder.bik(request.bik());
    }
    if (request.terrorist() != null) {
      builder.terrorist(request.terrorist());
    }
    if (request.blocked() != null) {
      builder.blocked(request.blocked());
    }

    return builder.build().generate();
  }

  public OrganizationBatchResponse generateOrganizationBatch(OrganizationBatchRequest request) {
    long baseSeed = resolveBaseSeed(request.seed());
    List<OrganizationProfile> raw = generateOrganizationBatchRaw(request, baseSeed);
    List<OrganizationProfileDto> dtos = raw.stream().map(mapper::toDto).toList();
    return new OrganizationBatchResponse(request.count(), baseSeed, dtos);
  }

  /** Raw-версия батча — core-модели без маппинга. */
  public List<OrganizationProfile> generateOrganizationBatchRaw(OrganizationBatchRequest request) {
    return generateOrganizationBatchRaw(request, resolveBaseSeed(request.seed()));
  }

  private List<OrganizationProfile> generateOrganizationBatchRaw(
    OrganizationBatchRequest request, long baseSeed) {
    List<OrganizationProfile> profiles = new ArrayList<>(request.count());
    for (int i = 0; i < request.count(); i++) {
      OrganizationGenerationRequest single = new OrganizationGenerationRequest(
        baseSeed + i, request.organizationType(), request.organizationName(),
        request.region(), request.okved(), request.bik(), request.terrorist(), request.blocked()
      );
      profiles.add(generateOrganizationRaw(single));
    }
    return profiles;
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  private static long resolveBaseSeed(Long seed) {
    return seed != null ? seed : System.nanoTime();
  }
}
