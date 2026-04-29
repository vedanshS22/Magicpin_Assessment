package com.magicpin.mde.domain.decision.category;

import com.magicpin.mde.domain.model.Category;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CategoryStrategyFactory {
  private final Map<Category, CategoryStrategy> strategies;

  public CategoryStrategyFactory(
      DentistStrategy dentistStrategy,
      SalonStrategy salonStrategy,
      RestaurantStrategy restaurantStrategy,
      GymStrategy gymStrategy,
      PharmacyStrategy pharmacyStrategy,
      OtherStrategy otherStrategy) {
    EnumMap<Category, CategoryStrategy> m = new EnumMap<>(Category.class);
    m.put(Category.DENTIST, dentistStrategy);
    m.put(Category.SALON, salonStrategy);
    m.put(Category.RESTAURANT, restaurantStrategy);
    m.put(Category.GYM, gymStrategy);
    m.put(Category.PHARMACY, pharmacyStrategy);
    m.put(Category.OTHER, otherStrategy);
    this.strategies = Map.copyOf(m);
  }

  public CategoryStrategy forCategory(Category c) {
    return strategies.getOrDefault(c, strategies.get(Category.OTHER));
  }
}

