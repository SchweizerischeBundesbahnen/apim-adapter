package ch.sbb.integration.api.adapter.model.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Hits {

    private static final Logger LOG = LoggerFactory.getLogger(Hits.class);

    private ConcurrentHashMap<ResponseSummary, Long> unreportedHits = new ConcurrentHashMap<>();

    public synchronized Map<ResponseSummary, Long> extractUnreportedHits() {
        //This method has to be synchronized, because between the unreportedHits.entrySet() and unreportedHits.clear() no entity should be added or changed.
        int sizeBeforeExtract = unreportedHits.size();

        //Defensive copy
        Map<ResponseSummary, Long> entry = new ConcurrentHashMap<>(unreportedHits);
        unreportedHits.clear();
        int sizeAfterExtract = unreportedHits.size();

        LOG.debug("Hits size extract: {} -> {}", sizeBeforeExtract, sizeAfterExtract);
        LOG.debug("extracted entry: {}", entry);
        return entry;
    }

    public synchronized void addUnreportHits(ResponseSummary responseSummary, Long additionalCount) {
        int sizeBeforeInsert = unreportedHits.size();
        unreportedHits.compute(responseSummary, (existingHit, existingCount) -> addOrInit(existingCount, additionalCount));
        int sizeAfterInsert = unreportedHits.size();
        if(sizeBeforeInsert != sizeAfterInsert) {
            LOG.debug("Hits size insert:  {} -> {}", sizeBeforeInsert, sizeAfterInsert);
        }
    }

    private Long addOrInit(Long existingCount, Long additionalCount) {
        if (existingCount == null) {
            return additionalCount;
        }
        return existingCount + additionalCount;
    }

    @Override
    public String toString() {
        return "Hits{" +
                "unreportedHits=" + unreportedHits +
                '}';
    }
}
