package com.barbu.api.variants;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.engine.card.Rank;
import org.engine.card.Suit;

import java.util.List;

/**
 * DTO for a single Barbu contract, sent as part of {@link VariantInfoDTO}.
 *
 * <p>Provides the domain identity ({@code id}, {@code displayName}) alongside a
 * normalized description of the contract mechanics so the frontend can render
 * rules dynamically without hardcoding knowledge of specific contracts.
 *
 * <p>The {@code family} discriminator tells the frontend which play area to
 * expect (trick vs tableau), while scoring rules and lead restrictions are
 * always sent as flat lists (composites are pre-flattened by the mapper).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ContractInfoDTO.TrickTaking.class, name = "TRICK_TAKING"),
        @JsonSubTypes.Type(value = ContractInfoDTO.Reussite.class, name = "REUSSITE")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface ContractInfoDTO permits ContractInfoDTO.TrickTaking, ContractInfoDTO.Reussite {

    String id();
    String displayName();

    record TrickTaking(String id,
            String displayName,
            List<ScoringRuleDTO> scoring,
            List<LeadRestrictionDTO> leadRestriction
    ) implements ContractInfoDTO {}

    record Reussite(
            String id,
            String displayName,
            Rank startingRank,
            Rank replayAfterRank,
            List<Integer> placementPoints
    ) implements ContractInfoDTO {}


    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = ScoringRuleDTO.SuitScores.class, name = "SUIT"),
            @JsonSubTypes.Type(value = ScoringRuleDTO.CardScores.class, name = "CARD"),
            @JsonSubTypes.Type(value = ScoringRuleDTO.RankScores.class, name = "RANK"),
            @JsonSubTypes.Type(value = ScoringRuleDTO.TricksScore.class, name = "TRICKS"),
            @JsonSubTypes.Type(value = ScoringRuleDTO.LastTwoTricks.class, name = "LAST_TWO")
    })
    sealed interface ScoringRuleDTO {
        record SuitScores(Suit suit, int pointsPerCard) implements ScoringRuleDTO {}
        record CardScores(Suit suit, Rank rank, int points) implements ScoringRuleDTO {}
        record RankScores(Rank rank, int pointsPerRank) implements ScoringRuleDTO {}
        record TricksScore(int pointsPerTrick) implements ScoringRuleDTO {}
        record LastTwoTricks(int pointsSecondLast, int pointsLast) implements ScoringRuleDTO {}
    }



    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = LeadRestrictionDTO.OpeningPhase.class, name = "OPENING_PHASE"),
            @JsonSubTypes.Type(value = LeadRestrictionDTO.BrokenSuit.class, name = "BROKEN_SUIT")
    })
    sealed interface LeadRestrictionDTO {
        record OpeningPhase(Suit suit, int length) implements LeadRestrictionDTO {}
        record BrokenSuit(Suit suit) implements LeadRestrictionDTO {}
    }
}