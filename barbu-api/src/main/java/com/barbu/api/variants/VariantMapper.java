package com.barbu.api.variants;

import com.barbu.catalog.ContractDefinition;
import com.barbu.catalog.GameVariant;
import org.engine.contract.reussite.ReussiteContract;
import org.engine.contract.trick.LeadRestriction;
import org.engine.contract.trick.ScoringPolicy;
import org.engine.contract.trick.TrickTakingContract;
import org.engine.game.Player;

import java.util.ArrayList;
import java.util.List;

public class VariantMapper {

    /** Maps a catalog variant. Contract ids are the stable catalog string keys. */
    public static VariantInfoDTO mapVariant(GameVariant variant) {
        List<ContractInfoDTO> contracts = variant.contracts().stream()
                .map(VariantMapper::mapContractInfo)
                .toList();
        return new VariantInfoDTO(variant.id(), variant.displayName(), contracts, variant.deckVariant(),
                variant.rankingOrder());
    }

    public static ContractInfoDTO mapContractInfo(ContractDefinition cd) {
        return switch (cd.contract()) {
            case TrickTakingContract ttc -> new ContractInfoDTO.TrickTaking(
                    cd.id(),
                    cd.displayName(),
                    flattenScoring(ttc.scoringPolicy()),
                    flattenLeadRestriction(ttc.leadRestriction()));

            case ReussiteContract rc -> new ContractInfoDTO.Reussite(
                    cd.id(),
                    cd.displayName(),
                    rc.startingRank(),
                    rc.replayAfterRank(),
                    padPlacementPoints(rc.placementPoints()));

            default -> throw new IllegalArgumentException(
                    "Unknown contract type: " + cd.contract().getClass().getSimpleName());
        };
    }



    /** Pads placement points to 4 entries (one per seat) so the frontend always gets a score for each player. */
    private static List<Integer> padPlacementPoints(List<Integer> points) {
        List<Integer> padded = new ArrayList<>(points);
        while (padded.size() < Player.values().length) {
            padded.add(0);
        }
        return List.copyOf(padded);
    }

    /** Flattens composite scoring policies into a flat list of individual rules. */
    private static List<ContractInfoDTO.ScoringRuleDTO> flattenScoring(ScoringPolicy policy) {
        if (policy instanceof ScoringPolicy.CompositeScoringPolicy composite) {
            return composite.children().stream()
                    .flatMap(child -> flattenScoring(child).stream())
                    .toList();
        }
        return List.of(mapScoringRule(policy));
    }

    private static ContractInfoDTO.ScoringRuleDTO mapScoringRule(ScoringPolicy policy) {
        return switch (policy) {
            case ScoringPolicy.SuitScoresPoints p ->
                    new ContractInfoDTO.ScoringRuleDTO.SuitScores(p.suit(), p.pointsPerCard());
            case ScoringPolicy.CardScoresPoints p ->
                    new ContractInfoDTO.ScoringRuleDTO.CardScores(p.card().suit(), p.card().rank(), p.points());
            case ScoringPolicy.RankScoresPoints p ->
                    new ContractInfoDTO.ScoringRuleDTO.RankScores(p.rank(), p.pointsPerCard());
            case ScoringPolicy.TricksScorePoints p ->
                    new ContractInfoDTO.ScoringRuleDTO.TricksScore(p.pointsPerTrick());
            case ScoringPolicy.LastTwoTricksScorePoints p ->
                    new ContractInfoDTO.ScoringRuleDTO.LastTwoTricks(p.pointsSecondLast(), p.pointsLast());
            case ScoringPolicy.CompositeScoringPolicy _ ->
                    throw new IllegalStateException("Composite should have been flattened");
            default -> throw new IllegalArgumentException(
                    "Unknown scoring policy: " + policy.getClass().getSimpleName());
        };
    }


    /** Flattens composite lead restrictions into a flat list, and maps None to an empty list. */
    private static List<ContractInfoDTO.LeadRestrictionDTO> flattenLeadRestriction(LeadRestriction restriction) {
        return switch (restriction) {
            case LeadRestriction.None _ -> List.of();
            case LeadRestriction.Composite composite ->
                    composite.restrictions().stream()
                            .flatMap(child -> flattenLeadRestriction(child).stream())
                            .toList();
            case LeadRestriction.OpeningPhase op ->
                    List.of(new ContractInfoDTO.LeadRestrictionDTO.OpeningPhase(op.suit(), op.length()));
            case LeadRestriction.BrokenSuit bs ->
                    List.of(new ContractInfoDTO.LeadRestrictionDTO.BrokenSuit(bs.suit()));
            default -> throw new IllegalArgumentException(
                    "Unknown lead restriction: " + restriction.getClass().getSimpleName());
        };
    }
}