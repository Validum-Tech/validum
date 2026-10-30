package ru.validum.core.organization;

import java.util.Random;
import ru.validum.core.account.AccountGenerator;
import ru.validum.core.account.AccountType;
import ru.validum.core.address.Address;
import ru.validum.core.address.AddressGenerator;
import ru.validum.core.bank.Bank;
import ru.validum.core.bank.BikGenerator;
import ru.validum.core.bank.BikSource;
import ru.validum.core.bank.BikSources;
import ru.validum.core.common.OrganizationType;
import ru.validum.core.common.Region;
import ru.validum.core.contact.Contact;
import ru.validum.core.contact.ContactGenerator;
import ru.validum.core.contact.ContactType;
import ru.validum.core.contact.EmailDomainMode;
import ru.validum.core.inn.InnGenerator;
import ru.validum.core.ogrn.OgrnGenerator;
import ru.validum.core.okved.OkvedEntry;
import ru.validum.core.okved.OkvedGenerator;
import ru.validum.core.okved.OkvedRegistry;
import ru.validum.core.random.DataGenerator;

/**
 * Генератор связанного профиля ЮЛ.
 *
 * <p><b>Каскадные дефолты:</b>
 *
 * <ul>
 *   <li>{@code region} не задан → случайный из {@link Region};
 *   <li>{@code organizationType} не задан → {@link OrganizationType#LLC};
 *   <li>{@code organizationName} не задан → генерируется из типа и набора слов;
 *   <li>{@code kpp} — всегда привязан к региону (первые 2 цифры);
 *   <li>{@code okfs} и {@code budget} — из {@link OrganizationType} (P2.2).
 * </ul>
 *
 * <p>Формы собственности и бюджетный статус различаются по ОПФ:
 *
 * <ul>
 *   <li>LLC / JSC / PJSC / NCO / FOUNDATION → частная, небюджетная;
 *   <li>SE (ГУП) → собственность субъекта РФ, бюджетная;
 *   <li>ME (МУП) → муниципальная, бюджетная.
 * </ul>
 */
public final class OrganizationProfileGenerator implements DataGenerator {

  // === Классификаторы-константы, не зависящие от ОПФ ===
  private static final String OKOGU_ECONOMIC = "4210014"; // Хозяйственные общества
  private static final String SWIFT_DEFAULT = "SABRRUMMXXX";

  private final Random random;
  private final OrganizationType organizationType;
  private final String organizationName;
  private final String okved;
  private final Region region;
  private final String email;
  private final EmailDomainMode emailDomainMode;
  private final boolean terrorist;
  private final boolean blocked;
  private final BikSource bikSource;
  private final String bik;
  private final Bank bank;

  private final OrganizationNameGenerator nameGen;
  private final KppGenerator kppGen;
  private final OkpoGenerator okpoGen;
  private final OkvedGenerator okvedGen;
  private final OgrnGenerator ogrnGen;
  private final InnGenerator innGen;
  private final ContactGenerator contactGen;
  private final BikGenerator bikGen;
  private final AccountGenerator accountGen;

  private OrganizationProfileGenerator(Builder b) {
    this.random = b.random;
    this.organizationType = b.organizationType;
    this.organizationName = b.organizationName;
    this.okved = b.okved;
    this.region = b.region;
    this.email = b.email;
    this.emailDomainMode = b.emailDomainMode;
    this.terrorist = b.terrorist;
    this.blocked = b.blocked;
    this.bikSource = b.bikSource;
    this.bik = b.bik;
    this.bank = b.bank;

    this.nameGen = new OrganizationNameGenerator(random);
    this.kppGen = new KppGenerator(random);
    this.okpoGen = new OkpoGenerator(random);
    this.okvedGen = new OkvedGenerator(random);
    this.ogrnGen = new OgrnGenerator(random);
    this.innGen = new InnGenerator(random);
    this.contactGen = new ContactGenerator(random);
    this.bikGen = new BikGenerator(random, bikSource);
    this.accountGen = new AccountGenerator(random);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static OrganizationProfileGenerator defaults() {
    return builder().build();
  }

  public OrganizationProfile generate() {
    OrganizationType type = organizationType != null ? organizationType : OrganizationType.LLC;
    Region rgn = region != null ? region : randomRegion();

    String name = organizationName != null ? organizationName : nameGen.generate(type);

    String ogrn = ogrnGen.generateLegalEntity();
    String inn = innGen.generateLegalEntity();
    String kpp = kppGen.generate(rgn);
    String okpo = okpoGen.generateLegalEntity();

    OkvedEntry okvedEntry = okved != null ? OkvedRegistry.require(okved) : okvedGen.generate();

    AddressGenerator addressGen = new AddressGenerator(random);
    Address registration = addressGen.generateRegistration(rgn);
    Address post = addressGen.generatePost(rgn);

    Contact mobile = contactGen.mobile();
    Bank bankInfo = resolveBank();
    String account = accountGen.generate("40702", AccountType.CLAC, bankInfo.bik());

    Contact emailContact = email != null
      ? new Contact(ContactType.EMAIL, email, "Описание для email", "Действительный", true)
      : contactGen.email(emailDomainMode);

    return new OrganizationProfile(
      name,
      type,
      ogrn,
      inn,
      kpp,
      okpo,
      type.okopf(),
      type.okfs(),
      OKOGU_ECONOMIC,
      okvedEntry.code(),
      okvedEntry.name(),
      type.budget(),
      SWIFT_DEFAULT,
      registration,
      post,
      mobile,
      emailContact,
      terrorist,
      blocked,
      bankInfo,
      account);
  }

  /** Банк ЮЛ: явный объект → поиск по БИК → случайный из справочника. */
  private Bank resolveBank() {
    if (bank != null) {
      return bank;
    }
    if (bik != null) {
      return bikSource
        .findByBik(bik)
        .orElseThrow(() -> new IllegalArgumentException("Банк с БИК " + bik + " не найден в справочнике"));
    }
    return bikGen.generateBank();
  }

  private Region randomRegion() {
    Region[] regions = Region.values();
    return regions[random.nextInt(regions.length)];
  }

  /** Builder с дефолтами и каскадной логикой. */
  public static final class Builder {

    private Random random = DataGenerator.defaultRandom();
    private OrganizationType organizationType;
    private String organizationName;
    private String okved;
    private Region region;
    private String email;
    private EmailDomainMode emailDomainMode = EmailDomainMode.TEST;
    private boolean terrorist = false;
    private boolean blocked = false;
    private BikSource bikSource = BikSources.defaultSource();
    private String bik;
    private Bank bank;

    private Builder() {}

    public Builder seed(long seed) {
      this.random = DataGenerator.seededRandom(seed);
      return this;
    }

    public Builder random(Random random) {
      this.random = random;
      return this;
    }

    public Builder organizationType(OrganizationType type) {
      this.organizationType = type;
      return this;
    }

    public Builder organizationName(String name) {
      this.organizationName = name;
      return this;
    }

    /** Фиксированный код ОКВЭД-2 (проверяется по справочнику при генерации). */
    public Builder okved(String code) {
      this.okved = code;
      return this;
    }

    public Builder region(Region region) {
      this.region = region;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder emailDomainMode(EmailDomainMode mode) {
      this.emailDomainMode = mode;
      return this;
    }

    public Builder terrorist(boolean terrorist) {
      this.terrorist = terrorist;
      return this;
    }

    public Builder blocked(boolean blocked) {
      this.blocked = blocked;
      return this;
    }

    /** Источник справочника БИК (по умолчанию — {@link BikSources#defaultSource()}). */
    public Builder bikSource(BikSource bikSource) {
      this.bikSource = bikSource;
      return this;
    }

    /** Фиксированный БИК банка ЮЛ; неизвестный БИК → {@link IllegalArgumentException}. */
    public Builder bik(String bik) {
      this.bik = bik;
      return this;
    }

    /** Готовый объект банка (приоритет выше, чем {@link #bik(String)}). */
    public Builder bank(Bank bank) {
      this.bank = bank;
      return this;
    }

    public OrganizationProfileGenerator build() {
      return new OrganizationProfileGenerator(this);
    }
  }
}
