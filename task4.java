package software_patterns.week10;

public class task4 {

}
package task4;

// =============================================================
// Task 4: International Order Pricing Pipeline with Legacy Currency Adapters
// Combined Patterns: Adapter Pattern + Decorator Pattern
// =============================================================

// --- 1. Legacy Currency & Order Classes (Adaptees) ---
class LegacyForeignOrder {
    private final String orderId;
    private final double gbpAmount;

    public LegacyForeignOrder(String orderId, double gbpAmount) {
        this.orderId = orderId;
        this.gbpAmount = gbpAmount;
    }

    public String getOrderId() {
        return orderId;
    }

    public double getGbpAmount() {
        return gbpAmount;
    }
}

class LegacyCurrencyConverter {
    public double convert(double amount, String from, String to) {
        if ("GBP".equalsIgnoreCase(from) && "USD".equalsIgnoreCase(to)) {
            return amount * 1.25; // 1 GBP = 1.25 USD[cite: 8]
        }
        return amount;
    }
}

// --- 2. Target Pricing Interface ---
interface PricingService {
    double calculateTotal();

    String getReceiptBreakdown();
}

// --- 3. Adapter: Adapts LegacyForeignOrder to PricingService (in USD) ---
class CurrencyOrderAdapter implements PricingService {
    private final LegacyForeignOrder foreignOrder;
    private final LegacyCurrencyConverter converter;
    private final String targetCurrency;

    public CurrencyOrderAdapter(LegacyForeignOrder order, LegacyCurrencyConverter converter, String targetCurrency) {
        this.foreignOrder = order;
        this.converter = converter;
        this.targetCurrency = targetCurrency;
    }

    @Override
    public double calculateTotal() {
        return converter.convert(foreignOrder.getGbpAmount(), "GBP", targetCurrency);
    }

    @Override
    public String getReceiptBreakdown() {
        return String.format("Order [%s] Base Item: $%.2f %s (Converted from %.2f GBP)",
                foreignOrder.getOrderId(), calculateTotal(), targetCurrency, foreignOrder.getGbpAmount());
    }
}

// --- 4. Decorator Pattern: Pricing Decorator & Concrete Modifiers ---
abstract class PricingDecorator implements PricingService {
    protected final PricingService wrappedService;

    public PricingDecorator(PricingService wrappedService) {
        if (wrappedService == null)
            throw new IllegalArgumentException("Wrapped service cannot be null");
        this.wrappedService = wrappedService;
    }

    @Override
    public double calculateTotal() {
        return wrappedService.calculateTotal();
    }

    @Override
    public String getReceiptBreakdown() {
        return wrappedService.getReceiptBreakdown();
    }
}

class GiftWrapDecorator extends PricingDecorator {
    public GiftWrapDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() + 5.00; // Flat $5[cite: 9]
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Gift Wrap: $5.00]";
    }
}

class ExpressCourierDecorator extends PricingDecorator {
    public ExpressCourierDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        return super.calculateTotal() + 15.00; // Flat $15[cite: 9]
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Express Courier: $15.00]";
    }
}

class CustomsDutyDecorator extends PricingDecorator {
    public CustomsDutyDecorator(PricingService wrapped) {
        super(wrapped);
    }

    @Override
    public double calculateTotal() {
        double subtotal = super.calculateTotal();
        return Math.round((subtotal * 1.085) * 100.0) / 100.0; // 8.5% customs tax[cite: 9]
    }

    @Override
    public String getReceiptBreakdown() {
        return super.getReceiptBreakdown() + " + [Customs Duty: 8.5%]";
    }
}

// --- 5. Verification Runner ---
public class Task4Runner {
    public static void main(String[] args) {
        LegacyForeignOrder order = new LegacyForeignOrder("ORD-9821", 100.00); // 100 GBP -> 125 USD[cite: 9]
        LegacyCurrencyConverter converter = new LegacyCurrencyConverter();
        PricingService baseOrder = new CurrencyOrderAdapter(order, converter, "USD");
        System.out.printf("Base Converted: $%.2f%n", baseOrder.calculateTotal());

        // Decorate: Base + GiftWrap ($5) + Express ($15) -> $145.00 -> Customs 8.5% -> $157.33[cite: 9]
        PricingService completeCheckout = new CustomsDutyDecorator(
            new ExpressCourierDecorator(
                new GiftWrapDecorator(baseOrder)
            )
        );

        System.out.println("\n>>> INVOICE BREAKDOWN <<<");
        System.out.println(completeCheckout.getReceiptBreakdown());
        System.out.printf("FINAL CHARGE TOTAL: $%.2f%n", completeCheckout.calculateTotal());
    }
}```[cite:7,8,9]