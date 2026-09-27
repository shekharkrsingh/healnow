package com.heal.doctor.services;

import com.heal.doctor.models.SequenceCounterEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;
import com.heal.doctor.utils.DateUtils;


@Service
public class SequenceGeneratorService {

    private final MongoOperations mongoOperations;

    @Autowired
    public SequenceGeneratorService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    public String generateAppointmentId() {
        // 1. Get today's date formatted as YYMMDD in IST.
        //    Without an explicit timezone, SimpleDateFormat uses the JVM default,
        //    which is UTC on most cloud/Docker servers. IDs generated between
        //    18:30–00:00 UTC would get the previous day's date prefix in IST.
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");
        sdf.setTimeZone(DateUtils.IST_TIMEZONE);
        String datePrefix = sdf.format(new Date());

        // 2. The sequence name is unique for each IST day
        String seqName = "appointment_seq_" + datePrefix;

        // 3. Atomically increment the sequence for today
        SequenceCounterEntity counter = mongoOperations.findAndModify(
                query(where("_id").is(seqName)),
                new Update().inc("seq", 1),
                options().returnNew(true).upsert(true),
                SequenceCounterEntity.class);

        long sequenceNumber = !Objects.isNull(counter) ? counter.getSeq() : 1;

        // 4. Format sequence as a 4-digit string (e.g. 0001)
        String sequenceString = String.format("%04d", sequenceNumber);

        // 5. Combine and return
        return datePrefix + sequenceString;
    }
}
