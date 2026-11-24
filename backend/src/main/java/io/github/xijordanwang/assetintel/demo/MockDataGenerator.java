package io.github.xijordanwang.assetintel.demo;

import io.github.xijordanwang.assetintel.asset.Asset;
import io.github.xijordanwang.assetintel.asset.AssetCategory;
import io.github.xijordanwang.assetintel.asset.AssetStatus;
import io.github.xijordanwang.assetintel.site.Site;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a fictional New Zealand manufacturer, "Kowhai Components Ltd", with four plants and about
 * fifty sites. Everything is random but repeatable for a given seed. No real company data is used.
 */
public class MockDataGenerator {

    record Town(String name, String region, double lat, double lon) {
    }

    record Plant(String code, Town home) {
    }

    static final List<Town> TOWNS = List.of(
            new Town("Auckland", "Auckland", -36.8485, 174.7633),
            new Town("Hamilton", "Waikato", -37.7870, 175.2793),
            new Town("Christchurch", "Canterbury", -43.5321, 172.6362),
            new Town("Wellington", "Wellington", -41.2866, 174.7756),
            new Town("Tauranga", "Bay of Plenty", -37.6878, 176.1651),
            new Town("Dunedin", "Otago", -45.8788, 170.5028),
            new Town("Palmerston North", "Manawatu-Whanganui", -40.3523, 175.6082),
            new Town("Napier", "Hawke's Bay", -39.4928, 176.9120),
            new Town("New Plymouth", "Taranaki", -39.0556, 174.0752),
            new Town("Nelson", "Nelson", -41.2706, 173.2840),
            new Town("Rotorua", "Bay of Plenty", -38.1368, 176.2497),
            new Town("Whangarei", "Northland", -35.7251, 174.3237),
            new Town("Invercargill", "Southland", -46.4132, 168.3538),
            new Town("Timaru", "Canterbury", -44.3970, 171.2550),
            new Town("Gisborne", "Gisborne", -38.6623, 178.0176),
            new Town("Blenheim", "Marlborough", -41.5134, 173.9612),
            new Town("Whanganui", "Manawatu-Whanganui", -39.9301, 175.0479),
            new Town("Taupo", "Waikato", -38.6857, 176.0702),
            new Town("Cambridge", "Waikato", -37.8915, 175.4697),
            new Town("Ashburton", "Canterbury", -43.9050, 171.7470));

    static final List<Plant> PLANTS = List.of(
            new Plant("AKL", TOWNS.get(0)),
            new Plant("HLZ", TOWNS.get(1)),
            new Plant("CHC", TOWNS.get(2)),
            new Plant("WLG", TOWNS.get(3)));

    private static final String[] SITE_KINDS = {"Warehouse", "Service Centre", "Training Centre", "Office", "Depot"};

    private static final String[] SUPPLIERS = {
            "Harbourline Engineering", "Tui Automation", "Southern Alps Steel", "Kauri IT Services",
            "Ruru Logistics", "Pohutukawa Fitouts", "Matai Electrical", "Kea Robotics", "Totara Construction",
            "Fernleaf Fleet", "Rimu Office Supply", "Kiwi Precision Tools", "Waka Industrial", "Moana Systems"};

    private static final String[][] NAMES = {
            /* BUILDING */ {"Assembly hall", "Warehouse building", "Office block", "Paint shop", "Training facility"},
            /* PLANT_MACHINERY */ {"CNC milling machine", "Robotic welding cell", "Hydraulic press", "Conveyor line",
                    "Injection moulding machine", "Air compressor", "Paint robot", "Laser cutter"},
            /* IT_EQUIPMENT */ {"Laptop", "Server rack", "Network switch", "Industrial PC", "Barcode scanner", "Firewall"},
            /* VEHICLE */ {"Forklift", "Delivery van", "Utility vehicle", "Electric tow tractor"},
            /* FURNITURE */ {"Workstation desk set", "Meeting room fit-out", "Storage racking", "Canteen fit-out"},
            /* LAND */ {"Freehold land"},
            /* CONSTRUCTION_IN_PROGRESS */ {"Solar array (in construction)", "New dock (in construction)"}};

    private final Random random;

    public MockDataGenerator(long seed) {
        this.random = new Random(seed);
    }

    public List<Site> sites() {
        List<Site> sites = new ArrayList<>();
        for (Plant plant : PLANTS) {
            sites.add(new Site(plant.code() + "-01", "Kowhai Components " + plant.home().name() + " Plant",
                    plant.code(), plant.home().name(), plant.home().region(),
                    plant.home().lat(), plant.home().lon(), money(4_000_000, 9_000_000)));
        }
        int index = 0;
        for (int i = 0; i < 44; i++) {
            Plant plant = PLANTS.get(i % PLANTS.size());
            Town town = TOWNS.get(4 + (index++ % (TOWNS.size() - 4)));
            String kind = SITE_KINDS[random.nextInt(SITE_KINDS.length)];
            int number = 2 + i / PLANTS.size();
            sites.add(new Site("%s-%02d".formatted(plant.code(), number), town.name() + " " + kind,
                    plant.code(), town.name(), town.region(),
                    jitter(town.lat()), jitter(town.lon()), money(300_000, 1_500_000)));
        }
        return sites;
    }

    public List<Asset> assets(List<Site> sites, int count, LocalDate today) {
        List<Asset> assets = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            AssetCategory category = pickCategory();
            Site site = category == AssetCategory.BUILDING || category == AssetCategory.LAND
                    ? sites.get(random.nextInt(sites.size()))
                    : weightedTowardPlants(sites);
            String[] names = NAMES[category.ordinal()];
            boolean cip = category == AssetCategory.CONSTRUCTION_IN_PROGRESS;
            LocalDate inService = cip ? null : today.minusDays(random.nextInt(15 * 365));
            Asset asset = new Asset(
                    "AST-%06d".formatted(i),
                    names[random.nextInt(names.length)],
                    category,
                    cip ? AssetStatus.PENDING : pickStatus(),
                    site,
                    category == AssetCategory.LAND ? null : SUPPLIERS[random.nextInt(SUPPLIERS.length)],
                    valueFor(category),
                    inService);
            if (!cip && random.nextDouble() < 0.02) {
                asset.recordImpairment(asset.getOriginalValue()
                        .multiply(BigDecimal.valueOf(0.1 + random.nextDouble() * 0.3))
                        .setScale(2, RoundingMode.HALF_UP));
            }
            if (!cip && random.nextDouble() < 0.8) {
                asset.markVerified(today.minusDays(random.nextInt(500)));
            }
            assets.add(asset);
        }
        return assets;
    }

    private AssetCategory pickCategory() {
        double r = random.nextDouble();
        if (r < 0.03) return AssetCategory.BUILDING;
        if (r < 0.33) return AssetCategory.PLANT_MACHINERY;
        if (r < 0.68) return AssetCategory.IT_EQUIPMENT;
        if (r < 0.80) return AssetCategory.VEHICLE;
        if (r < 0.96) return AssetCategory.FURNITURE;
        if (r < 0.98) return AssetCategory.LAND;
        return AssetCategory.CONSTRUCTION_IN_PROGRESS;
    }

    private AssetStatus pickStatus() {
        double r = random.nextDouble();
        if (r < 0.90) return AssetStatus.IN_SERVICE;
        if (r < 0.96) return AssetStatus.UNDER_MAINTENANCE;
        return AssetStatus.DISPOSED;
    }

    private BigDecimal valueFor(AssetCategory category) {
        return switch (category) {
            case BUILDING -> money(2_000_000, 25_000_000);
            case LAND -> money(1_000_000, 12_000_000);
            case CONSTRUCTION_IN_PROGRESS -> money(200_000, 3_000_000);
            case PLANT_MACHINERY -> money(40_000, 1_800_000);
            case VEHICLE -> money(25_000, 120_000);
            case IT_EQUIPMENT -> money(1_200, 60_000);
            case FURNITURE -> money(2_000, 80_000);
        };
    }

    /** Machinery and equipment sit mostly at the four main plants. */
    private Site weightedTowardPlants(List<Site> sites) {
        return random.nextDouble() < 0.6 ? sites.get(random.nextInt(PLANTS.size())) : sites.get(random.nextInt(sites.size()));
    }

    private BigDecimal money(double min, double max) {
        return BigDecimal.valueOf(min + random.nextDouble() * (max - min)).setScale(2, RoundingMode.HALF_UP);
    }

    private double jitter(double coordinate) {
        return Math.round((coordinate + (random.nextDouble() - 0.5) * 0.08) * 10_000) / 10_000.0;
    }

    Random random() {
        return random;
    }
}
