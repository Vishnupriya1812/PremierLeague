package com.example.pl_core_data.util;

import com.example.pl_core_data.DTO.MatchLineupDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RepositoryUtils {
    public List<MatchLineupDTO> getTeamOfTheWeek(List<MatchLineupDTO> pool) {
        int[][] formations = {
                {4, 4, 2},
                {4, 3, 3},
                {4, 5, 1},
                {3, 5, 2},
                {3, 4, 3},
                {5, 3, 2},
                {5, 4, 1},
                {5 ,2, 3}
        };
        var gks = pool.stream().filter(p -> p.getPosition().equals("G")).toList();
        var defs = pool.stream().filter(p -> p.getPosition().equals("D")).toList();
        var mids = pool.stream().filter(p -> p.getPosition().equals("M")).toList();
        var fwds = pool.stream().filter(p -> p.getPosition().equals("F")).toList();

        double maxTotalRating = 0;
        List<MatchLineupDTO> bestXI = null;

        for (int[] f : formations) {
            int dReq = f[0], mReq = f[1], fReq = f[2];

            List<MatchLineupDTO> currentXI = new ArrayList<>();
            currentXI.add(gks.get(0)); // Always pick the #1 Goalkeeper
            currentXI.addAll(defs.subList(0, dReq));
            currentXI.addAll(mids.subList(0, mReq));
            currentXI.addAll(fwds.subList(0, fReq));

            double total = currentXI.stream().mapToDouble(MatchLineupDTO::getRating).sum();

            if (total > maxTotalRating) {
                maxTotalRating = total;
                bestXI = currentXI;
            }
        }
        return bestXI;
    }

}
