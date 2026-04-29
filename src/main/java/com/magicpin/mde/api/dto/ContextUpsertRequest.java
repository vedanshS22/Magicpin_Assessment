package com.magicpin.mde.api.dto;

import com.magicpin.mde.domain.model.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class ContextUpsertRequest {
  @Valid @NotNull private MerchantIdentityDto merchant;
  @NotNull @Min(0) private Long version;
  @NotNull private Category category;

  @Valid @NotNull private MerchantPerformanceDto performance;
  @Valid private TriggerContextDto triggerContext;

  @Valid @NotNull private List<OfferDto> offers = new ArrayList<>();

  private List<String> replyHistory; // free-form, last few replies
}

